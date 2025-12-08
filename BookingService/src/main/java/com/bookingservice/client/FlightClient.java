package com.bookingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.bookingservice.client.dto.FlightDto;
import com.bookingservice.client.dto.ReleaseSeatsRequest;
import com.bookingservice.client.dto.ReserveSeatsRequest;
import com.bookingservice.client.dto.ReserveSeatsResponse;


@FeignClient( name = "flightClient",
url = "${flight.service.url}",
path = "/flights") 
public interface FlightClient {

    @GetMapping("/{id}")
    FlightDto getFlight(@PathVariable("id") String flightId);

    @PostMapping("/{id}/reserve")
    ReserveSeatsResponse reserveSeats(@PathVariable("id") String flightId,
                                      @RequestBody ReserveSeatsRequest request);

    @PostMapping("/{id}/release")
    void releaseSeats(@PathVariable("id") String flightId,
                      @RequestBody ReleaseSeatsRequest request);
}
