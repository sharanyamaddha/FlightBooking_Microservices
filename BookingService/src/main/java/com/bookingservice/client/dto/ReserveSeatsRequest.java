package com.bookingservice.client.dto;

import java.util.List;

import lombok.Data;

@Data
public class ReserveSeatsRequest {

	 private String bookingReference; 
	    private int count; 
	    private List<String> seatNumbers; 
}
