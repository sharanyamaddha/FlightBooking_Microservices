package com.flightservice.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class FlightResponse {

	private String flightNo; 
    private String airlineName;
    private String source;
    private String destination;
    private LocalDateTime departureDateTime;
    private LocalDateTime arrivalDateTime;
    private int availableSeats;
    private Double price;
}
