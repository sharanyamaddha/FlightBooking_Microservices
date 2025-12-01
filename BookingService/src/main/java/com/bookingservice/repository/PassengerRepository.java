package com.bookingservice.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.bookingservice.model.Booking;
import com.bookingservice.model.Passenger;

@Repository
public interface PassengerRepository extends MongoRepository<Passenger,String>{

	public List<Passenger> findByFlightIdAndSeatNoIn(String flightId, List<String> seatNos) ;

	public List<Passenger> findByPnr(String pnr);

	public long countByPnr(String pnr);


}
