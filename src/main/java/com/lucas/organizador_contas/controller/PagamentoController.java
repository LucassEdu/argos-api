package com.lucas.organizador_contas.controller;

import com.lucas.organizador_contas.dto.PagamentoResponse;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.repository.UsuarioRepository;
import com.lucas.organizador_contas.service.PagamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;
    private final UsuarioRepository usuarioRepository;

    public PagamentoController(PagamentoService pagamentoService, UsuarioRepository usuarioRepository) {
        this.pagamentoService = pagamentoService;
        this.usuarioRepository = usuarioRepository;
    }

    // Descobre quem está logado a partir do token (mesmo padrão do AssinaturaController).
    private Usuario obterUsuarioSeguro(Authentication authentication) {
        String identificador = authentication.getName();
        Object principal = authentication.getPrincipal();
        if (principal instanceof Usuario) {
            identificador = ((Usuario) principal).getEmail();
        }
        String email = identificador.trim().replace("\"", "");
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado na base de dados."));
    }

    // GET /api/pagamentos -> cobranças pendentes + as do mês atual
    @GetMapping
    public ResponseEntity<?> listar(Authentication authentication) {
        try {
            Usuario usuario = obterUsuarioSeguro(authentication);
            LocalDate hoje = LocalDate.now();
            List<PagamentoResponse> lista = pagamentoService.listarDoUsuario(usuario.getId()).stream()
                    .map(p -> new PagamentoResponse(p, hoje))
                    .toList();
            return ResponseEntity.ok(lista);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao listar cobranças: " + e.getMessage());
        }
    }

    // GET /api/pagamentos/resumo-mensal?meses=6 -> total pago/pendente por mês (gráfico de barras)
    @GetMapping("/resumo-mensal")
    public ResponseEntity<?> resumoMensal(Authentication authentication,
                                          @RequestParam(defaultValue = "6") int meses) {
        try {
            Usuario usuario = obterUsuarioSeguro(authentication);
            int janela = Math.max(1, Math.min(meses, 12));
            return ResponseEntity.ok(pagamentoService.resumoMensal(usuario.getId(), janela));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao montar o resumo mensal: " + e.getMessage());
        }
    }

    // PATCH /api/pagamentos/{id}/status?status=pago   (ou status=pendente para desfazer)
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> alterarStatus(Authentication authentication,
                                           @PathVariable Long id,
                                           @RequestParam String status) {
        try {
            Usuario usuario = obterUsuarioSeguro(authentication);
            var pagamento = pagamentoService.alterarStatus(usuario.getId(), id, status);
            return ResponseEntity.ok(new PagamentoResponse(pagamento, LocalDate.now()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}