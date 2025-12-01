package com.bookingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import com.bookingservice.client.dto.FlightDto;
import com.bookingservice.client.dto.ReserveSeatsRequest;
import com.bookingservice.client.dto.ReserveSeatsResponse;
import com.bookingservice.client.dto.ReleaseSeatsRequest;

@FeignClient(name = "flight-service") // serviceId registered in Eureka
public interface FlightClient {

    @GetMapping("/api/flights/{id}")
    FlightDto getFlight(@PathVariable("id") String flightId);

    @PostMapping("/api/flights/{id}/reserve")
    ReserveSeatsResponse reserveSeats(@PathVariable("id") String flightId,
                                      @RequestBody ReserveSeatsRequest request);

    @PostMapping("/api/flights/{id}/release")
    void releaseSeats(@PathVariable("id") String flightId,
                      @RequestBody ReleaseSeatsRequest request);
}
