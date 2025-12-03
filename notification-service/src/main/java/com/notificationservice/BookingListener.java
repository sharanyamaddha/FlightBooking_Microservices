package com.notificationservice;


import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class BookingListener {

    @RabbitListener(queues = "booking.queue")
    public void receiveBooking(String message) {
        System.out.println("📩 NotificationService received => " + message);
    }
}
