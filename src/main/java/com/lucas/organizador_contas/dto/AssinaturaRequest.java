package com.lucas.organizador_contas.dto;

import java.math.BigDecimal;

public class AssinaturaRequest {

    private Long workspaceId;
    private Long responsavelId;
    private String nome;
    private String categoria;
    private BigDecimal valor;
    private Integer diaCobranca;
    // "mensal", "bimestral", "trimestral", "semestral" ou "anual"
    private String recorrencia;
    // Mês (1 a 12) da primeira cobrança. Obrigatório quando a conta não é mensal.
    private Integer mesInicio;
    private String icone; // id do catálogo (ex.: "spotify")
    private String logo; // imagem enviada pelo cliente (data URL)
    private String visibilidade; // "privada" ou "compartilhada" (opcional, padrão "privada")

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public Long getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(Long responsavelId) {
        this.responsavelId = responsavelId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public Integer getDiaCobranca() {
        return diaCobranca;
    }

    public void setDiaCobranca(Integer diaCobranca) {
        this.diaCobranca = diaCobranca;
    }

    public String getRecorrencia() {
        return recorrencia;
    }

    public void setRecorrencia(String recorrencia) {
        this.recorrencia = recorrencia;
    }

    public Integer getMesInicio() {
        return mesInicio;
    }

    public void setMesInicio(Integer mesInicio) {
        this.mesInicio = mesInicio;
    }

    public String getIcone() {
        return icone;
    }

    public void setIcone(String icone) {
        this.icone = icone;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getVisibilidade() {
        return visibilidade;
    }

    public void setVisibilidade(String visibilidade) {
        this.visibilidade = visibilidade;
    }
}