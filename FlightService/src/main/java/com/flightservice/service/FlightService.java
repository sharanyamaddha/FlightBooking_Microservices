package com.flightservice.service;

import java.util.List;

import com.flightservice.dto.request.FlightRequest;
import com.flightservice.dto.request.ReleaseSeatsRequest;
import com.flightservice.dto.request.ReserveSeatsRequest;
import com.flightservice.dto.response.FlightResponse;
import com.flightservice.dto.response.ReserveSeatsResponse;
import com.flightservice.model.Flight;



public interface FlightService {

Flight addFlights(FlightRequest request);
	
	List<FlightResponse> searchFlights(FlightRequest request);
	FlightResponse getFlightById(String id);

    ReserveSeatsResponse reserveSeats(String flightId, ReserveSeatsRequest request);

    void releaseSeats(String flightId, ReleaseSeatsRequest request);
}
