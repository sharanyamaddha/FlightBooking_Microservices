package com.flightservice.dto.response;

import lombok.Data;

@Data
public class ReserveSeatsResponse {
    private boolean success;
    private String reservationReference; // optional id
    private int seatsReserved;
    private int remainingSeats;

  
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getReservationReference() { return reservationReference; }
    public void setReservationReference(String reservationReference) { this.reservationReference = reservationReference; }
    public int getSeatsReserved() { return seatsReserved; }
    public void setSeatsReserved(int seatsReserved) { this.seatsReserved = seatsReserved; }
    public int getRemainingSeats() { return remainingSeats; }
    public void setRemainingSeats(int remainingSeats) { this.remainingSeats = remainingSeats; }
}
