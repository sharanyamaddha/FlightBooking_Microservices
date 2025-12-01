package com.flightservice.service;

import java.util.List;

import com.flightservice.dto.request.FlightRequest;
import com.flightservice.dto.response.FlightResponse;
import com.flightservice.model.Flight;



public interface FlightService {

Flight addFlights(FlightRequest request);
	
	List<FlightResponse> searchFlights(FlightRequest request);
}
