package com.bookingservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.bookingservice.model.Booking;



@Repository
public interface BookingRepository extends MongoRepository<Booking,String> {
	 
	Optional<Booking> findByPnr(String pnr);
	
	List<Booking> findByBookerEmailIdOrderByBookingDateTimeDesc(String bookerEmailId);
	

}
