package com.project.query;

import com.project.model.ServiceType;

public record GetAvailableDoctorsQuery(String start, String end, ServiceType serviceType) {
}
