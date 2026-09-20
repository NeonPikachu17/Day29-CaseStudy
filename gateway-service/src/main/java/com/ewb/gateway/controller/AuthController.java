package com.ewb.gateway.controller;

import com.ewb.common.dto.AuthRequest;
import com.ewb.common.dto.AuthResponse;
import com.ewb.common.security.JwtUtil;
import com.ewb.common.security.SecurityConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping({"/auth", "/api/auth"})
public class AuthController {

    private final JwtUtil jwtUtil;

    // Supported Personas as specified in contracts.md Section 2
    private static final Map<String, PersonaInfo> PERSONAS = new HashMap<>();

    static {
        PERSONAS.put("asuna", new PersonaInfo(
                "asuna",
                "Asuna Yuuki",
                SecurityConstants.ROLE_CUSTOMER,
                List.of("EWB-ASU-1001", "EWB-ASU-2001")
        ));
        PERSONAS.put("kirito", new PersonaInfo(
                "kirito",
                "Kazuto Kirigaya",
                SecurityConstants.ROLE_CUSTOMER,
                List.of("EWB-KIR-5001")
        ));
        PERSONAS.put("klein", new PersonaInfo(
                "klein",
                "Ryoutarou Tsuboi",
                SecurityConstants.ROLE_CUSTOMER,
                List.of("EWB-KLN-4001")
        ));
        PERSONAS.put("heathcliff", new PersonaInfo(
                "heathcliff",
                "Akihiko Kayaba",
                SecurityConstants.ROLE_CUSTOMER,
                List.of("EWB-HTH-3001")
        ));
        PERSONAS.put("agil", new PersonaInfo(
                "agil",
                "Andrew Gilbert Mills",
                SecurityConstants.ROLE_OPERATIONS,
                Collections.emptyList()
        ));
        PERSONAS.put("sinon", new PersonaInfo(
                "sinon",
                "Shino Asada",
                SecurityConstants.ROLE_AUDITOR,
                Collections.emptyList()
        ));
    }

    public AuthController() {
        this.jwtUtil = new JwtUtil();
    }

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "BAD_REQUEST", "message", "Username is required"));
        }

        String username = request.getUsername().trim().toLowerCase();
        PersonaInfo persona = PERSONAS.get(username);

        if (persona == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "UNAUTHORIZED", "message", "Unknown persona: " + username));
        }

        String token = jwtUtil.generateToken(persona.username, persona.role, persona.accountIds);

        AuthResponse response = new AuthResponse(
                token,
                persona.username,
                persona.fullName,
                persona.role,
                persona.accountIds
        );

        return ResponseEntity.ok(response);
    }

    public static record PersonaInfo(
            String username,
            String fullName,
            String role,
            List<String> accountIds
    ) {}
}
