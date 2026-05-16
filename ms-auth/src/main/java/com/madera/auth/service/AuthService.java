package com.madera.auth.service;

import com.madera.auth.dto.AuthResponse;
import com.madera.auth.dto.ChangePasswordRequest;
import com.madera.auth.dto.LoginRequest;
import com.madera.auth.dto.RegisterRequest;
import com.madera.auth.dto.UsuarioResponse;
import com.madera.auth.model.Rol;
import java.util.List;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    List<UsuarioResponse> listarUsuarios();

    List<UsuarioResponse> listarPorRol(Rol rol);

    void desactivarUsuario(Long id);

    void cambiarPassword(String email, ChangePasswordRequest request);
}
