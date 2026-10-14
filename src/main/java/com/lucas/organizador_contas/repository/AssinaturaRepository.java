package com.lucas.organizador_contas.repository;

import com.lucas.organizador_contas.model.Assinatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {

    List<Assinatura> findByWorkspace_Id(Long workspaceId);
}