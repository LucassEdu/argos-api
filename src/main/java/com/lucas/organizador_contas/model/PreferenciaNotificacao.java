package com.lucas.organizador_contas.model;

import jakarta.persistence.*;

@Entity
@Table(name = "preferencias_notificacao",
       uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "assinatura_id"}))
public class PreferenciaNotificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "assinatura_id", nullable = false)
    private Assinatura assinatura;

    @Column(name = "dias_antecedencia", nullable = false)
    private Integer diasAntecedencia;

    @Column(nullable = false)
    private boolean ativa = true;

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Assinatura getAssinatura() {
        return assinatura;
    }

    public void setAssinatura(Assinatura assinatura) {
        this.assinatura = assinatura;
    }

    public Integer getDiasAntecedencia() {
        return diasAntecedencia;
    }

    public void setDiasAntecedencia(Integer diasAntecedencia) {
        this.diasAntecedencia = diasAntecedencia;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }
}