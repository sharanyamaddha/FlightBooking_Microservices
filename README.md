# Flight Booking Microservices System

A scalable, production-ready microservices architecture built using Spring Boot, Eureka Service Registry, API Gateway, and Feign Clients.
The system allows users to search flights, reserve seats, book tickets, view booking history, and cancel bookings — all through loosely coupled, independently deployable services.

## Microservices in This Project
<img width="550" height="700" alt="d2 (1)" src="https://github.com/user-attachments/assets/3ac1886b-df83-40ce-a54f-d82ebea2e24a" />


### 1️. Flight Service

Manages:

Flight details

Available seats

Seat reservation

Seat release (on cancellation or failure)

### 2️. Booking Service

Handles:

Booking creation

Seat locking (via Flight Service)

Passenger management

Cancellation (with seat release)

Booking history & PNR lookup

### 3️. API Gateway (Spring Cloud Gateway)

Provides:

Routing

Load balancing

Single entry point to all services

### 4️. Service Registry (Eureka Server)

Responsible for:

Service discovery

Dynamic service registration

Load balancing

Single entry point to all services


