package com.lucas.organizador_contas.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardResponse {

    private BigDecimal totalMensal;
    private Map<String, BigDecimal> gastosPorCategoria;
    private List<AssinaturaResponse> proximosVencimentos;

    public DashboardResponse(BigDecimal totalMensal, Map<String, BigDecimal> gastosPorCategoria, List<AssinaturaResponse> proximosVencimentos) {
        this.totalMensal = totalMensal;
        this.gastosPorCategoria = gastosPorCategoria;
        this.proximosVencimentos = proximosVencimentos;
    }

    public BigDecimal getTotalMensal() {
        return totalMensal;
    }

    public Map<String, BigDecimal> getGastosPorCategoria() {
        return gastosPorCategoria;
    }

    public List<AssinaturaResponse> getProximosVencimentos() {
        return proximosVencimentos;
    }
}