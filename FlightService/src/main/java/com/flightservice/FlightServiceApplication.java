package com.flightservice;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.MongoDatabaseFactory;

@SpringBootApplication
public class FlightServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlightServiceApplication.class, args);
    }
    
    
    
    
    
    
    
    
    
    
    

    
    
    
    
    
    
    
    

    // Create a MongoClient from the same URI we want to use
    @Bean
    public MongoClient mongoClient(Environment env) {
        String uri = env.getProperty("spring.data.mongodb.uri", "mongodb://localhost:27017/flight_microdb");
        return MongoClients.create(uri);
    }

    // Create a MongoDatabaseFactory from the client and database name
    @Bean
    public MongoDatabaseFactory mongoDbFactory(MongoClient mongoClient, Environment env) {
        String dbName = env.getProperty("spring.data.mongodb.database", "flight_microdb");
        // SimpleMongoClientDatabaseFactory will use the provided client and DB name
        return new SimpleMongoClientDatabaseFactory(mongoClient, dbName);
    }

    // Primary bean named 'mongoTemplate' so Spring Data repositories find it
    @Bean(name = "mongoTemplate")
    @Primary
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDbFactory) {
        return new MongoTemplate(mongoDbFactory);
    }

    // Diagnostic runner to print what DB is actually used
    @Bean
    public CommandLineRunner showMongoInfo(MongoTemplate mongoTemplate, Environment env) {
        return args -> {
            System.out.println("==============================================");
            System.out.println(">>> MongoTemplate DB: " + mongoTemplate.getDb().getName());
            System.out.println(">>> spring.data.mongodb.uri: " + env.getProperty("spring.data.mongodb.uri"));
            System.out.println(">>> spring.data.mongodb.database: " + env.getProperty("spring.data.mongodb.database"));
            System.out.println(">>> Active profiles: " + String.join(",", env.getActiveProfiles()));
            System.out.println("==============================================");
        };
    }
}
