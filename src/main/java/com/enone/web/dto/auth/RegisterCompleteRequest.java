package com.enone.web.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterCompleteRequest {
    
    @NotNull(message = "El ID de sesión es obligatorio")
    private Long sessionId;
    
    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "^(DNI|CE|PASSPORT)$", message = "Tipo de documento inválido")
    private String documentType;
    
    @NotBlank(message = "El número de documento es obligatorio")
    @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener exactamente 8 dígitos")
    private String documentNumber;
    
    @NotBlank(message = "Los nombres son obligatorios")
    @Size(min = 2, max = 60, message = "Los nombres deben tener entre 2 y 60 caracteres")
    private String firstName;
    
    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 60, message = "Los apellidos deben tener entre 2 y 60 caracteres")
    private String lastName;
    
    @Pattern(regexp = "^(M|F|O)$", message = "Género inválido")
    private String gender;
}