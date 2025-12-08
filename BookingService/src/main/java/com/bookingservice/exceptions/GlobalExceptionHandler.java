package com.bookingservice.exceptions;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<String> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<String> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<String> handleBusiness(BusinessException ex) {

		String msg = ex.getMessage();

		// Special case: circuit breaker / flight-service failure
		if (msg != null && msg.contains("Flight service is temporarily unavailable")) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE) // 503
					.body(msg);
		}

		// All other business errors (like seat taken, invalid PNR, etc.)
		return ResponseEntity.status(HttpStatus.BAD_REQUEST) // 400
				.body(msg);
	}


}
