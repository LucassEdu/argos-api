package com.lucas.organizador_contas.repository;

import com.lucas.organizador_contas.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<Servico> findByWorkspace_IdOrderByNomeAsc(Long workspaceId);

    boolean existsByWorkspace_IdAndNomeIgnoreCase(Long workspaceId, String nome);
}