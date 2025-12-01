package com.bookingservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.bookingservice.enums.Gender;
import com.bookingservice.enums.MealType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Passenger {

	@Id
	String passengerId;
	
	 String flightId;
	 
	 String pnr;
	 
	 String name;
	 
		int age;
		
		Gender gender;
		
		String seatNo;
		
		MealType mealType;

}
