package com.lucas.organizador_contas.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "assinaturas")
public class Assinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    @Column(nullable = false)
    private String nome;

    private String categoria;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(name = "dia_cobranca", nullable = false)
    private Integer diaCobranca;

    // "mensal", "bimestral", "trimestral", "semestral" ou "anual"
    @Column(nullable = false)
    private String recorrencia;

    // Mês (1 a 12) da PRIMEIRA cobrança do ciclo. Os outros meses saem dele.
    // Ex.: trimestral com mês 2 -> fevereiro, maio, agosto, novembro.
    // Fica vazio (null) nas contas mensais e nas contas antigas.
    @Column(name = "mes_inicio")
    private Integer mesInicio;

    // Id do serviço no catálogo do front (ex.: "spotify"). Vazio = sem logo de marca.
    private String icone;

    // Logo enviado pelo cliente, como texto (data URL de uma imagem pequena).
    @Column(columnDefinition = "TEXT")
    private String logo;

    @Column(nullable = false)
    private String visibilidade = "privada"; // "privada" ou "compartilhada"

    @Column(nullable = false)
    private String status = "ativa"; // "ativa" ou "pausada"

    // De quantos em quantos meses a conta é cobrada.
    public int intervaloMeses() {
        if (recorrencia == null) {
            return 1;
        }
        return switch (recorrencia) {
            case "bimestral" -> 2;
            case "trimestral" -> 3;
            case "semestral" -> 6;
            case "anual" -> 12;
            default -> 1;
        };
    }

    // Esta conta é cobrada no mês informado (1 a 12)?
    public boolean cobraNoMes(int mes) {
        int intervalo = intervaloMeses();
        if (intervalo == 1) {
            return true;
        }
        int inicio = (mesInicio != null) ? mesInicio : 1;
        return Math.floorMod(mes - inicio, intervalo) == 0;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public Usuario getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(Usuario responsavel) {
        this.responsavel = responsavel;
    }

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

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public Integer getDiaCobranca() {
        return diaCobranca;
    }

    public void setDiaCobranca(Integer diaCobranca) {
        this.diaCobranca = diaCobranca;
    }

    public String getRecorrencia() {
        return recorrencia;
    }

    public void setRecorrencia(String recorrencia) {
        this.recorrencia = recorrencia;
    }

    public Integer getMesInicio() {
        return mesInicio;
    }

    public void setMesInicio(Integer mesInicio) {
        this.mesInicio = mesInicio;
    }

    public String getIcone() {
        return icone;
    }

    public void setIcone(String icone) {
        this.icone = icone;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getVisibilidade() {
        return visibilidade;
    }

    public void setVisibilidade(String visibilidade) {
        this.visibilidade = visibilidade;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}