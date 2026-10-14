package com.lucas.organizador_contas.dto;

import com.lucas.organizador_contas.model.Pagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PagamentoResponse {

    // Quantos dias antes do vencimento a cobrança entra em "Vencem em breve" (amarelo).
    private static final int DIAS_PARA_FICAR_AMARELA = 7;

    private Long id;
    private Long assinaturaId;
    private String assinaturaNome;
    private String categoria;
    private String responsavelNome;
    private BigDecimal valor;
    private String dataVencimento; // "2026-10-12"
    private String status;         // "pendente" ou "pago"
    private String dataPagamento;  // null enquanto não pago
    private long diasParaVencer;   // negativo = já venceu
    private String situacao;       // "paga", "vencida", "proxima" (até 7 dias) ou "em_dia"

    public PagamentoResponse(Pagamento p, LocalDate hoje) {
        this.id = p.getId();
        this.assinaturaId = p.getAssinatura().getId();
        this.assinaturaNome = p.getAssinatura().getNome();
        this.categoria = p.getAssinatura().getCategoria();
        this.responsavelNome = p.getAssinatura().getResponsavel().getNome();
        this.valor = p.getValor();
        this.dataVencimento = p.getDataVencimento().toString();
        this.status = p.getStatus();
        this.dataPagamento = p.getDataPagamento() != null ? p.getDataPagamento().toString() : null;
        this.diasParaVencer = ChronoUnit.DAYS.between(hoje, p.getDataVencimento());

        if ("pago".equals(p.getStatus())) {
            this.situacao = "paga";
        } else if (this.diasParaVencer < 0) {
            this.situacao = "vencida";
        } else if (this.diasParaVencer <= DIAS_PARA_FICAR_AMARELA) {
            this.situacao = "proxima";
        } else {
            this.situacao = "em_dia";
        }
    }

    public Long getId() {
        return id;
    }

    public Long getAssinaturaId() {
        return assinaturaId;
    }

    public String getAssinaturaNome() {
        return assinaturaNome;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getResponsavelNome() {
        return responsavelNome;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getDataVencimento() {
        return dataVencimento;
    }

    public String getStatus() {
        return status;
    }

    public String getDataPagamento() {
        return dataPagamento;
    }

    public long getDiasParaVencer() {
        return diasParaVencer;
    }

    public String getSituacao() {
        return situacao;
    }
}