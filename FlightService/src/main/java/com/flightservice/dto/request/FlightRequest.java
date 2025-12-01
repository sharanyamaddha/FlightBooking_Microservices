package com.flightservice.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FlightRequest {


	@NotNull
	String airlineName;
	
	@NotBlank
	String source;
	
	@NotBlank
	String destination;
	
	@NotNull
	@FutureOrPresent(message="departure time must be before arrival time")
	LocalDateTime departureDateTime;
	
	@NotNull
	@Future(message="arrival time must be after departure time")
	 LocalDateTime arrivalDateTime;
	
	@NotNull
	 int totalSeats;
	
	@NotNull
	Double price;
}
