package com.project.notification_service.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.util.Map;

/**
 * Service for processing templates using Thymeleaf.
 * Enables dynamic content generation by replacing placeholders with provided variables.
 */
@Service
public class TemplateService {
    private final TemplateEngine templateEngine;

    /**
     * Initializes the template service with a Thymeleaf template engine.
     * 
     * @param templateEngine The engine used for string template processing
     */
    public TemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Processes a string template by injecting variables.
     * 
     * @param templateContent The raw template string containing placeholders
     * @param variables Map of data to be injected into the template
     * @return The processed string with all placeholders resolved
     */
    public String process(String templateContent, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateContent, context);
    }
}

