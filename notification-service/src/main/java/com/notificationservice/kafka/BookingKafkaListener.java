package com.notificationservice.kafka;



import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.notificationservice.events.BookingCreatedEvent;
import com.notificationservice.events.BookingCancelledEvent;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class BookingKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(BookingKafkaListener.class);

    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    public BookingKafkaListener(EmailService emailService) {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())  
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.emailService = emailService;
    }


    @KafkaListener(topics = "booking-created", groupId = "notification-service-group")
    public void handleBookingCreated(String message) {

        log.info("✅ Received from topic booking-created => {}", message);

        try {
            BookingCreatedEvent event = objectMapper.readValue(message, BookingCreatedEvent.class);

            emailService.sendBookingConfirmationEmail(
                    event.getBookerEmailId(),
                    event.getPnr(),
                    event.getAirlineName(),
                    event.getSeatsBooked(),
                    event.getTotalAmount(),
                    event.getBookingDateTime().toString()
            );

        } catch (Exception e) {
            log.error("❌ Failed to parse booking-created event: {}", e.getMessage(), e);
        }
    }


    @KafkaListener(topics = "booking-cancelled", groupId = "notification-service-group")
    public void handleBookingCancelled(String message, ConsumerRecord<String, String> record) throws JsonProcessingException {
        log.info("✅ Received from topic {} => {}", record.topic(), message);

        BookingCancelledEvent event = objectMapper.readValue(message, BookingCancelledEvent.class);

        emailService.sendBookingCancellationEmail(
                event.getBookerEmailId(),
                event.getPnr(),
                event.getAirlineName(),
                event.getCancelledAt().toString()
        );
    }
}

