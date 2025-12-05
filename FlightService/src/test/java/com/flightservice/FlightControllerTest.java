package com.flightservice;

import com.flightservice.controller.FlightController;
import com.flightservice.dto.request.FlightRequest;
import com.flightservice.dto.request.ReleaseSeatsRequest;
import com.flightservice.dto.request.ReserveSeatsRequest;
import com.flightservice.dto.response.FlightResponse;
import com.flightservice.dto.response.ReserveSeatsResponse;
import com.flightservice.model.Flight;
import com.flightservice.service.FlightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FlightControllerTest {

	@Mock
	private FlightService flightService;

	@InjectMocks
	private FlightController flightController;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	private FlightRequest sampleFlightRequest() {
		FlightRequest req = new FlightRequest();

		return req;
	}

	private Flight sampleFlight() {
		Flight f = new Flight();
		f.setFlightId("FL1");
		return f;
	}

	private FlightResponse sampleFlightResponse() {
		FlightResponse resp = new FlightResponse();
		resp.setFlightNo("FL1");
		// set other fields if needed
		return resp;
	}

	@Test
	void addFlights_success_returns201_andFlightId() {
		FlightRequest req = sampleFlightRequest();
		Flight saved = sampleFlight();

		when(flightService.addFlights(any(FlightRequest.class))).thenReturn(saved);

		ResponseEntity<String> result = flightController.addFlights(req);

		assertEquals(HttpStatus.CREATED, result.getStatusCode());
		assertEquals("FL1", result.getBody());

		verify(flightService).addFlights(any(FlightRequest.class));
	}

	@Test
	void searchFlights_returns200_withList() {
		FlightRequest req = sampleFlightRequest();
		FlightResponse resp = sampleFlightResponse();

		when(flightService.searchFlights(any(FlightRequest.class))).thenReturn(List.of(resp));

		ResponseEntity<List<FlightResponse>> result = flightController.searchFlights(req);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertNotNull(result.getBody());
		assertEquals(1, result.getBody().size());
		assertEquals("FL1", result.getBody().get(0).getFlightNo());

		verify(flightService).searchFlights(any(FlightRequest.class));
	}

	@Test
	void getFlightById_returns200_andBody() {
		FlightResponse resp = sampleFlightResponse();

		when(flightService.getFlightById("FL1")).thenReturn(resp);

		ResponseEntity<FlightResponse> result = flightController.getFlightById("FL1");

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertNotNull(result.getBody());
		assertEquals("FL1", result.getBody().getFlightNo());

		verify(flightService).getFlightById("FL1");
	}

	@Test
	void reserveSeats_returns200_andResponse() {
		ReserveSeatsRequest req = new ReserveSeatsRequest();

		ReserveSeatsResponse resp = new ReserveSeatsResponse();
		resp.setSuccess(true);
		resp.setReservationReference("RSV123");
		resp.setSeatsReserved(2);
		resp.setRemainingSeats(48);

		when(flightService.reserveSeats(eq("FL1"), any(ReserveSeatsRequest.class))).thenReturn(resp);

		ResponseEntity<ReserveSeatsResponse> result = flightController.reserveSeats("FL1", req);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertNotNull(result.getBody());

		ReserveSeatsResponse body = result.getBody();

		assertTrue(body.isSuccess());
		assertEquals("RSV123", body.getReservationReference());
		assertEquals(2, body.getSeatsReserved());
		assertEquals(48, body.getRemainingSeats());

		verify(flightService).reserveSeats(eq("FL1"), any(ReserveSeatsRequest.class));
	}

	@Test
	void releaseSeats_returns200_andCallsService() {
		ReleaseSeatsRequest req = new ReleaseSeatsRequest();

		doNothing().when(flightService).releaseSeats(eq("FL1"), any(ReleaseSeatsRequest.class));

		ResponseEntity<Void> result = flightController.releaseSeats("FL1", req);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertNull(result.getBody());

		verify(flightService).releaseSeats(eq("FL1"), any(ReleaseSeatsRequest.class));
	}
}
