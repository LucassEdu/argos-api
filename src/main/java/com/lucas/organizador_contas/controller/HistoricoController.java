package com.lucas.organizador_contas.controller;

import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.service.HistoricoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/historico")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    // GET /api/historico/{assinaturaId}: cobranças da conta, da mais recente para a mais antiga.
    @GetMapping("/{assinaturaId}")
    public ResponseEntity<?> listar(Authentication authentication, @PathVariable Long assinaturaId) {
        try {
            Usuario logado = (Usuario) authentication.getPrincipal();
            return ResponseEntity.ok(historicoService.listar(logado.getId(), assinaturaId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}