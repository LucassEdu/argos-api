package com.lucas.organizador_contas.repository;

import com.lucas.organizador_contas.model.TokenVerificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenVerificacaoRepository extends JpaRepository<TokenVerificacao, Long> {

    Optional<TokenVerificacao> findByToken(String token);

    void deleteByUsuario_Id(Long usuarioId);
}