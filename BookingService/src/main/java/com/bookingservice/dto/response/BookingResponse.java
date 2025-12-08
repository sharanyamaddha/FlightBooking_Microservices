package com.bookingservice.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.bookingservice.enums.BookingStatus;
import com.bookingservice.enums.TripType;

import lombok.Data;


@Data
public class BookingResponse {

	 private String pnr;
	    private String bookerEmailId;
	    private BookingStatus status;
	    private Double totalAmount;
	    private LocalDateTime bookingDateTime;
	    private String FlightId;
	    TripType tripType;


	    private String airlineName;
	    private String source;
	    private String destination;
	    
	    private String userMessage;

	    private List<PassengerResponse> passengers;

		
}
