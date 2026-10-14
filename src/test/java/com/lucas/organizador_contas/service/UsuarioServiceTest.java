package com.lucas.organizador_contas.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lucas.organizador_contas.dto.CadastroRequest;
import com.lucas.organizador_contas.dto.LoginRequest;
import com.lucas.organizador_contas.model.TokenVerificacao;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.model.Workspace;
import com.lucas.organizador_contas.repository.TokenVerificacaoRepository;
import com.lucas.organizador_contas.repository.UsuarioRepository;
import com.lucas.organizador_contas.repository.WorkspaceRepository;
import com.lucas.organizador_contas.security.JwtUtil;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cadastro, confirmação de e-mail, recuperação de senha e login.
 * Repositórios, e-mail e JWT são simulados (mocks): nada de banco, rede ou Spring.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private TokenVerificacaoRepository tokenVerificacaoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UsuarioService service;

    // ---------- auxiliares ----------

    private CadastroRequest cadastro(String nome, String email, String senha, String tipo) {
        CadastroRequest r = new CadastroRequest();
        r.setNome(nome);
        r.setEmail(email);
        r.setSenha(senha);
        r.setTipoPerfil(tipo);
        return r;
    }

    private LoginRequest login(String email, String senha) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setSenha(senha);
        return r;
    }

    private Usuario usuario(long id, String email, String senhaCriptografada, boolean verificado) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome("Lucas");
        u.setEmail(email);
        u.setSenha(senhaCriptografada);
        u.setEmailVerificado(verificado);
        return u;
    }

    private TokenVerificacao token(String valor, Usuario dono, LocalDateTime expiraEm) {
        TokenVerificacao t = new TokenVerificacao();
        t.setToken(valor);
        t.setUsuario(dono);
        t.setExpiraEm(expiraEm);
        return t;
    }

    private String mensagemDoErro(Runnable acao) {
        return assertThrows(IllegalArgumentException.class, acao::run).getMessage();
    }

    /** Faz os repositórios devolverem o próprio objeto salvo, como o JPA faria. */
    private void repositoriosDevolvemOQueRecebem() {
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenVerificacaoRepository.save(any(TokenVerificacao.class))).thenAnswer(i -> i.getArgument(0));
    }

    // =====================================================================
    // Cadastro
    // =====================================================================

    @Test
    @DisplayName("cadastro: nome vazio é recusado")
    void cadastroSemNome() {
        assertEquals("Informe seu nome.",
                mensagemDoErro(() -> service.cadastrar(cadastro("  ", "a@b.com", "123456", "solo"))));
    }

    @Test
    @DisplayName("cadastro: e-mail sem @ é recusado")
    void cadastroEmailInvalido() {
        assertEquals("Informe um e-mail válido.",
                mensagemDoErro(() -> service.cadastrar(cadastro("Lucas", "semarroba", "123456", "solo"))));
    }

    @Test
    @DisplayName("cadastro: senha com menos de 6 caracteres é recusada")
    void cadastroSenhaCurta() {
        assertTrue(mensagemDoErro(() -> service.cadastrar(cadastro("Lucas", "a@b.com", "12345", "solo")))
                .contains("pelo menos 6 caracteres"));
    }

    @Test
    @DisplayName("cadastro: e-mail já usado é recusado")
    void cadastroEmailDuplicado() {
        when(usuarioRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertTrue(mensagemDoErro(() -> service.cadastrar(cadastro("Lucas", "a@b.com", "123456", "solo")))
                .contains("já está cadastrado"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("cadastro: grava admin com senha criptografada, e-mail não verificado e token de 24h")
    void cadastroComSucesso() throws Exception {
        repositoriosDevolvemOQueRecebem();
        when(usuarioRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("HASH");

        Usuario criado = service.cadastrar(cadastro("  Lucas ", " a@b.com ", "123456", "solo"));

        assertEquals("Lucas", criado.getNome());
        assertEquals("a@b.com", criado.getEmail());
        assertEquals("HASH", criado.getSenha());
        assertNotEquals("123456", criado.getSenha(), "a senha nunca é guardada em texto puro");
        assertEquals("admin", criado.getPapel());
        assertFalse(criado.isEmailVerificado());

        ArgumentCaptor<Workspace> workspace = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(workspace.capture());
        assertEquals("Lucas - Pessoal", workspace.getValue().getNome());

        ArgumentCaptor<TokenVerificacao> token = ArgumentCaptor.forClass(TokenVerificacao.class);
        verify(tokenVerificacaoRepository).save(token.capture());
        assertFalse(token.getValue().isExpirado());
        assertTrue(token.getValue().getExpiraEm().isAfter(LocalDateTime.now().plusHours(23)));
        assertTrue(token.getValue().getExpiraEm().isBefore(LocalDateTime.now().plusHours(25)));

        verify(emailService).enviarEmailVerificacao(eq("a@b.com"), eq(token.getValue().getToken()));
    }

    @Test
    @DisplayName("cadastro: perfil 'familia' cria o espaço 'Nome - Família'")
    void cadastroFamilia() {
        repositoriosDevolvemOQueRecebem();
        when(passwordEncoder.encode(anyString())).thenReturn("HASH");

        service.cadastrar(cadastro("Lucas", "a@b.com", "123456", "familia"));

        ArgumentCaptor<Workspace> workspace = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(workspace.capture());
        assertEquals("Lucas - Família", workspace.getValue().getNome());
    }

    @Test
    @DisplayName("cadastro: se o e-mail falhar (ex.: sandbox do Resend), o cadastro continua valendo")
    void cadastroSobreviveAFalhaDeEmail() throws Exception {
        repositoriosDevolvemOQueRecebem();
        when(passwordEncoder.encode(anyString())).thenReturn("HASH");
        doThrow(new RuntimeException("sandbox")).when(emailService)
                .enviarEmailVerificacao(anyString(), anyString());

        Usuario criado = assertDoesNotThrow(
                () -> service.cadastrar(cadastro("Lucas", "a@b.com", "123456", "solo")));

        assertEquals("a@b.com", criado.getEmail());
    }

    // =====================================================================
    // Confirmação de e-mail
    // =====================================================================

    @Test
    @DisplayName("verificar e-mail: token que não existe é recusado")
    void verificarTokenInexistente() {
        when(tokenVerificacaoRepository.findByToken("xxx")).thenReturn(Optional.empty());
        assertEquals("Token inválido.", mensagemDoErro(() -> service.verificarEmail("xxx")));
    }

    @Test
    @DisplayName("verificar e-mail: token expirado é recusado e o e-mail continua não verificado")
    void verificarTokenExpirado() {
        Usuario u = usuario(1L, "a@b.com", "HASH", false);
        when(tokenVerificacaoRepository.findByToken("velho"))
                .thenReturn(Optional.of(token("velho", u, LocalDateTime.now().minusMinutes(1))));

        assertTrue(mensagemDoErro(() -> service.verificarEmail("velho")).contains("expirado"));
        assertFalse(u.isEmailVerificado());
    }

    @Test
    @DisplayName("verificar e-mail: token válido marca o usuário como verificado")
    void verificarTokenValido() {
        Usuario u = usuario(1L, "a@b.com", "HASH", false);
        when(tokenVerificacaoRepository.findByToken("bom"))
                .thenReturn(Optional.of(token("bom", u, LocalDateTime.now().plusHours(1))));

        service.verificarEmail("bom");

        assertTrue(u.isEmailVerificado());
        verify(usuarioRepository).save(u);
    }

    // =====================================================================
    // Reenvio da confirmação
    // =====================================================================

    @Test
    @DisplayName("reenvio: e-mail vazio é recusado")
    void reenvioSemEmail() {
        assertEquals("Informe o e-mail.", mensagemDoErro(() -> service.reenviarVerificacao("  ")));
    }

    @Test
    @DisplayName("reenvio: quem ainda não confirmou recebe um token novo de 24h")
    void reenvioParaNaoVerificado() throws Exception {
        Usuario u = usuario(7L, "a@b.com", "HASH", false);
        when(usuarioRepository.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        repositoriosDevolvemOQueRecebem();

        service.reenviarVerificacao("a@b.com");

        verify(tokenVerificacaoRepository).deleteByUsuario_Id(7L);
        ArgumentCaptor<TokenVerificacao> token = ArgumentCaptor.forClass(TokenVerificacao.class);
        verify(tokenVerificacaoRepository).save(token.capture());
        assertTrue(token.getValue().getExpiraEm().isAfter(LocalDateTime.now().plusHours(23)));
        verify(emailService).enviarEmailVerificacao(eq("a@b.com"), eq(token.getValue().getToken()));
    }

    @Test
    @DisplayName("reenvio: quem já confirmou não recebe nada (e a resposta é igual, sem revelar nada)")
    void reenvioParaJaVerificado() throws Exception {
        Usuario u = usuario(7L, "a@b.com", "HASH", true);
        when(usuarioRepository.findByEmail("a@b.com")).thenReturn(Optional.of(u));

        assertDoesNotThrow(() -> service.reenviarVerificacao("a@b.com"));

        verify(tokenVerificacaoRepository, never()).save(any(TokenVerificacao.class));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("reenvio: e-mail desconhecido responde normalmente, sem enviar nada")
    void reenvioParaEmailDesconhecido() {
        when(usuarioRepository.findByEmail("nao@existe.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.reenviarVerificacao("nao@existe.com"));

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("reenvio: segundo pedido em menos de 1 minuto é barrado")
    void reenvioLimitaPorTempo() {
        when(usuarioRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());

        service.reenviarVerificacao("a@b.com");

        assertTrue(mensagemDoErro(() -> service.reenviarVerificacao("a@b.com")).startsWith("Aguarde"));
        // maiúsculas/minúsculas contam como o mesmo e-mail
        assertTrue(mensagemDoErro(() -> service.reenviarVerificacao("A@B.com")).startsWith("Aguarde"));
    }

    // =====================================================================
    // "Esqueci minha senha": pedido do link
    // =====================================================================

    @Test
    @DisplayName("esqueci a senha: e-mail vazio é recusado")
    void esqueciSemEmail() {
        assertEquals("Informe o e-mail.", mensagemDoErro(() -> service.solicitarRedefinicaoSenha("")));
    }

    @Test
    @DisplayName("esqueci a senha: e-mail desconhecido responde normalmente e não envia nada")
    void esqueciEmailDesconhecido() {
        when(usuarioRepository.findByEmail("nao@existe.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.solicitarRedefinicaoSenha("nao@existe.com"));

        verify(tokenVerificacaoRepository, never()).save(any(TokenVerificacao.class));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("esqueci a senha: gera token de 1 hora, apaga os antigos e envia o e-mail")
    void esqueciGeraTokenDeUmaHora() throws Exception {
        Usuario u = usuario(7L, "a@b.com", "HASH", true);
        when(usuarioRepository.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        repositoriosDevolvemOQueRecebem();

        service.solicitarRedefinicaoSenha("a@b.com");

        verify(tokenVerificacaoRepository).deleteByUsuario_Id(7L);
        ArgumentCaptor<TokenVerificacao> token = ArgumentCaptor.forClass(TokenVerificacao.class);
        verify(tokenVerificacaoRepository).save(token.capture());
        assertTrue(token.getValue().getExpiraEm().isAfter(LocalDateTime.now().plusMinutes(50)));
        assertTrue(token.getValue().getExpiraEm().isBefore(LocalDateTime.now().plusMinutes(70)));
        verify(emailService).enviarEmailRedefinicaoSenha(eq("a@b.com"), eq(token.getValue().getToken()));
    }

    @Test
    @DisplayName("esqueci a senha: convidado que ainda não definiu senha não recebe este e-mail")
    void esqueciConvidadoSemSenha() {
        Usuario convidado = usuario(8L, "convidado@b.com", null, false);
        when(usuarioRepository.findByEmail("convidado@b.com")).thenReturn(Optional.of(convidado));

        assertDoesNotThrow(() -> service.solicitarRedefinicaoSenha("convidado@b.com"));

        verify(tokenVerificacaoRepository, never()).save(any(TokenVerificacao.class));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("esqueci a senha: segundo pedido em menos de 1 minuto é barrado")
    void esqueciLimitaPorTempo() {
        when(usuarioRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());

        service.solicitarRedefinicaoSenha("a@b.com");

        assertTrue(mensagemDoErro(() -> service.solicitarRedefinicaoSenha("a@b.com")).startsWith("Aguarde"));
    }

    @Test
    @DisplayName("os limites de reenvio de confirmação e de redefinição de senha são independentes")
    void limitesSaoIndependentes() {
        when(usuarioRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());

        service.reenviarVerificacao("a@b.com");

        assertDoesNotThrow(() -> service.solicitarRedefinicaoSenha("a@b.com"));
    }

    // =====================================================================
    // "Esqueci minha senha": uso do link
    // =====================================================================

    @Test
    @DisplayName("redefinir senha: link vazio é recusado")
    void redefinirSemLink() {
        assertEquals("Link inválido.", mensagemDoErro(() -> service.redefinirSenha(" ", "novaSenha1")));
    }

    @Test
    @DisplayName("redefinir senha: nova senha curta é recusada")
    void redefinirSenhaCurta() {
        assertTrue(mensagemDoErro(() -> service.redefinirSenha("abc", "123")).contains("pelo menos 6 caracteres"));
    }

    @Test
    @DisplayName("redefinir senha: link que não existe (ou já foi usado) é recusado")
    void redefinirLinkInexistente() {
        when(tokenVerificacaoRepository.findByToken("abc")).thenReturn(Optional.empty());
        assertEquals("Link inválido ou já utilizado.",
                mensagemDoErro(() -> service.redefinirSenha("abc", "novaSenha1")));
    }

    @Test
    @DisplayName("redefinir senha: link expirado é recusado e a senha não muda")
    void redefinirLinkExpirado() {
        Usuario u = usuario(7L, "a@b.com", "HASH_ANTIGO", true);
        when(tokenVerificacaoRepository.findByToken("velho"))
                .thenReturn(Optional.of(token("velho", u, LocalDateTime.now().minusMinutes(1))));

        assertTrue(mensagemDoErro(() -> service.redefinirSenha("velho", "novaSenha1")).contains("expirou"));
        assertEquals("HASH_ANTIGO", u.getSenha());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("redefinir senha: link válido troca a senha, verifica o e-mail e vale uma vez só")
    void redefinirComSucesso() {
        Usuario u = usuario(7L, "a@b.com", "HASH_ANTIGO", false);
        when(tokenVerificacaoRepository.findByToken("bom"))
                .thenReturn(Optional.of(token("bom", u, LocalDateTime.now().plusMinutes(30))));
        when(passwordEncoder.encode("novaSenha1")).thenReturn("HASH_NOVO");

        service.redefinirSenha("bom", "novaSenha1");

        assertEquals("HASH_NOVO", u.getSenha());
        assertTrue(u.isEmailVerificado(), "quem abriu o link provou ter acesso ao e-mail");
        verify(usuarioRepository).save(u);
        verify(tokenVerificacaoRepository, times(1)).deleteByUsuario_Id(7L);
    }

    // =====================================================================
    // Login
    // =====================================================================

    @Test
    @DisplayName("login: e-mail desconhecido recebe a mensagem genérica")
    void loginEmailDesconhecido() {
        when(usuarioRepository.findByEmail("nao@existe.com")).thenReturn(Optional.empty());
        assertEquals("E-mail ou senha inválidos.",
                mensagemDoErro(() -> service.login(login("nao@existe.com", "123456"))));
    }

    @Test
    @DisplayName("login: senha errada recebe a mesma mensagem genérica")
    void loginSenhaErrada() {
        Usuario u = usuario(1L, "a@b.com", "HASH", true);
        when(usuarioRepository.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("errada", "HASH")).thenReturn(false);

        assertEquals("E-mail ou senha inválidos.",
                mensagemDoErro(() -> service.login(login("a@b.com", "errada"))));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("login: convidado sem senha definida não consegue entrar")
    void loginSemSenhaDefinida() {
        Usuario convidado = usuario(2L, "convidado@b.com", null, false);
        when(usuarioRepository.findByEmail("convidado@b.com")).thenReturn(Optional.of(convidado));

        assertEquals("E-mail ou senha inválidos.",
                mensagemDoErro(() -> service.login(login("convidado@b.com", "123456"))));
    }

    @Test
    @DisplayName("login: senha certa, mas e-mail ainda não confirmado")
    void loginSemConfirmarEmail() {
        Usuario u = usuario(1L, "a@b.com", "HASH", false);
        when(usuarioRepository.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("123456", "HASH")).thenReturn(true);

        assertTrue(mensagemDoErro(() -> service.login(login("a@b.com", "123456")))
                .contains("Confirme seu e-mail"));
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("login: credenciais certas e e-mail confirmado devolvem o token JWT")
    void loginComSucesso() {
        Usuario u = usuario(1L, "a@b.com", "HASH", true);
        when(usuarioRepository.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("123456", "HASH")).thenReturn(true);
        when(jwtUtil.gerarToken("a@b.com")).thenReturn("TOKEN-JWT");

        assertEquals("TOKEN-JWT", service.login(login(" a@b.com ", "123456")));
    }
}
