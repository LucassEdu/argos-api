package com.lucas.organizador_contas.service;

import com.lucas.organizador_contas.dto.AlterarSenhaRequest;
import com.lucas.organizador_contas.dto.PerfilRequest;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.model.Workspace;
import com.lucas.organizador_contas.repository.UsuarioRepository;
import com.lucas.organizador_contas.repository.WorkspaceRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PerfilService {

    private static final int LIMITE_FOTO = 200_000; // caracteres (imagem pequena em texto)
    private static final int TAMANHO_MINIMO_SENHA = 6;

    private final UsuarioRepository usuarioRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PasswordEncoder passwordEncoder;

    public PerfilService(UsuarioRepository usuarioRepository,
                         WorkspaceRepository workspaceRepository,
                         PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.workspaceRepository = workspaceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Atualiza só o que veio preenchido: nome, foto e aviso padrão.
    public Usuario atualizarPerfil(Long usuarioId, PerfilRequest request) {
        Usuario usuario = buscar(usuarioId);

        if (request.getNome() != null) {
            String nome = request.getNome().trim();
            if (nome.isEmpty()) {
                throw new IllegalArgumentException("O nome não pode ficar vazio.");
            }
            if (nome.length() > 80) {
                throw new IllegalArgumentException("O nome deve ter até 80 caracteres.");
            }
            usuario.setNome(nome);
            acompanharNomeNoWorkspace(usuario, nome);
        }

        if (request.getFoto() != null) {
            usuario.setFoto(fotoValida(request.getFoto()));
        }

        if (request.getDiasAvisoPadrao() != null) {
            int dias = request.getDiasAvisoPadrao();
            if (dias < 0 || dias > 30) {
                throw new IllegalArgumentException("O aviso padrão deve ficar entre 0 e 30 dias.");
            }
            usuario.setDiasAvisoPadrao(dias);
        }

        return usuarioRepository.save(usuario);
    }

    public void alterarSenha(Long usuarioId, AlterarSenhaRequest request) {
        Usuario usuario = buscar(usuarioId);

        if (usuario.getSenha() == null) {
            throw new IllegalArgumentException("Esta conta ainda não tem senha definida.");
        }
        if (request.getSenhaAtual() == null
                || !passwordEncoder.matches(request.getSenhaAtual(), usuario.getSenha())) {
            throw new IllegalArgumentException("A senha atual está incorreta.");
        }
        String nova = request.getNovaSenha();
        if (nova == null || nova.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalArgumentException(
                    "A nova senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
        if (passwordEncoder.matches(nova, usuario.getSenha())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da atual.");
        }

        usuario.setSenha(passwordEncoder.encode(nova));
        usuarioRepository.save(usuario);
    }

    // Se o workspace é o "<Nome> - Pessoal" criado no cadastro, o nome dele acompanha o do dono.
    // Workspaces de família ou com nome personalizado não são mexidos.
    private void acompanharNomeNoWorkspace(Usuario usuario, String novoNome) {
        Workspace ws = usuario.getWorkspace();
        if (ws == null || !"admin".equals(usuario.getPapel())) {
            return;
        }
        if ("solo".equals(ws.getTipoPerfil())
                && ws.getNome() != null && ws.getNome().endsWith(" - Pessoal")) {
            ws.setNome(novoNome + " - Pessoal");
            workspaceRepository.save(ws);
        }
    }

    private Usuario buscar(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    }

    // Aceita só imagem em data URL e de tamanho razoável; vazio vira "sem foto".
    private String fotoValida(String foto) {
        if (foto.isBlank()) {
            return null;
        }
        if (!foto.startsWith("data:image/")) {
            throw new IllegalArgumentException("A foto precisa ser uma imagem.");
        }
        if (foto.length() > LIMITE_FOTO) {
            throw new IllegalArgumentException("A foto é grande demais. Use uma menor.");
        }
        return foto;
    }
}