package com.bookingservice.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.bookingservice.events.BookingCancelledEvent;
import com.bookingservice.events.BookingCreatedEvent;

import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

@Service
public class BookingEventProducer {

	private static final Logger log = LoggerFactory.getLogger(BookingEventProducer.class);

    private static final String TOPIC_BOOKING_CREATED = "booking-created";
    private static final String TOPIC_BOOKING_CANCELLED = "booking-cancelled";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public BookingEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void sendBookingCreatedEvent(BookingCreatedEvent event) {
        log.info("Sending BookingCreatedEvent to Kafka: {}", event);
        kafkaTemplate.send(TOPIC_BOOKING_CREATED, event.getPnr(), event);
    }

    public void sendBookingCancelledEvent(BookingCancelledEvent event) {
        log.info("Sending BookingCancelledEvent to Kafka: {}", event);
        kafkaTemplate.send(TOPIC_BOOKING_CANCELLED, event.getPnr(), event);
    }
    

}
