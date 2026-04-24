package com.project.notification_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private TemplateEngine templateEngine;

    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(templateEngine);
    }

    @Test
    void process_CallsTemplateEngine() {
        // Arrange
        String template = "Hello [[${name}]]";
        Map<String, Object> variables = Map.of("name", "John");
        String expected = "Hello John";

        when(templateEngine.process(eq(template), any(Context.class))).thenReturn(expected);

        // Act
        String result = templateService.process(template, variables);

        // Assert
        assertEquals(expected, result);
    }
}
