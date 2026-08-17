package br.com.accessmap.backend.application.user.dto;

import lombok.Data;

@Data
public class UserRequest {
    private String name;
    private String email;
    private String phone;
    private Integer age;
    private String accessibilityNeeds;
    private String password;
}
