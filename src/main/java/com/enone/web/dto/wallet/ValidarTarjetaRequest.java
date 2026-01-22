package com.enone.web.dto.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidarTarjetaRequest {
    
    @NotBlank(message = "El número de tarjeta es obligatorio")
    @Pattern(regexp = "^[0-9]{16}$", message = "El número de tarjeta debe tener exactamente 16 dígitos")
    private String numeroTarjeta;
    
    @NotBlank(message = "El CVV es obligatorio")
    @Pattern(regexp = "^[0-9]{3}$", message = "El CVV debe tener exactamente 3 dígitos")
    private String cvv;
    
    @NotBlank(message = "La fecha de vencimiento es obligatoria")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/[0-9]{4}$", message = "La fecha debe tener el formato MM/YYYY")
    private String fechaVencimiento;
    
    @NotBlank(message = "El nombre del titular es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre del titular debe tener entre 2 y 100 caracteres")
    private String nombreTitular;
}