package com.project.auth_service.dto;

import com.project.auth_service.entity.Role;
import java.util.List;

public class AdminRegisterRequestDto extends RegisterRequestDto {
    private List<Role> roles;

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }
}
