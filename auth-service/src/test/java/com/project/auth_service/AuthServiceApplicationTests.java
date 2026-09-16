package com.project.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import com.project.auth_service.kafka.PatientCreatedKafkaConsumer;

@ActiveProfiles("test")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "app.secret=dummy-secret-key-at-least-256-bits-long-dummy-secret-key"
})
@org.springframework.boot.autoconfigure.ImportAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.class
})
class AuthServiceApplicationTests {

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private PatientCreatedKafkaConsumer patientCreatedKafkaConsumer;

	@Test
	void contextLoads() {
	}

}
