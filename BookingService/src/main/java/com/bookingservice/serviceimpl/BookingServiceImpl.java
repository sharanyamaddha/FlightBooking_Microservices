package com.bookingservice.serviceimpl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;


import com.bookingservice.client.FlightClient;
import com.bookingservice.client.dto.FlightDto;
import com.bookingservice.client.dto.ReleaseSeatsRequest;
import com.bookingservice.client.dto.ReserveSeatsRequest;
import com.bookingservice.client.dto.ReserveSeatsResponse;
import com.bookingservice.dto.request.BookingRequest;
import com.bookingservice.dto.request.PassengerRequest;
import com.bookingservice.dto.response.BookingResponse;
import com.bookingservice.dto.response.PassengerResponse;
import com.bookingservice.enums.BookingStatus;
import com.bookingservice.enums.TripType;
import com.bookingservice.events.BookingCancelledEvent;
import com.bookingservice.events.BookingCreatedEvent;
import com.bookingservice.exceptions.BadRequestException;
import com.bookingservice.exceptions.BusinessException;
import com.bookingservice.exceptions.ConflictException;
import com.bookingservice.kafka.BookingEventProducer;
import com.bookingservice.model.Booking;
import com.bookingservice.model.Passenger;
import com.bookingservice.repository.BookingRepository;
import com.bookingservice.repository.PassengerRepository;
import com.bookingservice.service.BookingService;

@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);
    private static final String FLIGHT_SERVICE_CB = "flightService"; // circuit breaker name


    @Autowired
    private FlightClient flightClient;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PassengerRepository passengerRepository;
    
    @Autowired
    private BookingEventProducer bookingEventProducer;


    @Override
    @Transactional
    @CircuitBreaker(name=FLIGHT_SERVICE_CB,fallbackMethod= "createBookingFallback")
    public BookingResponse createBooking(String flightId, BookingRequest request) {

        // 1) Fetch flight metadata from FlightService
    	// NEW – don't wrap it, let the exception bubble up
    	FlightDto flightDto = flightClient.getFlight(flightId);


        // 2) Validate passenger count
        int passengerCount = request.getPassengers().size();
        if (flightDto.getAvailableSeats() < passengerCount) {
            throw new BusinessException("Not enough seats available");
        }

        // 3) Normalize seat numbers and check conflicts
        List<String> seatNos = request.getPassengers().stream()
                .map(PassengerRequest::getSeatNo)
                .filter(Objects::nonNull)
                .map(s -> s.trim().toUpperCase())
                .collect(Collectors.toList());

        if (!seatNos.isEmpty()) {
            List<Passenger> conflicts = passengerRepository.findByFlightIdAndSeatNoIn(flightId, seatNos);
            if (!conflicts.isEmpty()) {
                String taken = conflicts.stream()
                        .map(Passenger::getSeatNo)
                        .distinct()
                        .collect(Collectors.joining(", "));
                throw new BusinessException("Seat(s) already taken: " + taken);
            }
        }
        
        
        // 4) Reserve seats on flight-service
        String bookingReference = "BR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ReserveSeatsRequest reserveReq = new ReserveSeatsRequest();
        reserveReq.setBookingReference(bookingReference);
        reserveReq.setCount(passengerCount);
        reserveReq.setSeatNumbers(seatNos);

        ReserveSeatsResponse reserveResp = flightClient.reserveSeats(flightId, reserveReq);


        if (reserveResp == null || !reserveResp.isSuccess()) {
            String msg = reserveResp != null ? reserveResp.getMessage() : "Unknown reservation failure";
            throw new BusinessException("Seat reservation failed: " + msg);
        }

        // 5) Create booking locally
        String pnr = "PNR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Booking booking = new Booking();
        booking.setPnr(pnr);
        booking.setFlightId(flightId);
        booking.setBookerEmailId(request.getBookerEmailId());
        booking.setStatus(BookingStatus.BOOKED);
        booking.setTripType(request.getTripType() != null ? request.getTripType() : TripType.ONE_WAY);
        booking.setBookingDateTime(LocalDateTime.now());
        booking.setSeatsBooked(passengerCount);
        booking.setTotalAmount(flightDto.getPrice() * passengerCount);

        Booking savedBooking = bookingRepository.save(booking);

        // 6) Save passengers
        List<Passenger> passengersToSave = request.getPassengers().stream().map(pReq -> {
            Passenger p = new Passenger();
            p.setName(pReq.getName());
            p.setAge(pReq.getAge());
            p.setGender(pReq.getGender());
            p.setSeatNo(pReq.getSeatNo());
            p.setMealType(pReq.getMealType());
            p.setFlightId(flightId);
            p.setPnr(savedBooking.getPnr());
            return p;
        }).collect(Collectors.toList());

        try {
            passengerRepository.saveAll(passengersToSave);
        } catch (Exception ex) {
            // Compensation: release seats
            ReleaseSeatsRequest releaseReq = new ReleaseSeatsRequest();
            releaseReq.setBookingReference(bookingReference);
            releaseReq.setCount(passengerCount);
            releaseReq.setSeatNumbers(seatNos);

            try {
                flightClient.releaseSeats(flightId, releaseReq);
            } catch (Exception compEx) {
                throw new BusinessException("Failed to save passengers & release-seat compensation failed: " + compEx.getMessage());
            }

            throw new BusinessException("Failed to save passengers: " + ex.getMessage());
        }

        // 7) Build response
        BookingResponse response = new BookingResponse();
        response.setPnr(savedBooking.getPnr());
        response.setStatus(savedBooking.getStatus());
        response.setTripType(savedBooking.getTripType());
        response.setTotalAmount(savedBooking.getTotalAmount());
        response.setBookingDateTime(savedBooking.getBookingDateTime());
        response.setBookerEmailId(savedBooking.getBookerEmailId());
        response.setSource(flightDto.getSource());
        response.setDestination(flightDto.getDestination());
        response.setAirlineName(flightDto.getAirlineName());

        List<PassengerResponse> passengerResponses = passengersToSave.stream().map(p -> {
            PassengerResponse pr = new PassengerResponse();
            pr.setName(p.getName());
            pr.setAge(p.getAge());
            pr.setGender(p.getGender());
            pr.setSeatNo(p.getSeatNo());
            pr.setMealType(p.getMealType());
            return pr;
        }).collect(Collectors.toList());
        response.setPassengers(passengerResponses);
        
        //publish bookingCreatedEvent to Kafka
        BookingCreatedEvent event = new BookingCreatedEvent(
                savedBooking.getPnr(),
                savedBooking.getBookerEmailId(),
                savedBooking.getFlightId(),
                flightDto.getAirlineName(),
                savedBooking.getSeatsBooked(),
                savedBooking.getTotalAmount(),
                savedBooking.getBookingDateTime()
        );
        bookingEventProducer.sendBookingCreatedEvent(event);
        		

        return response;
    }
    
   
	public BookingResponse createBookingFallback(String flightId, BookingRequest request, Throwable ex) {
		logger.error("Fallback triggered for createBooking. Reason: {}", ex.toString());

		if (ex instanceof BusinessException be) {
			String msg = be.getMessage();
			if (msg != null && msg.startsWith("Seat(s) already taken")) {
				throw be; // goes to GlobalExceptionHandler -> 400 with that message
			}
			 // Flight not found - pass through
		    if (msg.startsWith("Flight not found")) {
		        throw be;
		    }
		}

		throw new BusinessException("Flight service is temporarily unavailable. Please try again later.");
	}



    

    @Override
    public BookingResponse getBookingByPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new BusinessException("invalid PNR"));

        FlightDto flightDto;
        try {
            flightDto = flightClient.getFlight(booking.getFlightId());
        } catch (Exception ex) {
            throw new BusinessException("Failed to fetch flight info: " + ex.getMessage());
        }

        BookingResponse res = new BookingResponse();
        res.setPnr(booking.getPnr());
        res.setStatus(booking.getStatus());
        res.setTripType(booking.getTripType());
        res.setTotalAmount(booking.getTotalAmount());
        res.setBookingDateTime(booking.getBookingDateTime());
        res.setBookerEmailId(booking.getBookerEmailId());
        res.setSource(flightDto.getSource());
        res.setDestination(flightDto.getDestination());
        res.setAirlineName(flightDto.getAirlineName());

        List<Passenger> passengers = passengerRepository.findByPnr(booking.getPnr());
        List<PassengerResponse> passengerResponses = passengers.stream().map(p -> {
            PassengerResponse pr = new PassengerResponse();
            pr.setName(p.getName());
            pr.setAge(p.getAge());
            pr.setGender(p.getGender());
            pr.setSeatNo(p.getSeatNo());
            pr.setMealType(p.getMealType());
            return pr;
        }).collect(Collectors.toList());
        res.setPassengers(passengerResponses);

        return res;
    }

    @Override
    public List<BookingResponse> getBookingHistory(String bookerEmailId) {
        List<Booking> bookings = bookingRepository.findByBookerEmailIdOrderByBookingDateTimeDesc(bookerEmailId);
        if (bookings == null || bookings.isEmpty()) {
            throw new BusinessException("No bookings found for email: " + bookerEmailId);
        }

        return bookings.stream().map(b -> {
            FlightDto flightDto;
            try {
                flightDto = flightClient.getFlight(b.getFlightId());
            } catch (Exception ex) {
                throw new BusinessException("Failed to fetch flight info for booking: " + b.getPnr());
            }

            BookingResponse res = new BookingResponse();
            res.setPnr(b.getPnr());
            res.setStatus(b.getStatus());
            res.setTripType(b.getTripType());
            res.setTotalAmount(b.getTotalAmount());
            res.setBookingDateTime(b.getBookingDateTime());
            res.setBookerEmailId(b.getBookerEmailId());
            res.setSource(flightDto.getSource());
            res.setDestination(flightDto.getDestination());
            res.setAirlineName(flightDto.getAirlineName());

            List<Passenger> passengers = passengerRepository.findByPnr(b.getPnr());
            List<PassengerResponse> passengerResponses = passengers.stream().map(p -> {
                PassengerResponse pr = new PassengerResponse();
                pr.setName(p.getName());
                pr.setAge(p.getAge());
                pr.setGender(p.getGender());
                pr.setSeatNo(p.getSeatNo());
                pr.setMealType(p.getMealType());
                return pr;
            }).collect(Collectors.toList());
            res.setPassengers(passengerResponses);

            return res;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CircuitBreaker(name = FLIGHT_SERVICE_CB, fallbackMethod = "cancelBookingFallback")

    public String cancelBooking(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new BusinessException("Invalid PNR"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("Booking already cancelled");
        }

        LocalDateTime bookingTime = booking.getBookingDateTime();
        LocalDateTime now = LocalDateTime.now();
        if (Duration.between(bookingTime, now).toHours() >= 24) {
            throw new BadRequestException("Cancellation allowed only within 24 hours of booking");
        }

        long passengerCount = passengerRepository.countByPnr(pnr);
        int seatsToFree = (int) passengerCount;

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        ReleaseSeatsRequest releaseReq = new ReleaseSeatsRequest();
        releaseReq.setBookingReference(pnr);
        releaseReq.setCount(seatsToFree);

        List<String> seatNumbers = passengerRepository.findByPnr(pnr).stream()
                .map(Passenger::getSeatNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        releaseReq.setSeatNumbers(seatNumbers);

        try {
            flightClient.releaseSeats(booking.getFlightId(), releaseReq);
        } catch (Exception ex) {
            throw new BusinessException("Booking cancelled locally but releasing seats failed: " + ex.getMessage());
        }
        
        FlightDto flightDto = flightClient.getFlight(booking.getFlightId());

        BookingCancelledEvent event = new BookingCancelledEvent(
                booking.getPnr(),
                booking.getBookerEmailId(),
                booking.getFlightId(),
                flightDto.getAirlineName(),
                LocalDateTime.now()
        );
        bookingEventProducer.sendBookingCancelledEvent(event);

        return "Booking cancelled successfully";
    }
    
    public String cancelBookingFallback(String pnr, Throwable ex) {
        logger.error("Circuit breaker OPEN for cancelBooking. Reason: {}", ex.getMessage());

        
        throw new BusinessException("Cannot cancel booking right now. Flight service is unavailable. Please try again later.");

        
    }

}
