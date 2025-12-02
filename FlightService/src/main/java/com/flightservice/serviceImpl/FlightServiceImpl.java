package com.flightservice.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.flightservice.dto.request.FlightRequest;
import com.flightservice.dto.request.ReleaseSeatsRequest;
import com.flightservice.dto.request.ReserveSeatsRequest;
import com.flightservice.dto.response.FlightResponse;
import com.flightservice.dto.response.ReserveSeatsResponse;
import com.flightservice.exceptions.BusinessException;
import com.flightservice.model.Airline;
import com.flightservice.model.Flight;
import com.flightservice.repository.AirlineRepository;
import com.flightservice.repository.FlightRepository;
import com.flightservice.service.FlightService;

@Service
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;
    private final AirlineRepository airlineRepository;

    public FlightServiceImpl(FlightRepository flightRepository, AirlineRepository airlineRepository) {
        this.flightRepository = flightRepository;
        this.airlineRepository = airlineRepository;
    }

    @Override
    public Flight addFlights(FlightRequest request) {

        LocalDateTime departure = request.getDepartureDateTime();
        LocalDateTime arrival = request.getArrivalDateTime();

        if (arrival.isBefore(departure)) {
            throw new BusinessException("Arrival time must be after departure time");
        }

        // Find or create airline
        Optional<Airline> optAirline = airlineRepository.findByAirlineNameIgnoreCase(request.getAirlineName());
        Airline airline = optAirline.orElseGet(() -> {
            Airline a = new Airline();
            a.setAirlineName(request.getAirlineName());
            return airlineRepository.save(a);
        });

        // Check if flight already exists for this airline/time/route
        boolean exists = flightRepository.existsByAirlineIdAndSourceAndDestinationAndDepartureDateTime(
                airline.getAirlineId(),
                request.getSource(),
                request.getDestination(),
                request.getDepartureDateTime()
        );

        if (exists) {
            throw new BusinessException("Flight already exists for this airline at this time");
        }

        
        Flight flight = new Flight();
        flight.setAirlineId(airline.getAirlineId());
        flight.setSource(request.getSource());
        flight.setDestination(request.getDestination());
        flight.setDepartureDateTime(departure);
        flight.setArrivalDateTime(arrival);
        flight.setTotalSeats(request.getTotalSeats());
        flight.setAvailableSeats(request.getTotalSeats());
        flight.setPrice(request.getPrice());

        
        String airlineCode = airline.getAirlineName()
                .substring(0, Math.min(2, airline.getAirlineName().length()))
                .toUpperCase();
        int randomNumber = (int) (Math.random() * 900) + 100;
        String flightNumber = airlineCode + "-" + randomNumber;
        flight.setFlightNo(flightNumber);

        
        return flightRepository.save(flight);
    }

    @Override
    public List<FlightResponse> searchFlights(FlightRequest request) {
        String source = request.getSource();
        String destination = request.getDestination();
        String airlineName = request.getAirlineName();

        List<FlightResponse> responses = new ArrayList<>();

        if (airlineName == null || airlineName.isBlank()) {
            List<Flight> flights = flightRepository.findBySourceIgnoreCaseAndDestinationIgnoreCase(source, destination);
            for (Flight f : flights) {
                responses.add(mapFlightToResponse(f));
            }
            return responses;
        }

        // searching by airlineName
        Optional<Airline> optAirline = airlineRepository.findByAirlineNameIgnoreCase(airlineName);
        if (optAirline.isEmpty()) {
            throw new BusinessException("Airline not found");
        }
        Airline airline = optAirline.get();
        List<Flight> flights = flightRepository.findByAirlineIdIgnoreCase(airline.getAirlineId());
        for (Flight f : flights) {
            responses.add(mapFlightToResponse(f));
        }
        return responses;
    }

    private FlightResponse mapFlightToResponse(Flight flight) {
        Optional<Airline> optAirline = airlineRepository.findById(flight.getAirlineId());
        String airlineName = optAirline.map(Airline::getAirlineName).orElse("Unknown");

        FlightResponse res = new FlightResponse();
        res.setFlightNo(flight.getFlightNo());
        res.setAirlineName(airlineName);
        res.setSource(flight.getSource());
        res.setDestination(flight.getDestination());
        res.setDepartureDateTime(flight.getDepartureDateTime());
        res.setArrivalDateTime(flight.getArrivalDateTime());
        res.setAvailableSeats(flight.getAvailableSeats());
        res.setPrice(flight.getPrice());
        return res;
    }
    
    @Override
    public ReserveSeatsResponse reserveSeats(String flightId, ReserveSeatsRequest request) {
        // load flight
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new BusinessException("Flight not found with id: " + flightId));

        int count = request.getCount();
        if (count <= 0) {
            throw new BusinessException("Invalid seats count: " + count);
        }

        if (flight.getAvailableSeats() < count) {
            throw new BusinessException("Not enough seats available. Requested: " + count + ", Available: " + flight.getAvailableSeats());
        }

        // decrement and save
        flight.setAvailableSeats(flight.getAvailableSeats() - count);
        Flight saved = flightRepository.save(flight);

        // build response
        ReserveSeatsResponse resp = new ReserveSeatsResponse();
        resp.setSuccess(true);
        resp.setReservationReference("RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        resp.setSeatsReserved(count);
        resp.setRemainingSeats(saved.getAvailableSeats());
        return resp;
    }

    @Override
    public void releaseSeats(String flightId, ReleaseSeatsRequest request) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new BusinessException("Flight not found with id: " + flightId));

        int count = request.getCount();
        if (count <= 0) {
            throw new BusinessException("Invalid seats count: " + count);
        }

        flight.setAvailableSeats(flight.getAvailableSeats() + count);
        flightRepository.save(flight);
    }

    @Override
    public FlightResponse getFlightById(String id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Flight not found with id: " + id));

        FlightResponse response = new FlightResponse();
        //response.setFlightId(flight.getFlightId());
        response.setAirlineName(flight.getAirlineName());
        response.setSource(flight.getSource());
        response.setDestination(flight.getDestination());
        response.setDepartureDateTime(flight.getDepartureDateTime());
        response.setDepartureDateTime(flight.getDepartureDateTime());
        response.setArrivalDateTime(flight.getArrivalDateTime());
        response.setArrivalDateTime(flight.getArrivalDateTime());
        response.setAvailableSeats(flight.getAvailableSeats());
        response.setPrice(flight.getPrice());

        return response;
    }

    
}
