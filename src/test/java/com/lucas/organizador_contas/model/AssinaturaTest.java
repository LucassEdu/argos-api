package com.lucas.organizador_contas.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Regras de ciclo de cobrança da assinatura (sem banco, sem Spring). */
class AssinaturaTest {

    private Assinatura com(String recorrencia, Integer mesInicio) {
        Assinatura a = new Assinatura();
        a.setRecorrencia(recorrencia);
        a.setMesInicio(mesInicio);
        return a;
    }

    @ParameterizedTest(name = "{0} cobra a cada {1} mês(es)")
    @CsvSource({
            "mensal,1",
            "bimestral,2",
            "trimestral,3",
            "semestral,6",
            "anual,12"
    })
    @DisplayName("intervaloMeses devolve o tamanho do ciclo")
    void intervaloDeCadaRecorrencia(String recorrencia, int esperado) {
        assertEquals(esperado, com(recorrencia, null).intervaloMeses());
    }

    @Test
    @DisplayName("intervaloMeses assume mensal quando a recorrência é nula ou desconhecida")
    void intervaloPadraoEhMensal() {
        assertEquals(1, com(null, null).intervaloMeses());
        assertEquals(1, com("quinzenal", null).intervaloMeses());
    }

    @Test
    @DisplayName("conta mensal cobra em todos os meses")
    void mensalCobraTodoMes() {
        Assinatura a = com("mensal", null);
        for (int mes = 1; mes <= 12; mes++) {
            assertTrue(a.cobraNoMes(mes), "deveria cobrar no mês " + mes);
        }
    }

    @Test
    @DisplayName("trimestral começando em fevereiro cobra em fev, mai, ago e nov")
    void trimestralComecandoEmFevereiro() {
        Assinatura a = com("trimestral", 2);
        for (int mes : new int[] {2, 5, 8, 11}) {
            assertTrue(a.cobraNoMes(mes), "deveria cobrar no mês " + mes);
        }
        for (int mes : new int[] {1, 3, 4, 6, 7, 9, 10, 12}) {
            assertFalse(a.cobraNoMes(mes), "não deveria cobrar no mês " + mes);
        }
    }

    @Test
    @DisplayName("trimestral começando em novembro dá a volta no ano (nov, fev, mai, ago)")
    void trimestralDaAVoltaNoAno() {
        Assinatura a = com("trimestral", 11);
        for (int mes : new int[] {11, 2, 5, 8}) {
            assertTrue(a.cobraNoMes(mes), "deveria cobrar no mês " + mes);
        }
        assertFalse(a.cobraNoMes(1));
        assertFalse(a.cobraNoMes(12));
    }

    @Test
    @DisplayName("anual cobra uma única vez, no mês de início")
    void anualCobraUmaVez() {
        Assinatura a = com("anual", 3);
        for (int mes = 1; mes <= 12; mes++) {
            assertEquals(mes == 3, a.cobraNoMes(mes), "mês " + mes);
        }
    }

    @Test
    @DisplayName("semestral começando em janeiro cobra em jan e jul")
    void semestral() {
        Assinatura a = com("semestral", 1);
        assertTrue(a.cobraNoMes(1));
        assertTrue(a.cobraNoMes(7));
        assertFalse(a.cobraNoMes(4));
    }

    @Test
    @DisplayName("sem mês de início (conta antiga), o ciclo conta a partir de janeiro")
    void semMesInicioConsideraJaneiro() {
        Assinatura a = com("semestral", null);
        assertTrue(a.cobraNoMes(1));
        assertTrue(a.cobraNoMes(7));
        assertFalse(a.cobraNoMes(2));
    }

    @Test
    @DisplayName("valores padrão: privada e ativa")
    void valoresPadrao() {
        Assinatura a = new Assinatura();
        assertEquals("privada", a.getVisibilidade());
        assertEquals("ativa", a.getStatus());
    }
}
