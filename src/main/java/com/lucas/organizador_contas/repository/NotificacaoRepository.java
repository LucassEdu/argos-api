package com.lucas.organizador_contas.repository;

import com.lucas.organizador_contas.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByUsuario_IdOrderByCriadoEmDesc(Long usuarioId);

    boolean existsByUsuario_IdAndAssinatura_IdAndDataReferencia(Long usuarioId, Long assinaturaId, LocalDate dataReferencia);
}