package com.notificationservice.kafka;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendBookingConfirmationEmail(String to, String pnr, String airlineName,
                                             int seatsBooked, Double totalAmount, String dateTime) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("Flight Booking Confirmation - " + pnr);

            String htmlMsg = """
                    <h2>Flight Booking Confirmed</h2>
                    <p>Your booking is confirmed!</p>
                    <p><b>PNR:</b> %s</p>
                    <p><b>Airline:</b> %s</p>
                    <p><b>Seats:</b> %d</p>
                    <p><b>Total Amount:</b> %s</p>
                    <p><b>Date & Time:</b> %s</p>
                    """.formatted(pnr, airlineName, seatsBooked, totalAmount, dateTime);

            helper.setText(htmlMsg, true);

            mailSender.send(message);

            log.info("📧 Email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("❌ Failed to send booking confirmation email: {}", e.getMessage());
        }
    }

    public void sendBookingCancellationEmail(String to, String pnr, String airlineName, String cancelledAt) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("Flight Booking Cancelled - " + pnr);

            String htmlMsg = """
                    <h2>Flight Booking Cancelled</h2>
                    <p>Your booking has been cancelled.</p>
                    <p><b>PNR:</b> %s</p>
                    <p><b>Airline:</b> %s</p>
                    <p><b>Cancelled At:</b> %s</p>
                    """.formatted(pnr, airlineName, cancelledAt);

            helper.setText(htmlMsg, true);

            mailSender.send(message);

            log.info("📧 Cancellation email sent to: {}", to);

        } catch (MessagingException e) {
            log.error("❌ Failed to send cancellation email: {}", e.getMessage());
        }
    }
}
