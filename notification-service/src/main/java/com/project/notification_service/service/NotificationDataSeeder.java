package com.project.notification_service.service;

import com.project.notification_service.model.NotificationTemplate;
import com.project.notification_service.repository.NotificationTemplateRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Seeds required notification templates on startup if they do not already exist.
 * Ensures that the system has the necessary templates (e.g., Welcome Email) for core workflows.
 */
@Component
public class NotificationDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(NotificationDataSeeder.class);

    private final NotificationTemplateRepository templateRepository;

    /**
     * Initializes the seeder with the template repository.
     * 
     * @param templateRepository Repository for persisting templates
     */
    public NotificationDataSeeder(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * Execution entry point for data seeding.
     * Runs automatically after the bean initialization is complete.
     */
    @PostConstruct
    public void seed() {
        seedPatientWelcome();
    }

    /**
     * Seeds the 'PATIENT_WELCOME' template if it is missing.
     * This template is used when a new patient record is provisioned with an auth account.
     */
    private void seedPatientWelcome() {
        final String code = "PATIENT_WELCOME";
        if (templateRepository.findByTemplateCode(code).isPresent()) {
            log.debug("NotificationDataSeeder: Template '{}' already exists, skipping.", code);
            return;
        }

        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateCode(code);
        template.setChannel("EMAIL");
        template.setSubject("Hastane Sistemine Hoş Geldiniz - Hesabınızı Aktive Edin");
        template.setBody(
                "Merhaba {{name}},\n\n" +
                "Hastane kayıt sistemine başarıyla eklendiniz.\n\n" +
                "Hesabınıza erişmek için aşağıdaki bağlantıyı kullanarak şifrenizi belirlemeniz gerekmektedir:\n\n" +
                "  {{resetLink}}\n\n" +
                "Bu bağlantı 24 saat geçerlidir. Süre dolduktan sonra klinikle iletişime geçiniz.\n\n" +
                "Giriş yapacağınız kullanıcı adınız: {{email}}\n\n" +
                "İyi günler,\n" +
                "Hastane Yönetim Sistemi"
        );

        templateRepository.save(template);
        log.info("NotificationDataSeeder: Seeded template '{}'", code);
    }
}

