package br.com.accessmap.backend.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String name;
    private String email;
    private String phone;
    private Integer age;
    private String accessibilityNeeds;
    private String password;
    private String createdAt;
    private String updatedAt;
}
