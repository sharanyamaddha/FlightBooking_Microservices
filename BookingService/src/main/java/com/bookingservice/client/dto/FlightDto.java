package com.bookingservice.client.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class FlightDto {
    private String flightId;
    private String flightNo;
    private String airlineName;
    private String source;
    private String destination;
    private LocalDateTime departureDateTime;
    private LocalDateTime arrivalDateTime;
    private int availableSeats;
    private double price;

}
