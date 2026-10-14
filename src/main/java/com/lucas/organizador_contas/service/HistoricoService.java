package com.lucas.organizador_contas.service;

import com.lucas.organizador_contas.dto.PagamentoResponse;
import com.lucas.organizador_contas.model.Assinatura;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.repository.AssinaturaRepository;
import com.lucas.organizador_contas.repository.PagamentoRepository;
import com.lucas.organizador_contas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

// Histórico de cobranças de uma conta (todos os meses já registrados, do mais recente para o mais antigo).
@Service
public class HistoricoService {

    private final PagamentoRepository pagamentoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final UsuarioRepository usuarioRepository;

    public HistoricoService(PagamentoRepository pagamentoRepository,
                            AssinaturaRepository assinaturaRepository,
                            UsuarioRepository usuarioRepository) {
        this.pagamentoRepository = pagamentoRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<PagamentoResponse> listar(Long usuarioId, Long assinaturaId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        Assinatura assinatura = assinaturaRepository.findById(assinaturaId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));

        if (!assinatura.getWorkspace().getId().equals(usuario.getWorkspace().getId())) {
            throw new IllegalArgumentException("Esta conta não pertence ao seu workspace.");
        }

        // Mesma regra de visibilidade das contas: admin vê tudo; membro vê as compartilhadas
        // e as que ele é responsável.
        boolean admin = "admin".equals(usuario.getPapel());
        boolean visivel = "compartilhada".equals(assinatura.getVisibilidade())
                || assinatura.getResponsavel().getId().equals(usuario.getId());
        if (!admin && !visivel) {
            throw new IllegalArgumentException("Você não tem acesso a esta conta.");
        }

        LocalDate hoje = LocalDate.now();

        return pagamentoRepository.findByAssinatura_Workspace_Id(assinatura.getWorkspace().getId()).stream()
                .filter(p -> p.getAssinatura().getId().equals(assinaturaId))
                .sorted(Comparator.comparing(com.lucas.organizador_contas.model.Pagamento::getDataVencimento).reversed())
                .map(p -> new PagamentoResponse(p, hoje))
                .toList();
    }
}