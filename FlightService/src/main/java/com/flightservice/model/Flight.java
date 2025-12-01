package com.flightservice.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.flightservice.enums.TripType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Flight {


	@Id
	String flightId;
	

	String airlineId;
	
	String airlineName;
	
	
	String flightNo;
	
	String source;
	
	String destination;
	
	LocalDateTime departureDateTime;
	
	 LocalDateTime arrivalDateTime;
	 
	 int totalSeats;
	 
	 int availableSeats;
	 
	 Double price;
	 
	 TripType tripType;
	
}
