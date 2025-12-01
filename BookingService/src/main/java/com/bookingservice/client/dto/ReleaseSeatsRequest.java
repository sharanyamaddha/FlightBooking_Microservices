package com.bookingservice.client.dto;

import java.util.List;

import lombok.Data;

@Data
public class ReleaseSeatsRequest {

	private String bookingReference;
    private int count;
    private List<String> seatNumbers;
}
