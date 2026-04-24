package com.project.support_service.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "patient_identities", schema = "support_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientIdentity {
    @Id
    private UUID patientId;
    
    @Column(unique = true, nullable = false)
    private String authUserId;
}
