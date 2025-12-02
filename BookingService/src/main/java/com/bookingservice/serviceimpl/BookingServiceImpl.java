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
import com.bookingservice.exceptions.BadRequestException;
import com.bookingservice.exceptions.BusinessException;
import com.bookingservice.exceptions.ConflictException;
import com.bookingservice.model.Booking;
import com.bookingservice.model.Passenger;
import com.bookingservice.repository.BookingRepository;
import com.bookingservice.repository.PassengerRepository;
import com.bookingservice.service.BookingService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class BookingServiceImpl implements BookingService {
	
    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);


    @Autowired
    private FlightClient flightClient;                

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PassengerRepository passengerRepository;


    @Override
    @Transactional
    public BookingResponse createBooking(String flightId, BookingRequest request) {

        // 1) Fetch flight metadata from FlightService via Feign
        FlightDto flightDto;
        try {
            flightDto = flightClient.getFlight(flightId);
        } catch (Exception ex) {
            throw new BusinessException("Flight not found or flight-service error: " + ex.getMessage());
        }

        // 2) validate counts
        int passengerCount = request.getPassengers().size();
        if (flightDto.getAvailableSeats() < passengerCount) {
            throw new BusinessException("Not enough seats available");
        }

        // 3) normalize seat numbers and check local conflicts (passenger collection)
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

        // 4) Reserve seats on flight-service (atomic)
        String bookingReference = "BR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ReserveSeatsRequest reserveReq = new ReserveSeatsRequest();
        reserveReq.setBookingReference(bookingReference);
        reserveReq.setCount(passengerCount);
        reserveReq.setSeatNumbers(seatNos);

        ReserveSeatsResponse reserveResp;
        try {
            reserveResp = flightClient.reserveSeats(flightId, reserveReq);
        } catch (Exception ex) {
            throw new BusinessException("Failed to reserve seats: " + ex.getMessage());
        }

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

        double price = flightDto.getPrice();
        booking.setTotalAmount(price * passengerCount);

        Booking savedBooking = bookingRepository.save(booking);

        // 6) Save passengers, with compensation if it fails
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
            // Compensation: release seats on FlightService
            ReleaseSeatsRequest releaseReq = new ReleaseSeatsRequest();
            releaseReq.setBookingReference(bookingReference);
            releaseReq.setCount(passengerCount);
            releaseReq.setSeatNumbers(seatNos);
            try {
                flightClient.releaseSeats(flightId, releaseReq);
            } catch (Exception compEx) {
                // Critical: compensation failed. In production enqueue a retry job for reconciliation.
                throw new BusinessException("Failed to save passengers and seat-release compensation failed: " + compEx.getMessage());
            }
            throw new BusinessException("Failed to save passengers: " + ex.getMessage());
        }

        // 7) Build response (include flight fields fetched earlier)
        BookingResponse response = new BookingResponse();
        response.setPnr(savedBooking.getPnr());
        response.setStatus(savedBooking.getStatus());
        response.setTripType(savedBooking.getTripType());
        response.setTotalAmount(savedBooking.getTotalAmount());
        response.setBookingDateTime(savedBooking.getBookingDateTime());
        response.setBookerEmailId(savedBooking.getBookerEmailId());

        // flight fields
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

        return response;
    }


    @Override
    public BookingResponse getBookingByPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new BusinessException("invalid PNR"));

        // fetch flight fields from flight-service
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
            // fetch flight info for each booking (could be optimized/cached)
            FlightDto flightDto;
            try {
                flightDto = flightClient.getFlight(b.getFlightId());
            } catch (Exception ex) {
                // if flight fetch fails just set nulls or skip — here we throw to keep behavior consistent
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

        // count passengers for release
        long passengerCount = passengerRepository.countByPnr(pnr);
        int seatsToFree = (int) passengerCount;

        // update local booking status
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // call flight-service to release seats (best-effort)
        ReleaseSeatsRequest releaseReq = new ReleaseSeatsRequest();
        releaseReq.setBookingReference(pnr); // using pnr as reference for release
        releaseReq.setCount(seatsToFree);
        // optional: gather seatNumbers from passengers if you stored them
        List<String> seatNumbers = passengerRepository.findByPnr(pnr).stream()
                .map(Passenger::getSeatNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        releaseReq.setSeatNumbers(seatNumbers);

        try {
            flightClient.releaseSeats(booking.getFlightId(), releaseReq);
        } catch (Exception ex) {
            // If release fails, log and enqueue retry in production. Here we throw to inform caller.
            throw new BusinessException("Booking cancelled locally but releasing seats failed: " + ex.getMessage());
        }

        return "Booking cancelled successfully";
    }
    
    @CircuitBreaker(name = "flightServiceCB", fallbackMethod = "flightFallback")
    @Retry(name = "flightServiceRetry", fallbackMethod = "flightFallback")
    public FlightDto safeGetFlight(String flightId) {
        return flightClient.getFlight(flightId);
    }

    @CircuitBreaker(name = "flightServiceCB", fallbackMethod = "reserveFallback")
    @Retry(name = "flightServiceRetry", fallbackMethod = "reserveFallback")
    public ReserveSeatsResponse safeReserveSeats(String flightId, ReserveSeatsRequest req) {
        return flightClient.reserveSeats(flightId, req);
    }

    @CircuitBreaker(name = "flightServiceCB", fallbackMethod = "releaseFallback")
    @Retry(name = "flightServiceRetry", fallbackMethod = "releaseFallback")
    public void safeReleaseSeats(String flightId, ReleaseSeatsRequest req) {
        flightClient.releaseSeats(flightId, req);
    }

    public FlightDto flightFallback(String flightId, Throwable ex) {
        logger.error("Flight service unavailable for id {} — fallback invoked: {}", flightId, ex.toString());
        throw new BusinessException("Flight service unavailable: " + ex.getMessage());
    }

    public ReserveSeatsResponse reserveFallback(String flightId, ReserveSeatsRequest req, Throwable ex) {
        logger.error("Reserve fallback for flight {}: {}", flightId, ex.toString());
        ReserveSeatsResponse r = new ReserveSeatsResponse();
        r.setSuccess(false);
        r.setMessage("Fallback: seat reservation failed because FlightService is unavailable");
        return r;
    }

    public void releaseFallback(String flightId, ReleaseSeatsRequest req, Throwable ex) {
        logger.error("Release fallback for flight {}: {}", flightId, ex.toString());
    }
}
