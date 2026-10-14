package com.lucas.organizador_contas.model;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma cobrança de um mês de uma assinatura (ex.: "Internet, outubro/2026").
 * Nasce como "pendente" e o usuário marca como "pago".
 */
@Entity
@Table(
        name = "pagamentos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assinatura_id", "data_vencimento"})
)
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Se a assinatura for excluída, as cobranças dela somem junto (ON DELETE CASCADE no banco).
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "assinatura_id", nullable = false)
    private Assinatura assinatura;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private String status = "pendente"; // "pendente" ou "pago"

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public Assinatura getAssinatura() {
        return assinatura;
    }

    public void setAssinatura(Assinatura assinatura) {
        this.assinatura = assinatura;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
    }
}