package com.lucas.organizador_contas.dto;

import java.math.BigDecimal;

/** Total pago e total pendente de um mês (alimenta o gráfico de barras). */
public class ResumoMensalResponse {

    private String mes;          // "2026-10"
    private BigDecimal pago;
    private BigDecimal pendente;

    public ResumoMensalResponse(String mes, BigDecimal pago, BigDecimal pendente) {
        this.mes = mes;
        this.pago = pago;
        this.pendente = pendente;
    }

    public String getMes() {
        return mes;
    }

    public BigDecimal getPago() {
        return pago;
    }

    public BigDecimal getPendente() {
        return pendente;
    }
}