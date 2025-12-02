package com.flightservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.flightservice.dto.request.FlightRequest;
import com.flightservice.dto.request.ReleaseSeatsRequest;
import com.flightservice.dto.request.ReserveSeatsRequest;
import com.flightservice.dto.response.FlightResponse;
import com.flightservice.dto.response.ReserveSeatsResponse;
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
	
	 @GetMapping("/flights/{id}")
	    public ResponseEntity<FlightResponse> getFlightById(@PathVariable("id") String id) {
	        FlightResponse response = flightService.getFlightById(id);
	        return ResponseEntity.ok(response);
	    }

	    @PostMapping("/flights/{id}/reserve")
	    public ResponseEntity<ReserveSeatsResponse> reserveSeats(@PathVariable("id") String id,
	                                                             @Valid @RequestBody ReserveSeatsRequest request) {
	        ReserveSeatsResponse res = flightService.reserveSeats(id, request);
	        return ResponseEntity.ok(res);
	    }

	    @PostMapping("/flights/{id}/release")
	    public ResponseEntity<Void> releaseSeats(@PathVariable("id") String id,
	                                             @Valid @RequestBody ReleaseSeatsRequest request) {
	        flightService.releaseSeats(id, request);
	        return ResponseEntity.ok().build();
	    }
}
