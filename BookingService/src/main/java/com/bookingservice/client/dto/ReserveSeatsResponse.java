package com.bookingservice.client.dto;

import java.util.List;

import lombok.Data;

@Data
public class ReserveSeatsResponse {

	private boolean success;
    private String message;
    private List<String> reservedSeats;

}
