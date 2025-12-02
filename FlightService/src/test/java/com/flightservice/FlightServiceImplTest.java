package com.flightservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.flightservice.exceptions.BusinessException;
import com.flightservice.model.Airline;
import com.flightservice.model.Flight;
import com.flightservice.repository.AirlineRepository;
import com.flightservice.repository.FlightRepository;
import com.flightservice.serviceImpl.FlightServiceImpl;
import com.flightservice.dto.request.FlightRequest;
import com.flightservice.dto.request.ReleaseSeatsRequest;
import com.flightservice.dto.request.ReserveSeatsRequest;
import com.flightservice.dto.response.ReserveSeatsResponse;
import com.flightservice.dto.response.FlightResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class FlightServiceImplTest {

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private AirlineRepository airlineRepository;

    @InjectMocks
    private FlightServiceImpl flightService;

    private FlightRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new FlightRequest();
        validRequest.setAirlineName("TestAir");
        validRequest.setSource("DEL");
        validRequest.setDestination("BLR");
        validRequest.setDepartureDateTime(LocalDateTime.now().plusDays(1));
        validRequest.setArrivalDateTime(LocalDateTime.now().plusDays(1).plusHours(2));
        validRequest.setTotalSeats(100);
        validRequest.setPrice(5000.0);
    }

    @Test
    void addFlights_success_createsFlightAndAirlineIfNotExists() {
        // airline not present
        when(airlineRepository.findByAirlineNameIgnoreCase("TestAir")).thenReturn(Optional.empty());
        when(airlineRepository.save(any(Airline.class))).thenAnswer(inv -> {
            Airline a = inv.getArgument(0);
            a.setAirlineId(UUID.randomUUID().toString());
            return a;
        });
        when(flightRepository.existsByAirlineIdAndSourceAndDestinationAndDepartureDateTime(anyString(), anyString(), anyString(), any()))
                .thenReturn(false);
        when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> {
            Flight f = inv.getArgument(0);
            f.setFlightId(UUID.randomUUID().toString());
            return f;
        });

        var saved = flightService.addFlights(validRequest);

        assertNotNull(saved);
        assertEquals("DEL", saved.getSource());
        assertEquals("BLR", saved.getDestination());
        verify(airlineRepository).save(any(Airline.class));
        verify(flightRepository).save(any(Flight.class));
    }

    @Test
    void addFlights_throwsWhenArrivalBeforeDeparture() {
        FlightRequest r = new FlightRequest();
        r.setAirlineName("TestAir");
        r.setSource("A");
        r.setDestination("B");
        r.setDepartureDateTime(LocalDateTime.now().plusDays(2));
        r.setArrivalDateTime(LocalDateTime.now().plusDays(1)); // arrival before departure

        BusinessException ex = assertThrows(BusinessException.class, () -> flightService.addFlights(r));
        assertTrue(ex.getMessage().toLowerCase().contains("arrival time"));
    }

    @Test
    void reserveSeats_success_decrementsAvailable() {
        Flight flight = new Flight();
        flight.setFlightId("F1");
        flight.setAvailableSeats(10);
        when(flightRepository.findById("F1")).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> inv.getArgument(0));

        ReserveSeatsRequest req = new ReserveSeatsRequest();
        req.setCount(3);

        var resp = flightService.reserveSeats("F1", req);
        assertTrue(resp.isSuccess());
        assertEquals(3, resp.getSeatsReserved());
        assertEquals(7, resp.getRemainingSeats());
        verify(flightRepository).save(any(Flight.class));
    }

    @Test
    void reserveSeats_throwsWhenNotEnoughSeats() {
        Flight flight = new Flight();
        flight.setFlightId("F1");
        flight.setAvailableSeats(1);
        when(flightRepository.findById("F1")).thenReturn(Optional.of(flight));

        ReserveSeatsRequest req = new ReserveSeatsRequest();
        req.setCount(2);

        BusinessException ex = assertThrows(BusinessException.class, () -> flightService.reserveSeats("F1", req));
        assertTrue(ex.getMessage().toLowerCase().contains("not enough seats"));
    }

    @Test
    void releaseSeats_incrementsAvailable() {
        Flight flight = new Flight();
        flight.setFlightId("F1");
        flight.setAvailableSeats(5);
        when(flightRepository.findById("F1")).thenReturn(Optional.of(flight));
        when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> inv.getArgument(0));

        ReleaseSeatsRequest req = new ReleaseSeatsRequest();
        req.setCount(4);

        flightService.releaseSeats("F1", req);

        ArgumentCaptor<Flight> captor = ArgumentCaptor.forClass(Flight.class);
        verify(flightRepository).save(captor.capture());
        assertEquals(9, captor.getValue().getAvailableSeats());
    }

    @Test
    void getFlightById_mapsToResponse() {
        Flight f = new Flight();
        f.setFlightId("F1");
        f.setAirlineName("TestAir");
        f.setSource("SRC");
        f.setDestination("DST");
        f.setDepartureDateTime(LocalDateTime.now().plusDays(1));
        f.setArrivalDateTime(LocalDateTime.now().plusDays(1).plusHours(2));
        f.setAvailableSeats(50);
        f.setPrice(1234.0);

        when(flightRepository.findById("F1")).thenReturn(Optional.of(f));

        FlightResponse resp = flightService.getFlightById("F1");
        assertNotNull(resp);
        assertEquals("TestAir", resp.getAirlineName());
        assertEquals(50, resp.getAvailableSeats());
    }
}
