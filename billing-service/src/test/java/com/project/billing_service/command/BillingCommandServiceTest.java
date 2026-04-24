package com.project.billing_service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.billing_service.client.ClaimClient;
import com.project.billing_service.dto.AppointmentDTO;
import com.project.billing_service.model.Claim;
import com.project.billing_service.model.ClaimStatus;
import com.project.billing_service.model.Invoice;
import com.project.billing_service.repository.BillingOutboxRepository;
import com.project.billing_service.repository.ClaimRepository;
import com.project.billing_service.repository.InvoiceRepository;
import com.project.billing_service.repository.UnbilledChargeRepository;
import com.project.billing_service.strategy.InsuranceCalculationResult;
import com.project.billing_service.strategy.InsuranceFactory;
import com.project.billing_service.strategy.InsuranceStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BillingCommandServiceTest {

    @Mock
    private InvoiceService invoiceService;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private UnbilledChargeRepository unbilledChargeRepository;
    @Mock
    private BillingOutboxRepository outboxRepository;
    @Mock
    private InsuranceFactory insuranceFactory;
    @Mock
    private ClaimClient claimClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BillingCommandService billingCommandService;

    @Test
    public void processPaymentUpdate_WhenInsuranceAmountIsZero_DoesNotCreateClaim() {
        AppointmentDTO appointmentDTO = getAppointmentDTO();
        InsuranceCalculationResult split = new InsuranceCalculationResult(new BigDecimal("100.00"), BigDecimal.ZERO);

        when(insuranceFactory.getStrategy(anyString(), anyString())).thenReturn(mock(InsuranceStrategy.class));
        when(insuranceFactory.getStrategy(anyString(), anyString()).calculate(any(BigDecimal.class))).thenReturn(split);
        when(invoiceService.generateInvoice(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(Path.of("invoices", "test.pdf"));

        billingCommandService.processPaymentUpdate(appointmentDTO);

        verify(invoiceRepository).save(any(Invoice.class));
        verify(claimRepository, never()).save(any(Claim.class));
        verify(claimClient, never()).submitClaim(any());
    }

    @Test
    public void processPaymentUpdate_WhenInsuranceAmountExistsAndClaimSucceeds_SavesSubmittedClaim() {
        AppointmentDTO appointmentDTO = getAppointmentDTO();
        InsuranceCalculationResult split = new InsuranceCalculationResult(new BigDecimal("20.00"), new BigDecimal("80.00"));

        InsuranceStrategy strategy = mock(InsuranceStrategy.class);
        when(insuranceFactory.getStrategy(anyString(), anyString())).thenReturn(strategy);
        when(strategy.calculate(any(BigDecimal.class))).thenReturn(split);
        when(invoiceService.generateInvoice(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(Path.of("invoices", "test.pdf"));

        billingCommandService.processPaymentUpdate(appointmentDTO);

        ArgumentCaptor<Claim> captor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(captor.capture());

        Claim savedClaim = captor.getValue();
        assertThat(savedClaim.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
    }

    @Test
    public void processPaymentUpdate_WhenClaimFails_SavesFailedClaim() {
        AppointmentDTO appointmentDTO = getAppointmentDTO();
        InsuranceCalculationResult split = new InsuranceCalculationResult(new BigDecimal("20.00"), new BigDecimal("80.00"));

        InsuranceStrategy strategy = mock(InsuranceStrategy.class);
        when(insuranceFactory.getStrategy(anyString(), anyString())).thenReturn(strategy);
        when(strategy.calculate(any(BigDecimal.class))).thenReturn(split);
        when(invoiceService.generateInvoice(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(Path.of("invoices", "test.pdf"));
        
        doThrow(new RuntimeException("API Down")).when(claimClient).submitClaim(any());

        billingCommandService.processPaymentUpdate(appointmentDTO);

        ArgumentCaptor<Claim> captor = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(captor.capture());

        Claim savedClaim = captor.getValue();
        assertThat(savedClaim.getStatus()).isEqualTo(ClaimStatus.FAILED);
    }

    private static AppointmentDTO getAppointmentDTO() {
        AppointmentDTO appointmentDTO = new AppointmentDTO();
        appointmentDTO.setDoctorId(UUID.randomUUID().toString());
        appointmentDTO.setPatientId(UUID.randomUUID().toString());
        appointmentDTO.setAmount(100.00);
        appointmentDTO.setInsuranceProviderType("PRIVATE");
        appointmentDTO.setProviderName("Allianz");
        appointmentDTO.setPaymentStatus(true);
        return appointmentDTO;
    }
}
