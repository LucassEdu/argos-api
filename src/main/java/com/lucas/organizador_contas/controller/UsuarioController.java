package com.lucas.organizador_contas.controller;

import com.lucas.organizador_contas.dto.AlterarSenhaRequest;
import com.lucas.organizador_contas.dto.MeResponse;
import com.lucas.organizador_contas.dto.PerfilRequest;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.service.PerfilService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final PerfilService perfilService;

    public UsuarioController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    // Devolve os dados de quem está logado (identificado pelo token JWT).
    // O frontend usa isso para saber o papel (admin/membro) e o workspace do usuário.
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        return ResponseEntity.ok(new MeResponse(usuario));
    }

    // Atualiza nome, foto e aviso padrão do próprio usuário.
    @PutMapping("/me")
    public ResponseEntity<?> atualizarPerfil(Authentication authentication, @RequestBody PerfilRequest request) {
        try {
            Usuario logado = (Usuario) authentication.getPrincipal();
            Usuario atualizado = perfilService.atualizarPerfil(logado.getId(), request);
            return ResponseEntity.ok(new MeResponse(atualizado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Troca a senha do próprio usuário (exige a senha atual).
    @PutMapping("/me/senha")
    public ResponseEntity<?> alterarSenha(Authentication authentication, @RequestBody AlterarSenhaRequest request) {
        try {
            Usuario logado = (Usuario) authentication.getPrincipal();
            perfilService.alterarSenha(logado.getId(), request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}