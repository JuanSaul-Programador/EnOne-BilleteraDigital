package com.enone.web.controller;

import com.enone.web.dto.admin.AdminMessageRequest;
import com.enone.web.dto.admin.BlockUserRequest;
import com.enone.web.dto.admin.PagedResponse;
import com.enone.web.dto.admin.UserAdminResponse;
import com.enone.web.dto.common.ApiResponse;
import com.enone.exception.ApiException;
import com.enone.application.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserAdminResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        try {
            log.info("Solicitud para listar usuarios - Page: {}, Size: {}, Search: '{}'", page, size, search);
            
            if (page < 0) page = 0;
            if (size < 1) size = 10;
            if (size > 100) size = 100;

            PagedResponse<UserAdminResponse> response = adminService.listUsers(page, size, search);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Error listando usuarios: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error interno al listar usuarios."));
        }
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<String>> enableUser(@PathVariable Long id) {
        try {
            log.info("Solicitud para activar usuario ID: {}", id);
            adminService.enableUser(id);
            log.info("Usuario ID {} procesado por el servicio (activación).", id);
            return ResponseEntity.ok(ApiResponse.success("Usuario activado correctamente."));
            
        } catch (ApiException e) {
            log.error("Error (API) activando usuario ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(e.getStatus()).body(ApiResponse.error(e.getMessage()));
            
        } catch (Exception e) {
            log.error("Error inesperado activando usuario ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error interno al activar el usuario."));
        }
    }

    @PostMapping("/{id}/message")
    public ResponseEntity<ApiResponse<String>> sendMessageToUser(
            @PathVariable Long id,
            @RequestBody @Valid AdminMessageRequest request) {
        try {
            log.info("Solicitud para enviar mensaje a usuario ID: {}", id);
            if (request == null) {
                throw new ApiException(400, "Cuerpo de la solicitud vacío o inválido.");
            }
            adminService.sendMessageToUser(id, request);
            log.info("Mensaje para usuario ID {} procesado por el servicio.", id);
            return ResponseEntity.ok(ApiResponse.success("Mensaje enviado al usuario."));
            
        } catch (ApiException e) {
            log.error("Error (API) enviando mensaje a ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(e.getStatus()).body(ApiResponse.error(e.getMessage()));
            
        } catch (Exception e) {
            log.error("Error inesperado enviando mensaje a ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error interno al enviar el mensaje."));
        }
    }

    @PutMapping("/{id}/block")
    public ResponseEntity<ApiResponse<String>> blockUser(
            @PathVariable Long id,
            @RequestBody @Valid BlockUserRequest request) {
        try {
            log.info("Solicitud para bloquear usuario ID: {}", id);
            if (request == null || request.getReason() == null || request.getReason().isBlank()) {
                throw new ApiException(400, "La razón del bloqueo es requerida.");
            }
            adminService.blockUser(id, request);
            return ResponseEntity.ok(ApiResponse.success("Usuario bloqueado y notificado."));
            
        } catch (ApiException e) {
            log.error("Error (API) bloqueando ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(e.getStatus()).body(ApiResponse.error(e.getMessage()));
            
        } catch (Exception e) {
            log.error("Error inesperado bloqueando ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error interno al bloquear el usuario."));
        }
    }
}