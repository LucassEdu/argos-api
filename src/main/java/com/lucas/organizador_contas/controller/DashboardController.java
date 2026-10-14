package com.lucas.organizador_contas.controller;

import com.lucas.organizador_contas.dto.DashboardResponse;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.repository.UsuarioRepository;
import com.lucas.organizador_contas.service.AssinaturaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AssinaturaService assinaturaService;
    private final UsuarioRepository usuarioRepository;

    public DashboardController(AssinaturaService assinaturaService, UsuarioRepository usuarioRepository) {
        this.assinaturaService = assinaturaService;
        this.usuarioRepository = usuarioRepository;
    }

    // Método utilitário de segurança idêntico ao do AssinaturaController
    private Usuario obterUsuarioSeguro(Authentication authentication) {
        String identificador = authentication.getName();
        Object principal = authentication.getPrincipal();
        if (principal instanceof Usuario) {
            identificador = ((Usuario) principal).getEmail();
        }
        identificador = identificador.trim().replace("\"", "");
        
        String emailFinal = identificador;
        return usuarioRepository.findByEmail(emailFinal)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado na base de dados."));
    }

    @GetMapping
    public ResponseEntity<?> obterDadosDashboard(Authentication authentication) {
        try {
            Usuario usuario = obterUsuarioSeguro(authentication);
            Long workspaceId = usuario.getWorkspace().getId();
            Long usuarioId = usuario.getId();

            DashboardResponse dados = assinaturaService.obterDadosDashboard(workspaceId, usuarioId);
            return ResponseEntity.ok(dados);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao carregar dashboard: " + e.getMessage());
        }
    }
}