package com.lucas.organizador_contas.controller;

import com.lucas.organizador_contas.dto.AssinaturaRequest;
import com.lucas.organizador_contas.dto.AssinaturaResponse;
import com.lucas.organizador_contas.model.Assinatura;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.repository.UsuarioRepository;
import com.lucas.organizador_contas.service.AssinaturaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assinaturas")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;
    private final UsuarioRepository usuarioRepository;

    public AssinaturaController(AssinaturaService assinaturaService, UsuarioRepository usuarioRepository) {
        this.assinaturaService = assinaturaService;
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

    @GetMapping("/usuario")
    public ResponseEntity<?> obterUsuarioLogado(Authentication authentication) {
        try {
            Usuario usuario = obterUsuarioSeguro(authentication);
            return ResponseEntity.ok(Map.of("nome", usuario.getNome()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> criar(Authentication authentication, @RequestBody AssinaturaRequest request) {
        try {
            Usuario admin = obterUsuarioSeguro(authentication);
            Assinatura assinatura = assinaturaService.criar(admin.getId(), request);
            return ResponseEntity.ok(new AssinaturaResponse(assinatura));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro no servidor: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(Authentication authentication, @PathVariable Long id, @RequestBody AssinaturaRequest request) {
        try {
            Usuario admin = obterUsuarioSeguro(authentication);
            Assinatura assinatura = assinaturaService.editar(admin.getId(), id, request);
            return ResponseEntity.ok(new AssinaturaResponse(assinatura));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(Authentication authentication, @PathVariable Long id) {
        try {
            Usuario admin = obterUsuarioSeguro(authentication);
            assinaturaService.deletar(admin.getId(), id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> alterarStatus(Authentication authentication, @PathVariable Long id, @RequestParam String status) {
        try {
            Usuario admin = (Usuario) authentication.getPrincipal();
            Assinatura assinatura = assinaturaService.alterarStatus(admin.getId(), id, status);
            return ResponseEntity.ok(new AssinaturaResponse(assinatura));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> listarTodas(Authentication authentication) {
        try {
            Usuario usuario = obterUsuarioSeguro(authentication);
            Long workspaceId = usuario.getWorkspace().getId();
            Long usuarioId = usuario.getId();
            
            return ResponseEntity.ok(
        assinaturaService.listarVisiveis(workspaceId, usuarioId)
                .stream()
                .map(AssinaturaResponse::new)
                .toList()
); 
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao listar contas: " + e.getMessage());
        }
    }
}