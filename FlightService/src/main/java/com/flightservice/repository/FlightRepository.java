package com.flightservice.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.flightservice.model.Flight;

@Repository
public interface FlightRepository extends MongoRepository<Flight, String> {

    Flight findByAirlineIdAndSourceAndDestinationAndDepartureDateTime(
            String airlineId,
            String source,
            String destination,
            LocalDateTime departureDateTime
    );

    List<Flight> findBySourceIgnoreCaseAndDestinationIgnoreCase(String source, String destination);

    List<Flight> findByAirlineIdIgnoreCase(String airlineId);

    boolean existsByAirlineIdAndSourceAndDestinationAndDepartureDateTime(
            String airlineId,
            String source,
            String destination,
            LocalDateTime departureDateTime
    );
}
