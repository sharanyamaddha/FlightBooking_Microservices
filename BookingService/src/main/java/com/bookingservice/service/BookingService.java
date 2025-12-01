package com.bookingservice.service;

import java.util.List;

import com.bookingservice.dto.request.BookingRequest;
import com.bookingservice.dto.response.BookingResponse;

public interface BookingService {

	
	 BookingResponse createBooking(String flightId, BookingRequest request);

	    BookingResponse getBookingByPnr(String pnr);

	    List<BookingResponse> getBookingHistory(String email);

	    String cancelBooking(String pnr);
}
