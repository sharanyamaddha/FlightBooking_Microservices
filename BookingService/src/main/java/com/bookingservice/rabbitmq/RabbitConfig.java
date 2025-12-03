package com.bookingservice.rabbitmq;



import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue bookingQueue() {
       
        return new Queue("booking.queue", true); // durable=true
    }
}


