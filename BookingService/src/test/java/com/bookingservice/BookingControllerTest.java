package com.bookingservice;

import com.bookingservice.controller.BookingController;
import com.bookingservice.dto.request.BookingRequest;
import com.bookingservice.dto.response.BookingResponse;
import com.bookingservice.dto.response.PassengerResponse;
import com.bookingservice.enums.BookingStatus;
import com.bookingservice.enums.TripType;
import com.bookingservice.rabbitmq.BookingProducer;
import com.bookingservice.service.BookingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private BookingProducer bookingProducer;

    @InjectMocks
    private BookingController bookingController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private BookingResponse sampleResponse() {
        BookingResponse response = new BookingResponse();
        response.setPnr("PNR123");
        response.setBookerEmailId("test@test.com");
        response.setFlightId("FL1");
        response.setStatus(BookingStatus.BOOKED);
        response.setBookingDateTime(LocalDateTime.now());
        response.setTripType(TripType.ONE_WAY);
        response.setSource("DEL");
        response.setDestination("BLR");
        response.setAirlineName("TestAir");

        PassengerResponse p = new PassengerResponse();
        p.setName("A");
        p.setSeatNo("1A");
        p.setAge(30);
        response.setPassengers(List.of(p));

        return response;
    }


    @Test
    void createBooking_success_returns201_andSendsEvent() {
        BookingRequest req = new BookingRequest();
        req.setBookerEmailId("test@test.com");
        req.setTripType(TripType.ONE_WAY);
        req.setPassengers(Collections.emptyList());

        BookingResponse resp = sampleResponse();

        when(bookingService.createBooking(eq("FL1"), any(BookingRequest.class)))
                .thenReturn(resp);

        ResponseEntity<String> result =
                bookingController.createBooking("FL1", req);

      
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("PNR123", result.getBody());

        verify(bookingProducer).sendBookingCreatedEvent(anyString());
    }

  
    @Test
    void getBookingByPnr_found_returns200() {
        BookingResponse resp = sampleResponse();

        when(bookingService.getBookingByPnr("PNR123")).thenReturn(resp);

        ResponseEntity<BookingResponse> result =
                bookingController.getBookingByPnr("PNR123");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("PNR123", result.getBody().getPnr());
    }

    @Test
    void getBookingByPnr_notFound_returns404() {
        when(bookingService.getBookingByPnr("NO_PNR")).thenReturn(null);

        ResponseEntity<BookingResponse> result =
                bookingController.getBookingByPnr("NO_PNR");

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertNull(result.getBody());
    }

    
    @Test
    void getBookingHistory_returnsList200() {
        BookingResponse resp = sampleResponse();

        when(bookingService.getBookingHistory("test@test.com"))
                .thenReturn(List.of(resp));

        ResponseEntity<List<BookingResponse>> result =
                bookingController.getBookingHistory("test@test.com");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
        assertEquals("PNR123", result.getBody().get(0).getPnr());
    }

   
    @Test
    void cancelBooking_success_returns200_andSendsCancelEvent() {
        when(bookingService.cancelBooking("PNR123"))
                .thenReturn("Booking cancelled successfully");

        ResponseEntity<String> result =
                bookingController.cancelBooking("PNR123");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Booking cancelled successfully", result.getBody());

        verify(bookingProducer).sendBookingCancelledEvent(anyString());
    }

    @Test
    void cancelBooking_notFound_returns404_andNoEvent() {
        when(bookingService.cancelBooking("NO_PNR")).thenReturn(null);

        ResponseEntity<String> result =
                bookingController.cancelBooking("NO_PNR");

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertNull(result.getBody());

        verify(bookingService).cancelBooking("NO_PNR");
        verify(bookingProducer, never()).sendBookingCancelledEvent(anyString());
    }
}
