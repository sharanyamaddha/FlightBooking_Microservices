package com.flightservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReserveSeatsRequest {
    @NotNull
    private String bookingReference; // optional cross-ref to booking
    @Min(1)
    private int count;

   
    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
