package com.bookingservice.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.bookingservice.enums.BookingStatus;
import com.bookingservice.enums.TripType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
	
	@Id
	String bookingid;
	
	String flightId;
	
	String airlineName;
	
	String pnr;
	
	LocalDateTime bookingDateTime;
	
	String bookerEmailId;
	
	int seatsBooked;
	
	Double totalAmount;
	
	BookingStatus status;
	
	TripType tripType;
	

	List<Passenger> passengers=new ArrayList<>();
}
