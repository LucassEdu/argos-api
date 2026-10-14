package com.lucas.organizador_contas.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column // pode ficar nulo até o membro convidado definir a própria senha
    private String senha;

    @Column(nullable = false)
    private String papel; // "admin" ou "membro"

    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado = false;

    @ManyToOne
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    // Foto de perfil, como texto (data URL de uma imagem pequena). Vazio = sem foto.
    @Column(columnDefinition = "TEXT")
    private String foto;

    // Quantos dias antes do vencimento avisar, por padrão, nas contas novas.
    // Nulo nos usuários antigos: o getter devolve 3.
    @Column(name = "dias_aviso_padrao")
    private Integer diasAvisoPadrao;

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getPapel() {
        return papel;
    }

    public void setPapel(String papel) {
        this.papel = papel;
    }

    public boolean isEmailVerificado() {
        return emailVerificado;
    }

    public void setEmailVerificado(boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public Integer getDiasAvisoPadrao() {
        return diasAvisoPadrao != null ? diasAvisoPadrao : 3;
    }

    public void setDiasAvisoPadrao(Integer diasAvisoPadrao) {
        this.diasAvisoPadrao = diasAvisoPadrao;
    }
}