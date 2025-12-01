package com.bookingservice.dto.response;

import com.bookingservice.enums.Gender;
import com.bookingservice.enums.MealType;

import lombok.Data;

@Data
public class PassengerResponse {


	String name;
	int age;
	Gender gender;
	String seatNo;
	MealType mealType;
}
