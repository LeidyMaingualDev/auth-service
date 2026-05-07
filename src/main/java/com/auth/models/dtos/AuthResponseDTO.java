package com.auth.models.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponseDTO {

    private String token;
    private String refreshToken;
    private String email;
    private String name;
    private String role;
}