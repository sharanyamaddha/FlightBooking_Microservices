package com.bookingservice.events;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingCancelledEvent {

    private String pnr;
    private String bookerEmailId;

    private String flightId;
    private String airlineName;

    private LocalDateTime cancelledAt;
}
