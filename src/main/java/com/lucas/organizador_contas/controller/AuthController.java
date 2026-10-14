package com.lucas.organizador_contas.controller;

import com.lucas.organizador_contas.dto.CadastroRequest;
import com.lucas.organizador_contas.dto.LoginRequest;
import com.lucas.organizador_contas.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<String> cadastro(@RequestBody CadastroRequest request) {
        try {
            usuarioService.cadastrar(request);
            return ResponseEntity.ok("Cadastro realizado! Verifique seu e-mail para ativar a conta.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/verificar")
    public ResponseEntity<String> verificar(@RequestParam String token) {
        try {
            usuarioService.verificarEmail(token);
            return ResponseEntity.ok("E-mail verificado com sucesso! Você já pode fazer login.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reenviar-verificacao")
    public ResponseEntity<String> reenviarVerificacao(@RequestBody Map<String, String> corpo) {
        try {
            usuarioService.reenviarVerificacao(corpo.get("email"));
            return ResponseEntity.ok("Se existir uma conta aguardando confirmação, enviamos um novo link.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<String> esqueciSenha(@RequestBody Map<String, String> corpo) {
        try {
            usuarioService.solicitarRedefinicaoSenha(corpo.get("email"));
            return ResponseEntity.ok("Se existir uma conta com esse e-mail, enviamos um link para redefinir a senha.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<String> redefinirSenha(@RequestBody Map<String, String> corpo) {
        try {
            usuarioService.redefinirSenha(corpo.get("token"), corpo.get("novaSenha"));
            return ResponseEntity.ok("Senha alterada com sucesso. Você já pode entrar.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = usuarioService.login(request);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}