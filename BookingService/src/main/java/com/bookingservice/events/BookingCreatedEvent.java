package com.bookingservice.events;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingCreatedEvent {

    private String pnr;
    private String bookerEmailId;

    private String flightId;
    private String airlineName;

    private int seatsBooked;
    private Double totalAmount;

    private LocalDateTime bookingDateTime;
}
