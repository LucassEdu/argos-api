package com.lucas.organizador_contas.dto;

import com.lucas.organizador_contas.model.Servico;

public class ServicoResponse {

    private Long id;
    private String nome;
    private String categoria;
    private String logo;

    public ServicoResponse(Servico servico) {
        this.id = servico.getId();
        this.nome = servico.getNome();
        this.categoria = servico.getCategoria();
        this.logo = servico.getLogo();
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

    public String getLogo() {
        return logo;
    }
}