package com.bookingservice;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.bookingservice.client.FlightClient;
import com.bookingservice.client.dto.FlightDto;
import com.bookingservice.client.dto.ReleaseSeatsRequest;
import com.bookingservice.client.dto.ReserveSeatsRequest;
import com.bookingservice.client.dto.ReserveSeatsResponse;
import com.bookingservice.dto.request.BookingRequest;
import com.bookingservice.dto.request.PassengerRequest;
import com.bookingservice.dto.response.BookingResponse;
import com.bookingservice.enums.BookingStatus;
import com.bookingservice.enums.TripType;
import com.bookingservice.exceptions.BusinessException;
import com.bookingservice.model.Booking;
import com.bookingservice.model.Passenger;
import com.bookingservice.repository.BookingRepository;
import com.bookingservice.repository.PassengerRepository;
import com.bookingservice.serviceimpl.BookingServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private FlightClient flightClient;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private FlightDto sampleFlight;

    @BeforeEach
    void setUp() {
        sampleFlight = new FlightDto();
        sampleFlight.setFlightId("FL1");
        sampleFlight.setAvailableSeats(5);
        sampleFlight.setPrice(1000.0);
        sampleFlight.setSource("DEL");
        sampleFlight.setDestination("BLR");
        sampleFlight.setAirlineName("TestAir");
    }

    private BookingRequest buildBookingRequest(String bookerEmail, List<PassengerRequest> passengers) {
        BookingRequest req = new BookingRequest();
        req.setBookerEmailId(bookerEmail);
        req.setTripType(TripType.ONE_WAY);
        req.setPassengers(passengers);
        return req;
    }

    private PassengerRequest p(String name, String seat) {
        PassengerRequest pr = new PassengerRequest();
        pr.setName(name);
        pr.setAge(30);
        //pr.setGender("F");
        pr.setSeatNo(seat);
        //pr.setMealType("VEG");
        return pr;
    }
    @Test
    void createBooking_success_reservesAndSaves() {
        // two passengers
        List<PassengerRequest> passengers = Arrays.asList(p("A", "1A"), p("B", "1B"));
        BookingRequest req = buildBookingRequest("u@test.com", passengers);

        when(flightClient.getFlight("FL1")).thenReturn(sampleFlight);
        when(passengerRepository.findByFlightIdAndSeatNoIn(eq("FL1"), anyList())).thenReturn(Collections.emptyList());
        ReserveSeatsResponse rresp = new ReserveSeatsResponse();
        rresp.setSuccess(true);
        rresp.setMessage("Reserved");
        rresp.setReservedSeats(Arrays.asList("1A", "1B")); // reserved seats list
        when(flightClient.reserveSeats(eq("FL1"), any(ReserveSeatsRequest.class))).thenReturn(rresp);

        // booking repository save -> return booking with pnr
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setPnr("PNR-ABC");
            return b;
        });

        // passenger saveAll
        when(passengerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse resp = bookingService.createBooking("FL1", req);

        assertNotNull(resp);
        assertEquals("PNR-ABC", resp.getPnr());
        assertEquals(2, resp.getPassengers().size());
        verify(flightClient).reserveSeats(eq("FL1"), any(ReserveSeatsRequest.class));
        verify(passengerRepository).saveAll(anyList());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_throws_when_reservationFails() {
        List<PassengerRequest> passengers = Arrays.asList(p("A", null));
        BookingRequest req = buildBookingRequest("fail@test", passengers);

        when(flightClient.getFlight("FL1")).thenReturn(sampleFlight);
        //when(passengerRepository.findByFlightIdAndSeatNoIn(eq("FL1"), anyList())).thenReturn(Collections.emptyList());

        // reservation unsuccessful
        ReserveSeatsResponse rresp = new ReserveSeatsResponse();
        rresp.setSuccess(false);
        rresp.setMessage("No seats available");
        rresp.setReservedSeats(Collections.emptyList());
        when(flightClient.reserveSeats(eq("FL1"), any(ReserveSeatsRequest.class))).thenReturn(rresp);

        BusinessException ex = assertThrows(BusinessException.class, () -> bookingService.createBooking("FL1", req));
        assertTrue(ex.getMessage().toLowerCase().contains("reservation") || ex.getMessage().toLowerCase().contains("reserve"));
        verify(flightClient).reserveSeats(eq("FL1"), any(ReserveSeatsRequest.class));
    }
    
    
    @Test
    void createBooking_allows_partialReservation_currentBehavior() {
        List<PassengerRequest> passengers = Arrays.asList(
                p("A", null),
                p("B", null),
                p("C", null)
        );
        BookingRequest req = buildBookingRequest("partial@test", passengers);

        when(flightClient.getFlight("FL1")).thenReturn(sampleFlight);

        // simulate partial reservation: only 1 seat reserved instead of 3
        ReserveSeatsResponse rresp = new ReserveSeatsResponse();
        rresp.setSuccess(true);
        rresp.setMessage("Partial reservation");
        rresp.setReservedSeats(Arrays.asList("1A")); // only 1 seat reserved
        when(flightClient.reserveSeats(eq("FL1"), any(ReserveSeatsRequest.class)))
                .thenReturn(rresp);

        // IMPORTANT: stub bookingRepository.save so it doesn't return null
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setPnr("PNR-PARTIAL");
            return b;
        });

        // also stub passengerRepository.saveAll to avoid NPE or unexpected behavior
        when(passengerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        // 👉 current implementation does NOT throw; it returns a BookingResponse
        BookingResponse resp = bookingService.createBooking("FL1", req);

        assertNotNull(resp);
        assertEquals("PNR-PARTIAL", resp.getPnr());
        // service still processes all passengers
        assertEquals(3, resp.getPassengers().size());

        // verify seat reservation was called
        verify(flightClient).reserveSeats(eq("FL1"), any(ReserveSeatsRequest.class));
    }


    @Test
    void createBooking_throws_when_notEnoughSeats() {
        BookingRequest req = buildBookingRequest("a@b", Arrays.asList(p("A", null), p("B", null), p("C", null), p("D", null), p("E", null), p("F", null)));
        // flight has 5 seats
        when(flightClient.getFlight("FL1")).thenReturn(sampleFlight);

        BusinessException ex = assertThrows(BusinessException.class, () -> bookingService.createBooking("FL1", req));
        assertTrue(ex.getMessage().toLowerCase().contains("not enough seats"));
    }

    @Test
    void createBooking_throws_when_seatConflictLocally() {
        List<PassengerRequest> passengers = Arrays.asList(p("A", "1A"));
        BookingRequest req = buildBookingRequest("x@y", passengers);

        when(flightClient.getFlight("FL1")).thenReturn(sampleFlight);
        // conflict exists
        Passenger existing = new Passenger();
        existing.setSeatNo("1A");
        when(passengerRepository.findByFlightIdAndSeatNoIn(eq("FL1"), anyList())).thenReturn(Arrays.asList(existing));

        BusinessException ex = assertThrows(BusinessException.class, () -> bookingService.createBooking("FL1", req));
        assertTrue(ex.getMessage().toLowerCase().contains("seat(s) already taken"));
    }

    @Test
    void createBooking_compensation_releasesOnPassengerSaveFailure() {
        List<PassengerRequest> passengers = Arrays.asList(p("A", "1A"), p("B", "1B"));
        BookingRequest req = buildBookingRequest("comp@test", passengers);

        when(flightClient.getFlight("FL1")).thenReturn(sampleFlight);
        when(passengerRepository.findByFlightIdAndSeatNoIn(eq("FL1"), anyList())).thenReturn(Collections.emptyList());

        ReserveSeatsResponse rresp = new ReserveSeatsResponse();
        rresp.setSuccess(true);
        when(flightClient.reserveSeats(eq("FL1"), any(ReserveSeatsRequest.class))).thenReturn(rresp);

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setPnr("PNR-COMP");
            return b;
        });

        // simulate failure while saving passengers
        when(passengerRepository.saveAll(anyList())).thenThrow(new RuntimeException("DB down"));

        // when releaseSeats called as compensation, do nothing (successful)
        doNothing().when(flightClient).releaseSeats(eq("FL1"), any(ReleaseSeatsRequest.class));

        BusinessException ex = assertThrows(BusinessException.class, () -> bookingService.createBooking("FL1", req));
        assertTrue(ex.getMessage().toLowerCase().contains("failed to save passengers"));

        // verify compensation release called
        verify(flightClient).releaseSeats(eq("FL1"), any(ReleaseSeatsRequest.class));
    }

    @Test
    void getBookingByPnr_success_mapsPassengersAndFlightInfo() {
        Booking booking = new Booking();
        booking.setPnr("PNR-1");
        booking.setFlightId("FL1");
        booking.setBookerEmailId("a@b");
        booking.setBookingDateTime(LocalDateTime.now());
        booking.setTotalAmount(2000.0);
        booking.setStatus(BookingStatus.BOOKED);
        when(bookingRepository.findByPnr("PNR-1")).thenReturn(Optional.of(booking));
        when(flightClient.getFlight("FL1")).thenReturn(sampleFlight);

        Passenger pas = new Passenger();
        pas.setName("A");
        pas.setSeatNo("1A");
        when(passengerRepository.findByPnr("PNR-1")).thenReturn(Arrays.asList(pas));

        BookingResponse resp = bookingService.getBookingByPnr("PNR-1");
        assertNotNull(resp);
        assertEquals("PNR-1", resp.getPnr());
        assertEquals(1, resp.getPassengers().size());
        assertEquals("DEL", resp.getSource());
    }

    @Test
    void cancelBooking_success_and_releasesSeats() {
        Booking booking = new Booking();
        booking.setPnr("PNR-C");
        booking.setStatus(BookingStatus.BOOKED);
        booking.setBookingDateTime(LocalDateTime.now());
        booking.setFlightId("FL1");

        when(bookingRepository.findByPnr("PNR-C")).thenReturn(Optional.of(booking));
        when(passengerRepository.countByPnr("PNR-C")).thenReturn(2L);
        Passenger p1 = new Passenger();
        p1.setSeatNo("1A");
        Passenger p2 = new Passenger();
        p2.setSeatNo("1B");
        when(passengerRepository.findByPnr("PNR-C")).thenReturn(Arrays.asList(p1, p2));
        doNothing().when(flightClient).releaseSeats(eq("FL1"), any(ReleaseSeatsRequest.class));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        String msg = bookingService.cancelBooking("PNR-C");
        assertEquals("Booking cancelled successfully", msg);
        verify(flightClient).releaseSeats(eq("FL1"), any(ReleaseSeatsRequest.class));
    }

    @Test
    void cancelBooking_throws_when_moreThan24h_old() {
        Booking booking = new Booking();
        booking.setPnr("PNR-OLD");
        booking.setStatus(BookingStatus.BOOKED);
        booking.setBookingDateTime(LocalDateTime.now().minus(25, ChronoUnit.HOURS));
        booking.setFlightId("FL1");

        when(bookingRepository.findByPnr("PNR-OLD")).thenReturn(Optional.of(booking));

        var ex = assertThrows(com.bookingservice.exceptions.BadRequestException.class, () -> bookingService.cancelBooking("PNR-OLD"));
        assertTrue(ex.getMessage().toLowerCase().contains("cancellation allowed only within 24 hours"));
    }

    @Test
    void cancelBooking_throws_when_alreadyCancelled() {
        Booking booking = new Booking();
        booking.setPnr("PNR-X");
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setBookingDateTime(LocalDateTime.now());
        when(bookingRepository.findByPnr("PNR-X")).thenReturn(Optional.of(booking));

        var ex = assertThrows(com.bookingservice.exceptions.ConflictException.class, () -> bookingService.cancelBooking("PNR-X"));
        assertTrue(ex.getMessage().toLowerCase().contains("already cancelled"));
    }
}
