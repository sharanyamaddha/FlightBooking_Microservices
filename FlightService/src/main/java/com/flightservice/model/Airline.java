package com.flightservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


import lombok.Data;

@Document
@Data
public class Airline {


	@Id
	String airlineId;
	
	
	String airlineName;
}
