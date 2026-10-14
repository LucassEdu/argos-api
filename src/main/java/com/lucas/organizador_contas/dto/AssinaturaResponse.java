package com.lucas.organizador_contas.dto;

import com.lucas.organizador_contas.model.Assinatura;

import java.math.BigDecimal;

public class AssinaturaResponse {

    private Long id;
    private String nome;
    private String categoria;
    private BigDecimal valor;
    private Integer diaCobranca;
    private String recorrencia;
    private Integer mesInicio;
    private String icone;
    private String logo;
    private String visibilidade;
    private String status;
    private Long responsavelId;
    private String responsavelNome;
    private Long workspaceId;

    public AssinaturaResponse(Assinatura assinatura) {
        this.id = assinatura.getId();
        this.nome = assinatura.getNome();
        this.categoria = assinatura.getCategoria();
        this.valor = assinatura.getValor();
        this.diaCobranca = assinatura.getDiaCobranca();
        this.recorrencia = assinatura.getRecorrencia();
        this.mesInicio = assinatura.getMesInicio();
        this.icone = assinatura.getIcone();
        this.logo = assinatura.getLogo();
        this.visibilidade = assinatura.getVisibilidade();
        this.status = assinatura.getStatus();
        this.responsavelId = assinatura.getResponsavel().getId();
        this.responsavelNome = assinatura.getResponsavel().getNome();
        this.workspaceId = assinatura.getWorkspace().getId();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCategoria() {
        return categoria;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Integer getDiaCobranca() {
        return diaCobranca;
    }

    public String getRecorrencia() {
        return recorrencia;
    }

    public Integer getMesInicio() {
        return mesInicio;
    }

    public String getIcone() {
        return icone;
    }

    public String getLogo() {
        return logo;
    }

    public String getVisibilidade() {
        return visibilidade;
    }

    public String getStatus() {
        return status;
    }

    public Long getResponsavelId() {
        return responsavelId;
    }

    public String getResponsavelNome() {
        return responsavelNome;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }
}