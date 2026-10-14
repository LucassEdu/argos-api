package com.lucas.organizador_contas.dto;

import com.lucas.organizador_contas.model.Usuario;

public class MeResponse {

    private Long id;
    private String nome;
    private String email;
    private String papel;
    private Long workspaceId;
    private String workspaceNome;
    private String tipoPerfil;
    private String foto;
    private Integer diasAvisoPadrao;

    public MeResponse(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.papel = usuario.getPapel();
        this.workspaceId = usuario.getWorkspace().getId();
        this.workspaceNome = usuario.getWorkspace().getNome();
        this.tipoPerfil = usuario.getWorkspace().getTipoPerfil();
        this.foto = usuario.getFoto();
        this.diasAvisoPadrao = usuario.getDiasAvisoPadrao();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getPapel() {
        return papel;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public String getWorkspaceNome() {
        return workspaceNome;
    }

    public String getTipoPerfil() {
        return tipoPerfil;
    }

    public String getFoto() {
        return foto;
    }

    public Integer getDiasAvisoPadrao() {
        return diasAvisoPadrao;
    }
}