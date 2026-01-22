package com.enone.security;


import com.enone.application.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String requestURI = request.getRequestURI();
        log.info("JWT Filter - Procesando request: {} {}", request.getMethod(), requestURI);

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("No hay token JWT en el header Authorization para: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        log.info("Token JWT encontrado para: {}", requestURI);

        try {
            if (!jwtService.isTokenValid(jwt)) {
                log.error("Token JWT INVÁLIDO o EXPIRADO para: {}", requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            username = jwtService.extractUsername(jwt);
            Long userId = jwtService.extractUserId(jwt);
            List<String> roles = jwtService.extractRoles(jwt);

            log.info("Token JWT VÁLIDO - Usuario: {} (ID: {}) Roles: {} para: {}", username, userId, roles, requestURI);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        authorities
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.info(" Usuario AUTENTICADO en SecurityContext: {} (ID: {}) con roles: {}", username, userId, roles);
            }

        } catch (Exception e) {
            log.error("ERROR CRÍTICO procesando token JWT para {}: {}", requestURI, e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}