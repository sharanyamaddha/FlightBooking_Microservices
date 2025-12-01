package com.flightservice.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.flightservice.model.Airline;


@Repository
public interface AirlineRepository extends MongoRepository<Airline,String>{

	Optional<Airline> findByAirlineNameIgnoreCase(String airlinenName);
}
