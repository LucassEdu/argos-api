package com.lucas.organizador_contas.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Validade dos tokens de confirmação de e-mail e de redefinição de senha. */
class TokenVerificacaoTest {

    private TokenVerificacao comExpiracao(LocalDateTime expiraEm) {
        TokenVerificacao t = new TokenVerificacao();
        t.setExpiraEm(expiraEm);
        return t;
    }

    @Test
    @DisplayName("token com prazo no passado está expirado")
    void expiradoQuandoPrazoPassou() {
        assertTrue(comExpiracao(LocalDateTime.now().minusMinutes(1)).isExpirado());
    }

    @Test
    @DisplayName("token com prazo no futuro ainda vale")
    void validoQuandoPrazoNaoChegou() {
        assertFalse(comExpiracao(LocalDateTime.now().plusMinutes(5)).isExpirado());
    }
}
