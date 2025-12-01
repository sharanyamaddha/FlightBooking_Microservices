package com.bookingservice.dto.request;


import com.bookingservice.enums.Gender;
import com.bookingservice.enums.MealType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PassengerRequest {


	@NotEmpty
	String name;
	
	@NotNull
	Gender gender;
	
	@NotNull
	int age;
	
	@NotBlank
	String seatNo;
	
	@NotNull
	MealType mealType;
}
