package com.lucas.organizador_contas.service;

import com.lucas.organizador_contas.dto.CadastroRequest;
import com.lucas.organizador_contas.dto.LoginRequest;
import com.lucas.organizador_contas.model.TokenVerificacao;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.model.Workspace;
import com.lucas.organizador_contas.repository.TokenVerificacaoRepository;
import com.lucas.organizador_contas.repository.UsuarioRepository;
import com.lucas.organizador_contas.repository.WorkspaceRepository;
import com.lucas.organizador_contas.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UsuarioService {

    private static final Logger LOG = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TokenVerificacaoRepository tokenVerificacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    // Guarda quando cada e-mail pediu o último reenvio (evita spam de e-mails).
    private static final long INTERVALO_REENVIO_SEGUNDOS = 60;
    private final Map<String, Instant> ultimosReenvios = new ConcurrentHashMap<>();

    public UsuarioService(UsuarioRepository usuarioRepository,
                          WorkspaceRepository workspaceRepository,
                          TokenVerificacaoRepository tokenVerificacaoRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService,
                          JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.workspaceRepository = workspaceRepository;
        this.tokenVerificacaoRepository = tokenVerificacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public Usuario cadastrar(CadastroRequest request) {
        String nome = request.getNome() == null ? "" : request.getNome().trim();
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String senha = request.getSenha();

        if (nome.isEmpty()) {
            throw new IllegalArgumentException("Informe seu nome.");
        }
        if (email.isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
        if (senha == null || senha.length() < 6) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 6 caracteres.");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }

        String tipo = "familia".equalsIgnoreCase(request.getTipoPerfil()) ? "familia" : "solo";

        Workspace workspace = new Workspace();
        workspace.setTipoPerfil(tipo);
        workspace.setNome(nome + (tipo.equals("familia") ? " - Família" : " - Pessoal"));
        workspace = workspaceRepository.save(workspace);

        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setPapel("admin");
        usuario.setEmailVerificado(false);
        usuario.setWorkspace(workspace);
        usuario = usuarioRepository.save(usuario);

        TokenVerificacao token = new TokenVerificacao();
        token.setToken(UUID.randomUUID().toString());
        token.setUsuario(usuario);
        token.setExpiraEm(LocalDateTime.now().plusHours(24));
        tokenVerificacaoRepository.save(token);

        enviarVerificacaoSemFalhar(usuario.getEmail(), token.getToken());

        return usuario;
    }

    // Se o envio falhar (ex.: sandbox do Resend), o cadastro não é desfeito: só registramos o aviso.
    private void enviarVerificacaoSemFalhar(String email, String token) {
        try {
            emailService.enviarEmailVerificacao(email, token);
        } catch (Exception e) {
            LOG.warn("Não foi possível enviar o e-mail de verificação para {}: {}", email, e.getMessage());
        }
    }

    @Transactional
    public void verificarEmail(String token) {
        TokenVerificacao tokenVerificacao = tokenVerificacaoRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido."));

        if (tokenVerificacao.isExpirado()) {
            throw new IllegalArgumentException("Token expirado. Faça um novo cadastro.");
        }

        Usuario usuario = tokenVerificacao.getUsuario();
        usuario.setEmailVerificado(true);
        usuarioRepository.save(usuario);
    }

    // Gera um link novo e envia de novo. A resposta é sempre a mesma, exista a conta ou não,
    // para ninguém descobrir quais e-mails estão cadastrados.
    @Transactional
    public void reenviarVerificacao(String emailInformado) {
        String email = emailInformado == null ? "" : emailInformado.trim();
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Informe o e-mail.");
        }

        String chave = email.toLowerCase();
        Instant agora = Instant.now();
        Instant ultimo = ultimosReenvios.get(chave);
        if (ultimo != null && agora.isBefore(ultimo.plusSeconds(INTERVALO_REENVIO_SEGUNDOS))) {
            long falta = ultimo.plusSeconds(INTERVALO_REENVIO_SEGUNDOS).getEpochSecond() - agora.getEpochSecond();
            throw new IllegalArgumentException("Aguarde " + Math.max(falta, 1) + "s para pedir outro e-mail.");
        }
        ultimosReenvios.put(chave, agora);

        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            // Só faz sentido para quem já tem senha e ainda não confirmou o e-mail.
            if (usuario.isEmailVerificado() || usuario.getSenha() == null) {
                return;
            }

            tokenVerificacaoRepository.deleteByUsuario_Id(usuario.getId());

            TokenVerificacao token = new TokenVerificacao();
            token.setToken(UUID.randomUUID().toString());
            token.setUsuario(usuario);
            token.setExpiraEm(LocalDateTime.now().plusHours(24));
            tokenVerificacaoRepository.save(token);

            enviarVerificacaoSemFalhar(usuario.getEmail(), token.getToken());
        });
    }

    // "Esqueci minha senha": gera um link de 1 hora e envia por e-mail. A resposta é sempre a mesma,
    // exista a conta ou não, para ninguém descobrir quais e-mails estão cadastrados.
    @Transactional
    public void solicitarRedefinicaoSenha(String emailInformado) {
        String email = emailInformado == null ? "" : emailInformado.trim();
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Informe o e-mail.");
        }

        String chave = "senha:" + email.toLowerCase();
        Instant agora = Instant.now();
        Instant ultimo = ultimosReenvios.get(chave);
        if (ultimo != null && agora.isBefore(ultimo.plusSeconds(INTERVALO_REENVIO_SEGUNDOS))) {
            long falta = ultimo.plusSeconds(INTERVALO_REENVIO_SEGUNDOS).getEpochSecond() - agora.getEpochSecond();
            throw new IllegalArgumentException("Aguarde " + Math.max(falta, 1) + "s para pedir outro e-mail.");
        }
        ultimosReenvios.put(chave, agora);

        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            // Convidado que ainda não definiu senha usa o link do convite, não esta tela.
            if (usuario.getSenha() == null) {
                return;
            }

            tokenVerificacaoRepository.deleteByUsuario_Id(usuario.getId());

            TokenVerificacao token = new TokenVerificacao();
            token.setToken(UUID.randomUUID().toString());
            token.setUsuario(usuario);
            token.setExpiraEm(LocalDateTime.now().plusHours(1));
            tokenVerificacaoRepository.save(token);

            try {
                emailService.enviarEmailRedefinicaoSenha(usuario.getEmail(), token.getToken());
            } catch (Exception e) {
                LOG.warn("Não foi possível enviar o e-mail de redefinição para {}: {}", usuario.getEmail(), e.getMessage());
            }
        });
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Link inválido.");
        }
        if (novaSenha == null || novaSenha.length() < 6) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 6 caracteres.");
        }

        TokenVerificacao tokenVerificacao = tokenVerificacaoRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Link inválido ou já utilizado."));

        if (tokenVerificacao.isExpirado()) {
            throw new IllegalArgumentException("Este link expirou. Peça um novo em \"Esqueci minha senha\".");
        }

        Usuario usuario = tokenVerificacao.getUsuario();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        // Quem recebeu o link provou que tem acesso ao e-mail.
        usuario.setEmailVerificado(true);
        usuarioRepository.save(usuario);

        // O link só vale uma vez.
        tokenVerificacaoRepository.deleteByUsuario_Id(usuario.getId());
    }

    public String login(LoginRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim();

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("E-mail ou senha inválidos."));

        if (usuario.getSenha() == null
                || request.getSenha() == null
                || !passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            throw new IllegalArgumentException("E-mail ou senha inválidos.");
        }

        if (!usuario.isEmailVerificado()) {
            throw new IllegalArgumentException("Confirme seu e-mail antes de entrar.");
        }

        return jwtUtil.gerarToken(usuario.getEmail());
    }
}