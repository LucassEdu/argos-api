package com.lucas.organizador_contas.controller;

import com.lucas.organizador_contas.dto.ServicoRequest;
import com.lucas.organizador_contas.dto.ServicoResponse;
import com.lucas.organizador_contas.model.Servico;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.repository.UsuarioRepository;
import com.lucas.organizador_contas.service.ServicoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Catálogo de serviços do workspace: nome, categoria e logo reaproveitáveis.
@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final ServicoService servicoService;
    private final UsuarioRepository usuarioRepository;

    public ServicoController(ServicoService servicoService, UsuarioRepository usuarioRepository) {
        this.servicoService = servicoService;
        this.usuarioRepository = usuarioRepository;
    }

    private Usuario obterUsuarioSeguro(Authentication authentication) {
        String identificador = authentication.getName();
        Object principal = authentication.getPrincipal();
        if (principal instanceof Usuario) {
            identificador = ((Usuario) principal).getEmail();
        }
        identificador = identificador.trim().replace("\"", "");

        String emailFinal = identificador;
        return usuarioRepository.findByEmail(emailFinal)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado na base de dados."));
    }

    @GetMapping
    public ResponseEntity<?> listar(Authentication authentication) {
        try {
            Usuario usuario = obterUsuarioSeguro(authentication);
            return ResponseEntity.ok(
                    servicoService.listar(usuario.getId()).stream().map(ServicoResponse::new).toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> criar(Authentication authentication, @RequestBody ServicoRequest request) {
        try {
            Usuario admin = obterUsuarioSeguro(authentication);
            Servico servico = servicoService.criar(admin.getId(), request);
            return ResponseEntity.ok(new ServicoResponse(servico));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(Authentication authentication, @PathVariable Long id,
                                    @RequestBody ServicoRequest request) {
        try {
            Usuario admin = obterUsuarioSeguro(authentication);
            Servico servico = servicoService.editar(admin.getId(), id, request);
            return ResponseEntity.ok(new ServicoResponse(servico));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(Authentication authentication, @PathVariable Long id) {
        try {
            Usuario admin = obterUsuarioSeguro(authentication);
            servicoService.deletar(admin.getId(), id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}