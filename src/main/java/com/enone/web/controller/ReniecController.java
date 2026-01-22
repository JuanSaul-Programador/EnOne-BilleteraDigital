package com.enone.web.controller;

import com.enone.web.dto.common.ApiResponse;
import com.enone.web.dto.wallet.ReniecResponse;
import com.enone.domain.model.MockReniec;
import com.enone.exception.ApiException;
import com.enone.domain.repository.MockReniecRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/public/reniec")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReniecController {

    private final MockReniecRepository reniecRepository;

    @GetMapping("/consulta/{dni}")
    public ResponseEntity<ApiResponse<ReniecResponse>> consultarDni(@PathVariable String dni) {
        log.info("Consultando DNI: {}", dni);
        
        if (dni == null || dni.trim().length() != 8) {
            throw new ApiException(400, "Formato de DNI inválido. Debe tener 8 dígitos.");
        }
        
        Optional<MockReniec> reniecData = reniecRepository.findByDni(dni);
        
        if (reniecData.isEmpty()) {
            throw new ApiException(404, "DNI no encontrado en nuestros registros.");
        }
        
        MockReniec data = reniecData.get();
       
        ReniecResponse responseDto = ReniecResponse.builder()
                .dni(data.getDni())
                .nombres(data.getNombres())
                .apellidos(data.getApellidos())
                .fechaNacimiento(data.getFechaNacimiento()) 
                .build();
        
        log.info("DNI {} encontrado: {} {}", dni, data.getNombres(), data.getApellidos());
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }
}