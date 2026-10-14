package com.lucas.organizador_contas.service;

import com.lucas.organizador_contas.dto.ResumoMensalResponse;
import com.lucas.organizador_contas.model.Assinatura;
import com.lucas.organizador_contas.model.Pagamento;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.repository.AssinaturaRepository;
import com.lucas.organizador_contas.repository.PagamentoRepository;
import com.lucas.organizador_contas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final UsuarioRepository usuarioRepository;

    public PagamentoService(PagamentoRepository pagamentoRepository,
                            AssinaturaRepository assinaturaRepository,
                            UsuarioRepository usuarioRepository) {
        this.pagamentoRepository = pagamentoRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Lista as cobranças que o usuário pode ver: todas as pendentes + as do mês atual.
     * Antes de listar, garante que cada assinatura ativa tem a sua próxima cobrança
     * (mensal: a do mês; bimestral/trimestral/semestral/anual: a do próximo ciclo,
     * criada com até 60 dias de antecedência).
     */
    @Transactional
    public List<Pagamento> listarDoUsuario(Long usuarioId) {
        Usuario usuario = buscarUsuario(usuarioId);
        Long workspaceId = usuario.getWorkspace().getId();
        LocalDate hoje = LocalDate.now();

        // Todas as assinaturas ativas entram, qualquer que seja o ciclo.
        List<Assinatura> mensaisAtivas = assinaturaRepository.findByWorkspace_Id(workspaceId).stream()
                .filter(a -> "ativa".equals(a.getStatus()))
                .toList();

        for (Assinatura assinatura : mensaisAtivas) {
            garantirCobrancaDoMes(assinatura, hoje);
        }

        // Mesma regra de visibilidade das assinaturas: admin vê tudo; membro vê as
        // compartilhadas e as que ele é responsável.
        Set<Long> visiveis = mensaisAtivas.stream()
                .filter(a -> podeVer(usuario, a))
                .map(Assinatura::getId)
                .collect(Collectors.toSet());

        YearMonth mesAtual = YearMonth.from(hoje);

        List<Pagamento> resultado = pagamentoRepository.findByAssinatura_Workspace_Id(workspaceId).stream()
                .filter(p -> visiveis.contains(p.getAssinatura().getId()))
                .filter(p -> "pendente".equals(p.getStatus())
                        || YearMonth.from(p.getDataVencimento()).equals(mesAtual))
                .sorted(Comparator.comparing(Pagamento::getDataVencimento))
                .toList();

        // Se o valor da assinatura mudou, as cobranças ainda pendentes acompanham.
        for (Pagamento p : resultado) {
            if ("pendente".equals(p.getStatus())
                    && p.getValor().compareTo(p.getAssinatura().getValor()) != 0) {
                p.setValor(p.getAssinatura().getValor());
                pagamentoRepository.save(p);
            }
        }

        return resultado;
    }

    /**
     * Total pago e total pendente dos últimos meses (inclui o atual), só das
     * assinaturas que o usuário pode ver. Meses sem cobrança vêm zerados.
     */
    @Transactional(readOnly = true)
    public List<ResumoMensalResponse> resumoMensal(Long usuarioId, int meses) {
        Usuario usuario = buscarUsuario(usuarioId);
        Long workspaceId = usuario.getWorkspace().getId();

        Set<Long> visiveis = assinaturaRepository.findByWorkspace_Id(workspaceId).stream()
                .filter(a -> podeVer(usuario, a))
                .map(Assinatura::getId)
                .collect(Collectors.toSet());

        // Um balde por mês: [0] = pago, [1] = pendente.
        YearMonth atual = YearMonth.now();
        Map<YearMonth, BigDecimal[]> baldes = new LinkedHashMap<>();
        for (int i = meses - 1; i >= 0; i--) {
            baldes.put(atual.minusMonths(i), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (Pagamento p : pagamentoRepository.findByAssinatura_Workspace_Id(workspaceId)) {
            if (!visiveis.contains(p.getAssinatura().getId())) {
                continue;
            }
            BigDecimal[] balde = baldes.get(YearMonth.from(p.getDataVencimento()));
            if (balde == null) {
                continue; // fora da janela de meses pedida
            }
            int posicao = "pago".equals(p.getStatus()) ? 0 : 1;
            balde[posicao] = balde[posicao].add(p.getValor());
        }

        return baldes.entrySet().stream()
                .map(e -> new ResumoMensalResponse(e.getKey().toString(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }

    /** Marca como "pago" ou volta para "pendente". Só admin ou o responsável pela conta. */
    @Transactional
    public Pagamento alterarStatus(Long usuarioId, Long pagamentoId, String novoStatus) {
        if (!"pago".equals(novoStatus) && !"pendente".equals(novoStatus)) {
            throw new IllegalArgumentException("Status deve ser 'pago' ou 'pendente'.");
        }

        Usuario usuario = buscarUsuario(usuarioId);
        Pagamento pagamento = pagamentoRepository.findById(pagamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada."));
        Assinatura assinatura = pagamento.getAssinatura();

        if (!assinatura.getWorkspace().getId().equals(usuario.getWorkspace().getId())) {
            throw new IllegalArgumentException("Esta cobrança não pertence ao seu workspace.");
        }

        boolean admin = "admin".equals(usuario.getPapel());
        boolean responsavel = assinatura.getResponsavel().getId().equals(usuario.getId());
        if (!admin && !responsavel) {
            throw new IllegalArgumentException("Apenas o admin ou o responsável pela conta pode alterar o pagamento.");
        }

        pagamento.setStatus(novoStatus);
        pagamento.setDataPagamento("pago".equals(novoStatus) ? LocalDate.now() : null);
        return pagamentoRepository.save(pagamento);
    }

    // --- Métodos auxiliares privados ---

    // Cobranças de contas que não são mensais nascem com até 60 dias de antecedência.
    private static final int DIAS_DE_ANTECEDENCIA_CICLO = 60;

    /**
     * Cria a próxima cobrança da assinatura, se ainda não existir.
     * - Já existe cobrança deste mês em diante: nada a fazer.
     * - Senão, procura o primeiro mês (a partir de agora) em que a conta cobra.
     * - Conta recém-criada cujo dia já passou: a primeira cobrança é a do próximo ciclo
     *   (senão a conta nasceria "vencida" sem ter dado tempo de pagar).
     * - Conta que não é mensal só gera a cobrança quando faltam até 60 dias.
     */
    private void garantirCobrancaDoMes(Assinatura assinatura, LocalDate hoje) {
        int dia = assinatura.getDiaCobranca();
        LocalDate primeiroDiaDoMes = hoje.withDayOfMonth(1);

        if (pagamentoRepository.existsByAssinatura_IdAndDataVencimentoGreaterThanEqual(
                assinatura.getId(), primeiroDiaDoMes)) {
            return;
        }

        boolean primeiraCobranca = !pagamentoRepository.existsByAssinatura_Id(assinatura.getId());
        LocalDate vencimento = proximoVencimentoDoCiclo(assinatura, dia, hoje, 0);

        if (primeiraCobranca && vencimento.isBefore(hoje)) {
            vencimento = proximoVencimentoDoCiclo(assinatura, dia, hoje, 1);
        }

        boolean mensal = assinatura.intervaloMeses() == 1;
        if (!mensal && vencimento.isAfter(hoje.plusDays(DIAS_DE_ANTECEDENCIA_CICLO))) {
            return; // ainda falta muito: a cobrança será criada mais perto da data
        }

        Pagamento novo = new Pagamento();
        novo.setAssinatura(assinatura);
        novo.setDataVencimento(vencimento);
        novo.setValor(assinatura.getValor());
        novo.setStatus("pendente");
        pagamentoRepository.save(novo);
    }

    // Primeira data de cobrança a partir do mês atual + mesesAFrente, respeitando o ciclo.
    private LocalDate proximoVencimentoDoCiclo(Assinatura assinatura, int dia, LocalDate hoje, int mesesAFrente) {
        for (int i = mesesAFrente; i < mesesAFrente + 13; i++) {
            LocalDate mes = hoje.plusMonths(i);
            if (assinatura.cobraNoMes(mes.getMonthValue())) {
                return dataValida(mes.getYear(), mes.getMonthValue(), dia);
            }
        }
        return dataValida(hoje.getYear(), hoje.getMonthValue(), dia); // não deve acontecer
    }

    private boolean podeVer(Usuario usuario, Assinatura assinatura) {
        if ("admin".equals(usuario.getPapel())) {
            return true;
        }
        return "compartilhada".equals(assinatura.getVisibilidade())
                || assinatura.getResponsavel().getId().equals(usuario.getId());
    }

    // Dia 31 em fevereiro vira o último dia do mês.
    private LocalDate dataValida(int ano, int mes, int diaDesejado) {
        int ultimoDia = YearMonth.of(ano, mes).lengthOfMonth();
        return LocalDate.of(ano, mes, Math.min(diaDesejado, ultimoDia));
    }

    private Usuario buscarUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    }
}