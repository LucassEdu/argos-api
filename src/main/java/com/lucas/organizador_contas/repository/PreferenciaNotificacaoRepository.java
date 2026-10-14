package com.lucas.organizador_contas.repository;

import com.lucas.organizador_contas.model.PreferenciaNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreferenciaNotificacaoRepository extends JpaRepository<PreferenciaNotificacao, Long> {

    List<PreferenciaNotificacao> findByAtivaTrue();

    Optional<PreferenciaNotificacao> findByUsuario_IdAndAssinatura_Id(Long usuarioId, Long assinaturaId);
}