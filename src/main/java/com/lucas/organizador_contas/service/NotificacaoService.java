package com.lucas.organizador_contas.service;

import com.lucas.organizador_contas.dto.PreferenciaNotificacaoRequest;
import com.lucas.organizador_contas.model.*;
import com.lucas.organizador_contas.repository.AssinaturaRepository;
import com.lucas.organizador_contas.repository.NotificacaoRepository;
import com.lucas.organizador_contas.repository.PagamentoRepository;
import com.lucas.organizador_contas.repository.PreferenciaNotificacaoRepository;
import com.lucas.organizador_contas.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    private final PreferenciaNotificacaoRepository preferenciaRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final EmailService emailService;

    public NotificacaoService(PreferenciaNotificacaoRepository preferenciaRepository,
                               NotificacaoRepository notificacaoRepository,
                               UsuarioRepository usuarioRepository,
                               AssinaturaRepository assinaturaRepository,
                               PagamentoRepository pagamentoRepository,
                               EmailService emailService) {
        this.preferenciaRepository = preferenciaRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.emailService = emailService;
    }

    public PreferenciaNotificacao definirPreferencia(Long usuarioId, PreferenciaNotificacaoRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        Assinatura assinatura = assinaturaRepository.findById(request.getAssinaturaId())
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada."));

        if (!assinatura.getWorkspace().getId().equals(usuario.getWorkspace().getId())) {
            throw new IllegalArgumentException("Esta assinatura não pertence ao seu workspace.");
        }

        if (request.getDiasAntecedencia() == null || request.getDiasAntecedencia() < 0) {
            throw new IllegalArgumentException("Dias de antecedência deve ser um número positivo.");
        }

        PreferenciaNotificacao preferencia = preferenciaRepository
                .findByUsuario_IdAndAssinatura_Id(usuarioId, request.getAssinaturaId())
                .orElse(new PreferenciaNotificacao());

        preferencia.setUsuario(usuario);
        preferencia.setAssinatura(assinatura);
        preferencia.setDiasAntecedencia(request.getDiasAntecedencia());
        boolean ativa = request.getAtiva() != null ? request.getAtiva() : true;
        preferencia.setAtiva(ativa);

        PreferenciaNotificacao salva = preferenciaRepository.save(preferencia);

        // Se a conta já está dentro da janela de aviso (ex.: vence em 3 dias e o aviso é de
        // 3 dias), avisa agora, sem esperar o job das 08:00 de amanhã.
        if (ativa) {
            try {
                processarPreferencia(salva, LocalDate.now());
            } catch (Exception e) {
                // Falha no aviso nunca pode impedir o cadastro da conta.
                log.warn("Não foi possível avisar na hora sobre a assinatura {}: {}",
                        assinatura.getId(), e.getMessage());
            }
        }

        return salva;
    }

    public List<Notificacao> listarNotificacoes(Long usuarioId) {
        return notificacaoRepository.findByUsuario_IdOrderByCriadoEmDesc(usuarioId);
    }

    public void marcarComoLida(Long usuarioId, Long notificacaoId) {
        Notificacao notificacao = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Notificação não encontrada."));
        // Só o dono da notificação pode mexer nela. A mensagem é a mesma de "não existe"
        // para não revelar que o id pertence a outra pessoa.
        if (!notificacao.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("Notificação não encontrada.");
        }
        notificacao.setLida(true);
        notificacaoRepository.save(notificacao);
    }

    /**
     * Roda todo dia às 08:00. Para cada preferência ativa, calcula quantos dias
     * faltam até o vencimento da assinatura e, se estiver dentro da janela
     * configurada pelo usuário, dispara e-mail + notificação no sino.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void verificarVencimentos() {
        log.info("Iniciando verificação diária de vencimentos...");

        LocalDate hoje = LocalDate.now();
        List<PreferenciaNotificacao> preferenciasAtivas = preferenciaRepository.findByAtivaTrue();

        for (PreferenciaNotificacao preferencia : preferenciasAtivas) {
            try {
                processarPreferencia(preferencia, hoje);
            } catch (Exception e) {
                // Um erro em uma conta não pode travar o aviso das outras.
                log.warn("Falha ao processar o aviso da assinatura {}: {}", preferencia.getAssinatura().getId(), e.getMessage());
            }
        }

        log.info("Verificação diária de vencimentos concluída.");
    }

    /**
     * Regra de aviso de UMA preferência: conta ativa, dentro da janela de dias,
     * ainda não paga e ainda não avisada hoje. Usada pelo job diário e também
     * logo depois de cadastrar/editar a preferência.
     */
    private void processarPreferencia(PreferenciaNotificacao preferencia, LocalDate hoje) {
        Assinatura assinatura = preferencia.getAssinatura();

        if (!"ativa".equals(assinatura.getStatus())) {
            return; // não notifica assinatura pausada
        }

        long diasRestantes = calcularDiasAteVencimento(assinatura, hoje);

        if (diasRestantes < 0 || diasRestantes > preferencia.getDiasAntecedencia()) {
            return;
        }

        // Se a cobrança deste vencimento já foi marcada como paga, não avisa.
        LocalDate dataVencimento = hoje.plusDays(diasRestantes);
        boolean jaPaga = pagamentoRepository.existsByAssinatura_IdAndDataVencimentoAndStatus(
                assinatura.getId(), dataVencimento, "pago");
        if (jaPaga) {
            return;
        }

        notificarSeAindaNaoNotificado(preferencia.getUsuario(), assinatura, diasRestantes, hoje);
    }

    private void notificarSeAindaNaoNotificado(Usuario usuario, Assinatura assinatura, long diasRestantes, LocalDate hoje) {
        boolean jaNotificadoHoje = notificacaoRepository
                .existsByUsuario_IdAndAssinatura_IdAndDataReferencia(usuario.getId(), assinatura.getId(), hoje);

        if (jaNotificadoHoje) {
            return;
        }

        String mensagem = montarMensagem(assinatura.getNome(), diasRestantes);

        Notificacao notificacao = new Notificacao();
        notificacao.setUsuario(usuario);
        notificacao.setAssinatura(assinatura);
        notificacao.setMensagem(mensagem);
        notificacao.setDataReferencia(hoje);
        notificacaoRepository.save(notificacao);

        try {
            emailService.enviarEmailVencimento(usuario.getEmail(), assinatura.getNome(), diasRestantes);
        } catch (Exception e) {
            log.warn("Notificação salva, mas o e-mail para {} falhou: {}", usuario.getEmail(), e.getMessage());
        }
    }

    private String montarMensagem(String nomeAssinatura, long diasRestantes) {
        if (diasRestantes == 0) {
            return nomeAssinatura + " vence hoje.";
        } else if (diasRestantes == 1) {
            return nomeAssinatura + " vence amanhã.";
        } else {
            return nomeAssinatura + " vence em " + diasRestantes + " dias.";
        }
    }

    /**
     * Calcula quantos dias faltam até a próxima cobrança, considerando que
     * "diaCobranca" é só o dia do mês (ex: 15) — não uma data completa.
     * Se o dia já passou nesse mês, considera o mesmo dia no mês seguinte.
     * Meses mais curtos que o diaCobranca (ex: dia 31 em fevereiro) usam o último dia do mês.
     */
    // Dias até a próxima cobrança, respeitando o ciclo (mensal, trimestral, anual...).
    private long calcularDiasAteVencimento(Assinatura assinatura, LocalDate hoje) {
        int diaCobranca = assinatura.getDiaCobranca();

        for (int i = 0; i < 13; i++) {
            LocalDate mes = hoje.plusMonths(i);
            if (!assinatura.cobraNoMes(mes.getMonthValue())) {
                continue;
            }
            LocalDate vencimento = construirDataValida(mes.getYear(), mes.getMonthValue(), diaCobranca);
            if (!vencimento.isBefore(hoje)) {
                return ChronoUnit.DAYS.between(hoje, vencimento);
            }
        }
        return Long.MAX_VALUE; // sem próxima cobrança: não avisa
    }

    private LocalDate construirDataValida(int ano, int mes, int diaDesejado) {
        int ultimoDiaDoMes = YearMonth.of(ano, mes).lengthOfMonth();
        int dia = Math.min(diaDesejado, ultimoDiaDoMes);
        return LocalDate.of(ano, mes, dia);
    }
}