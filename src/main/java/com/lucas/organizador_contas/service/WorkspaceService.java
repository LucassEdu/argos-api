package com.lucas.organizador_contas.service;

import com.lucas.organizador_contas.dto.ConvidarMembroRequest;
import com.lucas.organizador_contas.dto.DefinirSenhaRequest;
import com.lucas.organizador_contas.model.TokenVerificacao;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.model.Workspace;
import com.lucas.organizador_contas.repository.TokenVerificacaoRepository;
import com.lucas.organizador_contas.repository.UsuarioRepository;
import com.lucas.organizador_contas.repository.WorkspaceRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UsuarioRepository usuarioRepository;
    private final TokenVerificacaoRepository tokenVerificacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                             UsuarioRepository usuarioRepository,
                             TokenVerificacaoRepository tokenVerificacaoRepository,
                             PasswordEncoder passwordEncoder,
                             EmailService emailService) {
        this.workspaceRepository = workspaceRepository;
        this.usuarioRepository = usuarioRepository;
        this.tokenVerificacaoRepository = tokenVerificacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public Usuario convidarMembro(Long adminId, ConvidarMembroRequest request) {

        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário admin não encontrado."));

        if (!admin.getPapel().equals("admin")) {
            throw new IllegalArgumentException("Apenas o admin do workspace pode convidar membros.");
        }

        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace não encontrado."));

        if (!admin.getWorkspace().getId().equals(workspace.getId())) {
            throw new IllegalArgumentException("Este admin não pertence a este workspace.");
        }

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }

        Usuario membro = new Usuario();
        membro.setNome(request.getNome() != null ? request.getNome() : request.getEmail());
        membro.setEmail(request.getEmail());
        membro.setSenha(null);
        membro.setPapel("membro");
        membro.setEmailVerificado(false);
        membro.setWorkspace(workspace);
        membro = usuarioRepository.save(membro);

        TokenVerificacao token = new TokenVerificacao();
        token.setToken(UUID.randomUUID().toString());
        token.setUsuario(membro);
        token.setExpiraEm(LocalDateTime.now().plusHours(24));
        tokenVerificacaoRepository.save(token);

        emailService.enviarEmailConvite(membro.getEmail(), workspace.getNome(), token.getToken());

        return membro;
    }

    public void definirSenha(DefinirSenhaRequest request) {
        TokenVerificacao tokenVerificacao = tokenVerificacaoRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido."));

        if (tokenVerificacao.isExpirado()) {
            throw new IllegalArgumentException("Token expirado. Peça um novo convite ao admin.");
        }

        Usuario membro = tokenVerificacao.getUsuario();
        membro.setSenha(passwordEncoder.encode(request.getSenha()));
        membro.setEmailVerificado(true);
        usuarioRepository.save(membro);
    }

    @Transactional
    public void removerMembro(Long adminId, Long membroId) {
        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário admin não encontrado."));

        if (!admin.getPapel().equals("admin")) {
            throw new IllegalArgumentException("Apenas o admin do workspace pode remover membros.");
        }

        Usuario membro = usuarioRepository.findById(membroId)
                .orElseThrow(() -> new IllegalArgumentException("Membro não encontrado."));

        if (!membro.getWorkspace().getId().equals(admin.getWorkspace().getId())) {
            throw new IllegalArgumentException("Este membro não pertence ao seu workspace.");
        }

        if (membro.getId().equals(admin.getId())) {
            throw new IllegalArgumentException("O admin não pode remover a si mesmo.");
        }

        // Remove primeiro qualquer token pendente vinculado a esse membro,
        // senão o banco bloqueia a exclusão por causa da foreign key.
        tokenVerificacaoRepository.deleteByUsuario_Id(membro.getId());

        usuarioRepository.delete(membro);
    }

    public List<Usuario> listarMembros(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace não encontrado."));

        return usuarioRepository.findAll().stream()
                .filter(u -> u.getWorkspace().getId().equals(workspace.getId()))
                .toList();
    }
}