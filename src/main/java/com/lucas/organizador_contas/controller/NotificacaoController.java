package com.lucas.organizador_contas.controller;

import com.lucas.organizador_contas.dto.NotificacaoResponse;
import com.lucas.organizador_contas.dto.PreferenciaNotificacaoRequest;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.service.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @PostMapping("/preferencia")
    public ResponseEntity<?> definirPreferencia(Authentication authentication, @RequestBody PreferenciaNotificacaoRequest request) {
        try {
            Usuario usuario = (Usuario) authentication.getPrincipal();
            notificacaoService.definirPreferencia(usuario.getId(), request);
            return ResponseEntity.ok("Preferência de notificação salva com sucesso.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> listar(Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        List<NotificacaoResponse> notificacoes = notificacaoService.listarNotificacoes(usuario.getId())
                .stream()
                .map(NotificacaoResponse::new)
                .toList();
        return ResponseEntity.ok(notificacoes);
    }

    @PatchMapping("/{id}/lida")
    public ResponseEntity<?> marcarComoLida(Authentication authentication, @PathVariable Long id) {
        try {
            Usuario usuario = (Usuario) authentication.getPrincipal();
            notificacaoService.marcarComoLida(usuario.getId(), id);
            return ResponseEntity.ok("Notificação marcada como lida.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}