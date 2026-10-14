package com.lucas.organizador_contas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.lucas.organizador_contas.dto.AssinaturaRequest;
import com.lucas.organizador_contas.dto.DashboardResponse;
import com.lucas.organizador_contas.model.Assinatura;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.model.Workspace;
import com.lucas.organizador_contas.repository.AssinaturaRepository;
import com.lucas.organizador_contas.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Regras de assinaturas: quem pode criar/alterar, validação do ciclo e visibilidade.
 * Os repositórios são simulados (mocks), então não precisa de banco nem de Spring.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssinaturaServiceTest {

    @Mock
    private AssinaturaRepository assinaturaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AssinaturaService service;

    private Workspace workspace;
    private Workspace outroWorkspace;
    private Usuario admin;
    private Usuario membro;

    @BeforeEach
    void preparar() {
        workspace = novoWorkspace(1L);
        outroWorkspace = novoWorkspace(2L);
        admin = novoUsuario(10L, "admin", workspace);
        membro = novoUsuario(20L, "membro", workspace);

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(membro));
        // save devolve o próprio objeto recebido, como o JPA faria
        when(assinaturaRepository.save(any(Assinatura.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ---------- auxiliares ----------

    private Workspace novoWorkspace(long id) {
        Workspace w = new Workspace();
        ReflectionTestUtils.setField(w, "id", id);
        return w;
    }

    private Usuario novoUsuario(long id, String papel, Workspace w) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome("Usuario " + id);
        u.setEmail("usuario" + id + "@teste.com");
        u.setPapel(papel);
        u.setWorkspace(w);
        return u;
    }

    private Assinatura novaAssinatura(long id, Usuario responsavel, String visibilidade) {
        Assinatura a = new Assinatura();
        a.setId(id);
        a.setWorkspace(responsavel.getWorkspace());
        a.setResponsavel(responsavel);
        a.setNome("Conta " + id);
        a.setValor(new BigDecimal("10.00"));
        a.setDiaCobranca(5);
        a.setRecorrencia("mensal");
        a.setVisibilidade(visibilidade);
        return a;
    }

    private AssinaturaRequest pedido(String recorrencia, Integer mesInicio) {
        AssinaturaRequest r = new AssinaturaRequest();
        r.setNome("Netflix");
        r.setValor(new BigDecimal("39.90"));
        r.setDiaCobranca(15);
        r.setRecorrencia(recorrencia);
        r.setMesInicio(mesInicio);
        return r;
    }

    private String mensagemDoErro(Runnable acao) {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, acao::run);
        return erro.getMessage();
    }

    // ---------- criar: permissão e campos obrigatórios ----------

    @Test
    @DisplayName("membro comum não pode criar assinatura")
    void membroNaoPodeCriar() {
        String msg = mensagemDoErro(() -> service.criar(20L, pedido("mensal", null)));
        assertTrue(msg.contains("Apenas o admin"));
    }

    @Test
    @DisplayName("nome em branco é recusado")
    void nomeObrigatorio() {
        AssinaturaRequest r = pedido("mensal", null);
        r.setNome("   ");
        assertEquals("Nome da assinatura é obrigatório.", mensagemDoErro(() -> service.criar(10L, r)));
    }

    @Test
    @DisplayName("valor ausente é recusado")
    void valorObrigatorio() {
        AssinaturaRequest r = pedido("mensal", null);
        r.setValor(null);
        assertEquals("Valor é obrigatório.", mensagemDoErro(() -> service.criar(10L, r)));
    }

    @Test
    @DisplayName("dia de cobrança fora de 1 a 31 é recusado")
    void diaDeCobrancaInvalido() {
        for (int dia : new int[] {0, 32}) {
            AssinaturaRequest r = pedido("mensal", null);
            r.setDiaCobranca(dia);
            assertEquals("Dia de cobrança deve estar entre 1 e 31.",
                    mensagemDoErro(() -> service.criar(10L, r)), "dia " + dia);
        }
    }

    // ---------- criar: ciclo ----------

    @Test
    @DisplayName("recorrência desconhecida é recusada")
    void recorrenciaInvalida() {
        String msg = mensagemDoErro(() -> service.criar(10L, pedido("semanal", null)));
        assertTrue(msg.startsWith("Recorrência deve ser"));
    }

    @Test
    @DisplayName("conta que não é mensal exige o mês da primeira cobrança (1 a 12)")
    void cicloLongoExigeMes() {
        assertTrue(mensagemDoErro(() -> service.criar(10L, pedido("trimestral", null)))
                .contains("mês da primeira cobrança"));
        assertTrue(mensagemDoErro(() -> service.criar(10L, pedido("anual", 13)))
                .contains("mês da primeira cobrança"));
        assertTrue(mensagemDoErro(() -> service.criar(10L, pedido("semestral", 0)))
                .contains("mês da primeira cobrança"));
    }

    @Test
    @DisplayName("conta mensal descarta o mês de início")
    void mensalDescartaMes() {
        Assinatura criada = service.criar(10L, pedido("mensal", 5));
        assertNull(criada.getMesInicio());
    }

    @Test
    @DisplayName("conta trimestral guarda o mês de início")
    void trimestralGuardaMes() {
        Assinatura criada = service.criar(10L, pedido("trimestral", 2));
        assertEquals(2, criada.getMesInicio());
        assertEquals("trimestral", criada.getRecorrencia());
    }

    // ---------- criar: valores padrão e responsável ----------

    @Test
    @DisplayName("sem categoria e sem visibilidade, vira 'Geral' e 'privada', com o admin como responsável")
    void valoresPadraoAoCriar() {
        Assinatura criada = service.criar(10L, pedido("mensal", null));
        assertEquals("Geral", criada.getCategoria());
        assertEquals("privada", criada.getVisibilidade());
        assertSame(admin, criada.getResponsavel());
        assertSame(workspace, criada.getWorkspace());
    }

    @Test
    @DisplayName("visibilidade 'compartilhada' é mantida; qualquer outro valor vira 'privada'")
    void visibilidadeAoCriar() {
        AssinaturaRequest compartilhada = pedido("mensal", null);
        compartilhada.setVisibilidade("compartilhada");
        assertEquals("compartilhada", service.criar(10L, compartilhada).getVisibilidade());

        AssinaturaRequest estranha = pedido("mensal", null);
        estranha.setVisibilidade("publica");
        assertEquals("privada", service.criar(10L, estranha).getVisibilidade());
    }

    @Test
    @DisplayName("responsável escolhido do mesmo workspace é aceito")
    void responsavelDoMesmoWorkspace() {
        AssinaturaRequest r = pedido("mensal", null);
        r.setResponsavelId(20L);
        assertSame(membro, service.criar(10L, r).getResponsavel());
    }

    @Test
    @DisplayName("responsável de outro workspace é recusado")
    void responsavelDeOutroWorkspace() {
        Usuario estranho = novoUsuario(30L, "membro", outroWorkspace);
        when(usuarioRepository.findById(30L)).thenReturn(Optional.of(estranho));

        AssinaturaRequest r = pedido("mensal", null);
        r.setResponsavelId(30L);
        assertTrue(mensagemDoErro(() -> service.criar(10L, r)).contains("mesmo workspace"));
    }

    // ---------- pausar / reativar ----------

    @Test
    @DisplayName("admin pausa e reativa a conta")
    void alterarStatus() {
        Assinatura a = novaAssinatura(1L, admin, "privada");
        when(assinaturaRepository.findById(1L)).thenReturn(Optional.of(a));

        assertEquals("pausada", service.alterarStatus(10L, 1L, "pausada").getStatus());
        assertEquals("ativa", service.alterarStatus(10L, 1L, "ativa").getStatus());
    }

    @Test
    @DisplayName("status diferente de 'ativa' ou 'pausada' é recusado")
    void statusInvalido() {
        Assinatura a = novaAssinatura(1L, admin, "privada");
        when(assinaturaRepository.findById(1L)).thenReturn(Optional.of(a));

        assertEquals("Status deve ser 'ativa' ou 'pausada'.",
                mensagemDoErro(() -> service.alterarStatus(10L, 1L, "cancelada")));
    }

    @Test
    @DisplayName("admin não mexe em assinatura de outro workspace")
    void naoAlteraAssinaturaDeOutroWorkspace() {
        Usuario donoDeFora = novoUsuario(30L, "admin", outroWorkspace);
        Assinatura deFora = novaAssinatura(2L, donoDeFora, "compartilhada");
        when(assinaturaRepository.findById(2L)).thenReturn(Optional.of(deFora));

        assertTrue(mensagemDoErro(() -> service.alterarStatus(10L, 2L, "pausada"))
                .contains("não pertence ao seu workspace"));
    }

    @Test
    @DisplayName("não exclui assinatura de outro workspace")
    void naoDeletaAssinaturaDeOutroWorkspace() {
        Usuario donoDeFora = novoUsuario(30L, "admin", outroWorkspace);
        Assinatura deFora = novaAssinatura(2L, donoDeFora, "compartilhada");
        when(assinaturaRepository.findById(2L)).thenReturn(Optional.of(deFora));

        assertTrue(mensagemDoErro(() -> service.deletar(10L, 2L))
                .contains("não pertence ao seu workspace"));
    }

    // ---------- visibilidade ----------

    @Test
    @DisplayName("admin enxerga todas as contas do workspace")
    void adminVeTudo() {
        Assinatura privadaDoAdmin = novaAssinatura(1L, admin, "privada");
        Assinatura compartilhada = novaAssinatura(2L, admin, "compartilhada");
        Assinatura privadaDoMembro = novaAssinatura(3L, membro, "privada");
        when(assinaturaRepository.findByWorkspace_Id(1L))
                .thenReturn(List.of(privadaDoAdmin, compartilhada, privadaDoMembro));

        List<Assinatura> visiveis = service.listarVisiveis(1L, 10L);

        assertEquals(3, visiveis.size());
    }

    @Test
    @DisplayName("membro vê as compartilhadas e as próprias, mas não as privadas dos outros")
    void membroVeSoOQuePode() {
        Assinatura privadaDoAdmin = novaAssinatura(1L, admin, "privada");
        Assinatura compartilhada = novaAssinatura(2L, admin, "compartilhada");
        Assinatura privadaDoMembro = novaAssinatura(3L, membro, "privada");
        when(assinaturaRepository.findByWorkspace_Id(1L))
                .thenReturn(List.of(privadaDoAdmin, compartilhada, privadaDoMembro));

        List<Assinatura> visiveis = service.listarVisiveis(1L, 20L);

        assertEquals(2, visiveis.size());
        assertTrue(visiveis.contains(compartilhada));
        assertTrue(visiveis.contains(privadaDoMembro));
        assertTrue(!visiveis.contains(privadaDoAdmin), "a privada do admin não pode aparecer");
    }

    @Test
    @DisplayName("usuário de outro workspace não lista as contas deste")
    void naoListaWorkspaceAlheio() {
        assertTrue(mensagemDoErro(() -> service.listarVisiveis(2L, 10L))
                .contains("não pertence a este workspace"));
    }

    @Test
    @DisplayName("usuário inexistente é recusado")
    void usuarioInexistenteNaListagem() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        assertEquals("Usuário não encontrado.", mensagemDoErro(() -> service.listarVisiveis(1L, 99L)));
    }

    // ---------- dashboard: gasto mensal ----------

    private Assinatura conta(long id, Usuario responsavel, String categoria, String valor,
            String recorrencia, String status) {
        Assinatura a = novaAssinatura(id, responsavel, "compartilhada");
        a.setCategoria(categoria);
        a.setValor(new BigDecimal(valor));
        a.setRecorrencia(recorrencia);
        a.setMesInicio("mensal".equals(recorrencia) ? null : 1);
        a.setStatus(status);
        return a;
    }

    private void assertValor(String esperado, BigDecimal real, String mensagem) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                mensagem + ": esperado " + esperado + " mas foi " + real);
    }

    @Test
    @DisplayName("gasto mensal divide as contas longas pelos meses do ciclo e ignora as pausadas")
    void gastoMensalDivideOsCiclos() {
        when(assinaturaRepository.findByWorkspace_Id(1L)).thenReturn(List.of(
                conta(1L, admin, "Moradia", "100.00", "mensal", "ativa"),
                conta(2L, admin, "Educação", "1200.00", "anual", "ativa"),
                conta(3L, admin, "Educação", "90.00", "trimestral", "ativa"),
                conta(4L, admin, "Lazer", "500.00", "mensal", "pausada")));

        DashboardResponse dash = service.obterDadosDashboard(1L, 10L);

        // 100 + 1200/12 + 90/3 = 230 (a pausada não entra)
        assertValor("230.00", dash.getTotalMensal(), "total mensal");
        assertValor("100.00", dash.getGastosPorCategoria().get("Moradia"), "Moradia");
        assertValor("130.00", dash.getGastosPorCategoria().get("Educação"), "Educação");
        assertTrue(!dash.getGastosPorCategoria().containsKey("Lazer"), "categoria só com conta pausada não aparece");
        assertEquals(3, dash.getProximosVencimentos().size());
    }

    @Test
    @DisplayName("a divisão do ciclo arredonda para 2 casas (half up)")
    void gastoMensalArredonda() {
        when(assinaturaRepository.findByWorkspace_Id(1L)).thenReturn(List.of(
                conta(1L, admin, "A", "100.00", "anual", "ativa"),
                conta(2L, admin, "B", "100.00", "semestral", "ativa")));

        DashboardResponse dash = service.obterDadosDashboard(1L, 10L);

        assertValor("8.33", dash.getGastosPorCategoria().get("A"), "100/12");
        assertValor("16.67", dash.getGastosPorCategoria().get("B"), "100/6");
        assertValor("25.00", dash.getTotalMensal(), "soma");
    }

    @Test
    @DisplayName("sem categoria, o gasto cai em 'Outros'")
    void gastoSemCategoriaVaiParaOutros() {
        when(assinaturaRepository.findByWorkspace_Id(1L)).thenReturn(List.of(
                conta(1L, admin, null, "40.00", "mensal", "ativa")));

        DashboardResponse dash = service.obterDadosDashboard(1L, 10L);

        assertValor("40.00", dash.getGastosPorCategoria().get("Outros"), "Outros");
    }

    @Test
    @DisplayName("o painel do membro só soma o que ele pode ver")
    void gastoDoMembroRespeitaVisibilidade() {
        Assinatura privadaDoAdmin = conta(1L, admin, "A", "100.00", "mensal", "ativa");
        privadaDoAdmin.setVisibilidade("privada");
        Assinatura compartilhada = conta(2L, admin, "B", "50.00", "mensal", "ativa");
        Assinatura privadaDoMembro = conta(3L, membro, "C", "10.00", "mensal", "ativa");
        privadaDoMembro.setVisibilidade("privada");
        when(assinaturaRepository.findByWorkspace_Id(1L))
                .thenReturn(List.of(privadaDoAdmin, compartilhada, privadaDoMembro));

        DashboardResponse dash = service.obterDadosDashboard(1L, 20L);

        assertValor("60.00", dash.getTotalMensal(), "50 compartilhada + 10 própria");
    }
}
