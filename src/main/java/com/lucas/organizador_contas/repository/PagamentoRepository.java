package com.lucas.organizador_contas.repository;

import com.lucas.organizador_contas.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    // Todas as cobranças das assinaturas de um workspace.
    List<Pagamento> findByAssinatura_Workspace_Id(Long workspaceId);

    // Esta assinatura já tem alguma cobrança?
    boolean existsByAssinatura_Id(Long assinaturaId);

    // Esta cobrança (assinatura + data) está com este status? Usado para não avisar o que já foi pago.
    boolean existsByAssinatura_IdAndDataVencimentoAndStatus(Long assinaturaId, LocalDate dataVencimento, String status);

    // Esta assinatura já tem cobrança neste mês ou em algum mês futuro?
    boolean existsByAssinatura_IdAndDataVencimentoGreaterThanEqual(Long assinaturaId, LocalDate data);
}