package com.madera.auth.service;

import com.madera.auth.dto.AuthResponse;
import com.madera.auth.dto.ChangePasswordRequest;
import com.madera.auth.dto.LoginRequest;
import com.madera.auth.dto.RegisterRequest;
import com.madera.auth.dto.UsuarioResponse;
import com.madera.auth.model.Rol;
import com.madera.auth.model.Usuario;
import com.madera.auth.repository.UsuarioRepository;
import com.madera.auth.security.JwtService;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new RuntimeException("El email ya esta registrado: " + email);
        }

        Rol rol = request.getRol() != null ? request.getRol() : Rol.ROLE_COMPRAS;
        String mina = request.getMina() != null ? request.getMina().trim() : null;

        if (rol == Rol.ROLE_COMPRAS && (mina == null || mina.isBlank())) {
            throw new RuntimeException("La mina es obligatoria para usuarios con rol ROLE_COMPRAS");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre().trim())
                .apellido(request.getApellido().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(rol)
                .mina(mina)
                .activo(true)
                .build();

        usuarioRepository.save(usuario);
        String token = jwtService.generarToken(usuario);

        return buildAuthResponse(usuario, token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String token = jwtService.generarToken(usuario);
        return buildAuthResponse(usuario, token);
    }

    @Override
    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<UsuarioResponse> listarPorRol(Rol rol) {
        return usuarioRepository.findByRol(rol).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void desactivarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void cambiarPassword(String email, ChangePasswordRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
            throw new RuntimeException("La contrasena actual es incorrecta");
        }

        usuario.setPassword(passwordEncoder.encode(request.getPasswordNueva()));
        usuarioRepository.save(usuario);
    }

    private AuthResponse buildAuthResponse(Usuario usuario, String token) {
        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .rol(usuario.getRol().name())
                .expiracion(System.currentTimeMillis() + jwtService.getExpiration())
                .build();
    }

    private UsuarioResponse mapToResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .rol(usuario.getRol().name())
                .mina(usuario.getMina())
                .activo(usuario.isActivo())
                .fechaCreacion(usuario.getFechaCreacion())
                .build();
    }
}
