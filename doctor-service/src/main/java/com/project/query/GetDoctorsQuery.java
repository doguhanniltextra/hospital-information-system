package com.project.query;

import org.springframework.data.domain.Pageable;

public record GetDoctorsQuery(Pageable pageable, String specialization) {
}
