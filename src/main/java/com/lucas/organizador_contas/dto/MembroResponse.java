package com.lucas.organizador_contas.dto;

import com.lucas.organizador_contas.model.Usuario;

public class MembroResponse {

    private Long id;
    private String nome;
    private String email;
    private String papel;
    private boolean emailVerificado;

    public MembroResponse(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.papel = usuario.getPapel();
        this.emailVerificado = usuario.isEmailVerificado();
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

    public boolean isEmailVerificado() {
        return emailVerificado;
    }
}