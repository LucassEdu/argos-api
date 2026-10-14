package com.lucas.organizador_contas.repository;

import com.lucas.organizador_contas.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
}