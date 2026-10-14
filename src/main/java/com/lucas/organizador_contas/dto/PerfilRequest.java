package com.lucas.organizador_contas.dto;

// Campos nulos ficam como estão. Foto vazia ("") remove a foto.
public class PerfilRequest {

    private String nome;
    private String foto;
    private Integer diasAvisoPadrao;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public Integer getDiasAvisoPadrao() {
        return diasAvisoPadrao;
    }

    public void setDiasAvisoPadrao(Integer diasAvisoPadrao) {
        this.diasAvisoPadrao = diasAvisoPadrao;
    }
}