package com.lucas.organizador_contas.service;

import com.lucas.organizador_contas.dto.AssinaturaRequest;
import com.lucas.organizador_contas.dto.AssinaturaResponse;
import com.lucas.organizador_contas.dto.DashboardResponse;
import com.lucas.organizador_contas.model.Assinatura;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.model.Workspace;
import com.lucas.organizador_contas.repository.AssinaturaRepository;
import com.lucas.organizador_contas.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntityManager entityManager;

    public AssinaturaService(AssinaturaRepository assinaturaRepository,
                              UsuarioRepository usuarioRepository,
                              EntityManager entityManager) {
        this.assinaturaRepository = assinaturaRepository;
        this.usuarioRepository = usuarioRepository;
        this.entityManager = entityManager;
    }

    public Assinatura criar(Long adminId, AssinaturaRequest request) {
        Usuario admin = validarAdmin(adminId);

        Workspace workspace = admin.getWorkspace();
        Usuario responsavel = admin;

        validarCamposObrigatorios(request);

        // Se o formulário escolheu outra pessoa para pagar, usa ela (do mesmo workspace).
        if (request.getResponsavelId() != null) {
            responsavel = usuarioRepository.findById(request.getResponsavelId())
                    .orElseThrow(() -> new IllegalArgumentException("Responsável não encontrado."));
            if (!responsavel.getWorkspace().getId().equals(workspace.getId())) {
                throw new IllegalArgumentException("O responsável precisa pertencer ao mesmo workspace.");
            }
        }

        Assinatura assinatura = new Assinatura();
        assinatura.setWorkspace(workspace);
        assinatura.setResponsavel(responsavel);
        assinatura.setNome(request.getNome());
        assinatura.setCategoria(request.getCategoria() != null ? request.getCategoria() : "Geral");
        assinatura.setValor(request.getValor());
        assinatura.setDiaCobranca(request.getDiaCobranca());
        assinatura.setRecorrencia(request.getRecorrencia());
        // Conta mensal não precisa de mês; as demais guardam o mês da primeira cobrança.
        assinatura.setMesInicio("mensal".equals(request.getRecorrencia()) ? null : request.getMesInicio());
        assinatura.setVisibilidade(
                "compartilhada".equals(request.getVisibilidade()) ? "compartilhada" : "privada");
        assinatura.setIcone(textoOuNulo(request.getIcone()));
        assinatura.setLogo(logoValido(request.getLogo()));

        return assinaturaRepository.save(assinatura);
    }

    public Assinatura editar(Long adminId, Long assinaturaId, AssinaturaRequest request) {
        Usuario admin = validarAdmin(adminId);
        Assinatura assinatura = buscarEValidarWorkspace(assinaturaId, admin);

        if (request.getResponsavelId() != null) {
            Usuario responsavel = usuarioRepository.findById(request.getResponsavelId())
                    .orElseThrow(() -> new IllegalArgumentException("Responsável não encontrado."));

            if (!responsavel.getWorkspace().getId().equals(assinatura.getWorkspace().getId())) {
                throw new IllegalArgumentException("O responsável precisa pertencer ao mesmo workspace.");
            }
            assinatura.setResponsavel(responsavel);
        }

        if (request.getNome() != null) assinatura.setNome(request.getNome());
        if (request.getCategoria() != null) assinatura.setCategoria(request.getCategoria());
        if (request.getValor() != null) assinatura.setValor(request.getValor());
        if (request.getDiaCobranca() != null) assinatura.setDiaCobranca(request.getDiaCobranca());
        if (request.getRecorrencia() != null || request.getMesInicio() != null) {
            String recorrencia = request.getRecorrencia() != null
                    ? request.getRecorrencia() : assinatura.getRecorrencia();
            Integer mesInicio = request.getMesInicio() != null
                    ? request.getMesInicio() : assinatura.getMesInicio();
            validarCiclo(recorrencia, mesInicio);

            assinatura.setRecorrencia(recorrencia);
            assinatura.setMesInicio("mensal".equals(recorrencia) ? null : mesInicio);
        }
        if (request.getVisibilidade() != null) assinatura.setVisibilidade(request.getVisibilidade());
        // Texto vazio apaga o logo/ícone; campo ausente (null) mantém o que já existe.
        if (request.getIcone() != null) assinatura.setIcone(textoOuNulo(request.getIcone()));
        if (request.getLogo() != null) assinatura.setLogo(logoValido(request.getLogo()));

        return assinaturaRepository.save(assinatura);
    }

    @Transactional
    public void deletar(Long usuarioId, Long assinaturaId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        
        Assinatura assinatura = assinaturaRepository.findById(assinaturaId)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada."));

        if (!assinatura.getWorkspace().getId().equals(usuario.getWorkspace().getId())) {
            throw new IllegalArgumentException("Esta assinatura não pertence ao seu workspace.");
        }
        
        // Remove dependências de tabelas associadas antes de apagar a assinatura
        entityManager.createQuery("DELETE FROM Notificacao n WHERE n.assinatura.id = :assinaturaId")
                .setParameter("assinaturaId", assinaturaId)
                .executeUpdate();

        entityManager.createQuery("DELETE FROM PreferenciaNotificacao p WHERE p.assinatura.id = :assinaturaId")
                .setParameter("assinaturaId", assinaturaId)
                .executeUpdate();

        assinaturaRepository.delete(assinatura);
    }

    public Assinatura alterarStatus(Long adminId, Long assinaturaId, String novoStatus) {
        Usuario admin = validarAdmin(adminId);
        Assinatura assinatura = buscarEValidarWorkspace(assinaturaId, admin);

        if (!novoStatus.equals("ativa") && !novoStatus.equals("pausada")) {
            throw new IllegalArgumentException("Status deve ser 'ativa' ou 'pausada'.");
        }

        assinatura.setStatus(novoStatus);
        return assinaturaRepository.save(assinatura);
    }

    public List<Assinatura> listarVisiveis(Long workspaceId, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (!usuario.getWorkspace().getId().equals(workspaceId)) {
            throw new IllegalArgumentException("Este usuário não pertence a este workspace.");
        }

        List<Assinatura> todas = assinaturaRepository.findByWorkspace_Id(workspaceId);

        if (usuario.getPapel().equals("admin")) {
            return todas;
        }

        return todas.stream()
                .filter(a -> a.getVisibilidade().equals("compartilhada")
                        || a.getResponsavel().getId().equals(usuario.getId()))
                .toList();
    }

    public DashboardResponse obterDadosDashboard(Long workspaceId, Long usuarioId) {
        List<Assinatura> ativas = listarVisiveis(workspaceId, usuarioId).stream()
                .filter(a -> a.getStatus().equals("ativa"))
                .toList();

        BigDecimal totalMensal = BigDecimal.ZERO;
        Map<String, BigDecimal> gastosPorCategoria = new HashMap<>();

        for (Assinatura a : ativas) {
            BigDecimal valor = a.getValor();
            
            // Conta que cobra de vez em quando entra dividida pelos meses do ciclo
            // (anual /12, semestral /6, trimestral /3, bimestral /2).
            if (a.intervaloMeses() > 1) {
                valor = valor.divide(new BigDecimal(a.intervaloMeses()), 2, RoundingMode.HALF_UP);
            }

            totalMensal = totalMensal.add(valor);

            String categoria = a.getCategoria() != null ? a.getCategoria() : "Outros";
            gastosPorCategoria.put(categoria, gastosPorCategoria.getOrDefault(categoria, BigDecimal.ZERO).add(valor));
        }

        int diaHoje = LocalDate.now().getDayOfMonth();

        List<AssinaturaResponse> proximosVencimentos = ativas.stream()
                .sorted((a1, a2) -> {
                    int diasA1 = a1.getDiaCobranca() >= diaHoje ? a1.getDiaCobranca() - diaHoje : (a1.getDiaCobranca() + 30) - diaHoje;
                    int diasA2 = a2.getDiaCobranca() >= diaHoje ? a2.getDiaCobranca() - diaHoje : (a2.getDiaCobranca() + 30) - diaHoje;
                    return Integer.compare(diasA1, diasA2);
                })
                .map(AssinaturaResponse::new)
                .toList();

        return new DashboardResponse(totalMensal, gastosPorCategoria, proximosVencimentos);
    }

    private Usuario validarAdmin(Long adminId) {
        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário admin não encontrado."));

        if (!admin.getPapel().equals("admin")) {
            throw new IllegalArgumentException("Apenas o admin do workspace pode gerenciar assinaturas.");
        }
        return admin;
    }

    private Assinatura buscarEValidarWorkspace(Long assinaturaId, Usuario admin) {
        Assinatura assinatura = assinaturaRepository.findById(assinaturaId)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada."));

        if (!assinatura.getWorkspace().getId().equals(admin.getWorkspace().getId())) {
            throw new IllegalArgumentException("Esta assinatura não pertence ao seu workspace.");
        }
        return assinatura;
    }

    private void validarCamposObrigatorios(AssinaturaRequest request) {
        if (request.getNome() == null || request.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome da assinatura é obrigatório.");
        }
        if (request.getValor() == null) {
            throw new IllegalArgumentException("Valor é obrigatório.");
        }
        if (request.getDiaCobranca() == null || request.getDiaCobranca() < 1 || request.getDiaCobranca() > 31) {
            throw new IllegalArgumentException("Dia de cobrança deve estar entre 1 e 31.");
        }
        validarCiclo(request.getRecorrencia(), request.getMesInicio());
    }

    private static final int LIMITE_LOGO = 150_000; // caracteres (imagem pequena em texto)

    private String textoOuNulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    // Aceita só imagem em data URL e de tamanho razoável; vazio vira "sem logo".
    private String logoValido(String logo) {
        if (logo == null || logo.isBlank()) {
            return null;
        }
        if (!logo.startsWith("data:image/")) {
            throw new IllegalArgumentException("O logo precisa ser uma imagem.");
        }
        if (logo.length() > LIMITE_LOGO) {
            throw new IllegalArgumentException("A imagem do logo é grande demais. Use uma menor.");
        }
        return logo;
    }

    private static final List<String> RECORRENCIAS =
            List.of("mensal", "bimestral", "trimestral", "semestral", "anual");

    // Confere o tipo de cobrança e, se não for mensal, o mês da primeira cobrança.
    private void validarCiclo(String recorrencia, Integer mesInicio) {
        if (recorrencia == null || !RECORRENCIAS.contains(recorrencia)) {
            throw new IllegalArgumentException(
                    "Recorrência deve ser mensal, bimestral, trimestral, semestral ou anual.");
        }
        if (!recorrencia.equals("mensal") && (mesInicio == null || mesInicio < 1 || mesInicio > 12)) {
            throw new IllegalArgumentException("Informe o mês da primeira cobrança (1 a 12).");
        }
    }
}