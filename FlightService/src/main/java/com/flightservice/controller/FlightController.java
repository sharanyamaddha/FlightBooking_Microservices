package com.flightservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.flightservice.dto.request.FlightRequest;
import com.flightservice.dto.response.FlightResponse;
import com.flightservice.model.Flight;
import com.flightservice.service.FlightService;

import jakarta.validation.Valid;

@RestController
public class FlightController {

	@Autowired
	FlightService flightService;
	
	@PostMapping("/flights")
	public ResponseEntity<String> addFlights(@Valid @RequestBody FlightRequest request){
		Flight saved = flightService.addFlights(request);
		return ResponseEntity
						.status(HttpStatus.CREATED)
						.body(saved.getFlightId());
	}
	
	@PostMapping("/flights/search")
	public ResponseEntity<List<FlightResponse>> searchFlights(@RequestBody FlightRequest request) {
	    List<FlightResponse> responses = flightService.searchFlights(request);
	    return ResponseEntity.ok(responses);
	}
	
}
