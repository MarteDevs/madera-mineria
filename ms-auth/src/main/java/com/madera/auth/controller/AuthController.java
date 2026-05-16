package com.madera.auth.controller;

import com.madera.auth.dto.AuthResponse;
import com.madera.auth.dto.ChangePasswordRequest;
import com.madera.auth.dto.LoginRequest;
import com.madera.auth.dto.RegisterRequest;
import com.madera.auth.dto.UsuarioResponse;
import com.madera.auth.model.Rol;
import com.madera.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {
        return ResponseEntity.ok(authService.listarUsuarios());
    }

    @GetMapping("/usuarios/rol/{rol}")
    public ResponseEntity<List<UsuarioResponse>> listarPorRol(@PathVariable Rol rol) {
        return ResponseEntity.ok(authService.listarPorRol(rol));
    }

    @PutMapping("/usuarios/{id}/desactivar")
    public ResponseEntity<Void> desactivarUsuario(@PathVariable Long id) {
        authService.desactivarUsuario(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/password")
    public ResponseEntity<Void> cambiarPassword(
            Authentication authentication,
            @RequestBody @Valid ChangePasswordRequest request) {
        authService.cambiarPassword(authentication.getName(), request);
        return ResponseEntity.ok().build();
    }
}
