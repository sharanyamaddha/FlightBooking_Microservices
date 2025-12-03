package com.bookingservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bookingservice.dto.request.BookingRequest;
import com.bookingservice.dto.response.BookingResponse;
import com.bookingservice.rabbitmq.BookingProducer;
import com.bookingservice.service.BookingService;

import jakarta.validation.Valid;


@RestController
public class BookingController {

	@Autowired
    private BookingService bookingService;
	
    @Autowired
    private BookingProducer bookingProducer;  

    @PostMapping("/booking/{flightId}")
    public ResponseEntity<String> createBooking(@PathVariable("flightId") String flightId,
                                                @Valid @RequestBody BookingRequest request) {
        BookingResponse saved = bookingService.createBooking(flightId, request);
        
     // ✅ Send event to RabbitMQ after successful booking
        String eventMessage = "BOOKING_CREATED | PNR: " + saved.getPnr()
                            + " | Flight: " + flightId
                            + " | Email: " + saved.getBookerEmailId();
        bookingProducer.sendBookingCreatedEvent(eventMessage);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.getPnr());
    }

    @GetMapping("/booking/{pnr}")
    public ResponseEntity<BookingResponse> getBookingByPnr(@PathVariable("pnr") String pnr) {
        BookingResponse booking = bookingService.getBookingByPnr(pnr);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/booking/history/{email}")
    public ResponseEntity<List<BookingResponse>> getBookingHistory(@PathVariable("email") String email) {
        List<BookingResponse> history = bookingService.getBookingHistory(email);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/booking/cancel/{pnr}")
    public ResponseEntity<String> cancelBooking(@PathVariable("pnr") String pnr) {
        String message = bookingService.cancelBooking(pnr);
        if (message == null) {
            return ResponseEntity.notFound().build();
           
        }
        // ✅ Send cancel event to RabbitMQ
        String eventMessage = "BOOKING_CANCELLED | PNR: " + pnr;
        bookingProducer.sendBookingCancelledEvent(eventMessage);

        return ResponseEntity.ok(message);
    }
	
}
