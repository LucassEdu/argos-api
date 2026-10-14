package com.lucas.organizador_contas.controller;

import com.lucas.organizador_contas.dto.ConvidarMembroRequest;
import com.lucas.organizador_contas.dto.DefinirSenhaRequest;
import com.lucas.organizador_contas.dto.MembroResponse;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.service.WorkspaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping("/convidar")
    public ResponseEntity<?> convidar(Authentication authentication, @RequestBody ConvidarMembroRequest request) {
        try {
            Usuario admin = (Usuario) authentication.getPrincipal();
            Usuario membro = workspaceService.convidarMembro(admin.getId(), request);
            return ResponseEntity.ok("Convite enviado para " + membro.getEmail() + "! ID do membro: " + membro.getId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/definir-senha")
    public ResponseEntity<?> definirSenha(@RequestBody DefinirSenhaRequest request) {
        try {
            workspaceService.definirSenha(request);
            return ResponseEntity.ok("Senha definida com sucesso! Sua conta está ativa.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/membro/{membroId}")
    public ResponseEntity<?> removerMembro(Authentication authentication, @PathVariable Long membroId) {
        try {
            Usuario admin = (Usuario) authentication.getPrincipal();
            workspaceService.removerMembro(admin.getId(), membroId);
            return ResponseEntity.ok("Membro removido com sucesso.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{workspaceId}/membros")
    public ResponseEntity<?> listarMembros(@PathVariable Long workspaceId) {
        try {
            List<MembroResponse> membros = workspaceService.listarMembros(workspaceId)
                    .stream()
                    .map(MembroResponse::new)
                    .toList();
            return ResponseEntity.ok(membros);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}