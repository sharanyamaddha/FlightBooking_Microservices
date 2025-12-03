package com.bookingservice.rabbitmq;



import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BookingProducer {

    private final AmqpTemplate rabbitTemplate;

    @Value("${booking.queue}")
    private String bookingQueue;

    public BookingProducer(AmqpTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendBookingCreatedEvent(String message) {
        rabbitTemplate.convertAndSend(bookingQueue, message);
        System.out.println("📤 [BookingService] Sent to RabbitMQ => " + message);
    }

    public void sendBookingCancelledEvent(String message) {
        rabbitTemplate.convertAndSend(bookingQueue, message);
        System.out.println("📤 [BookingService] Sent CANCEL event => " + message);
    }
}
