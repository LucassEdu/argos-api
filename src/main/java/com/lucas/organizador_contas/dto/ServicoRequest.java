package com.lucas.organizador_contas.dto;

public class ServicoRequest {

    private String nome;
    private String categoria;
    private String logo; // imagem em data URL; vazio = sem logo

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

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }
}