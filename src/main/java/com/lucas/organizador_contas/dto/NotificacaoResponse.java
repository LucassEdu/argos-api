package com.lucas.organizador_contas.dto;

import com.lucas.organizador_contas.model.Notificacao;

import java.time.LocalDateTime;

public class NotificacaoResponse {

    private Long id;
    private String mensagem;
    private boolean lida;
    private Long assinaturaId;
    private String assinaturaNome;
    private LocalDateTime criadoEm;

    public NotificacaoResponse(Notificacao notificacao) {
        this.id = notificacao.getId();
        this.mensagem = notificacao.getMensagem();
        this.lida = notificacao.isLida();
        this.assinaturaId = notificacao.getAssinatura().getId();
        this.assinaturaNome = notificacao.getAssinatura().getNome();
        this.criadoEm = notificacao.getCriadoEm();
    }

    public Long getId() {
        return id;
    }

    public String getMensagem() {
        return mensagem;
    }

    public boolean isLida() {
        return lida;
    }

    public Long getAssinaturaId() {
        return assinaturaId;
    }

    public String getAssinaturaNome() {
        return assinaturaNome;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}