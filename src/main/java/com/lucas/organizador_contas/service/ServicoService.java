package com.lucas.organizador_contas.service;

import com.lucas.organizador_contas.dto.ServicoRequest;
import com.lucas.organizador_contas.model.Servico;
import com.lucas.organizador_contas.model.Usuario;
import com.lucas.organizador_contas.repository.ServicoRepository;
import com.lucas.organizador_contas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicoService {

    private static final int LIMITE_LOGO = 150_000; // caracteres (imagem pequena em texto)

    private final ServicoRepository servicoRepository;
    private final UsuarioRepository usuarioRepository;

    public ServicoService(ServicoRepository servicoRepository, UsuarioRepository usuarioRepository) {
        this.servicoRepository = servicoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Qualquer membro vê os serviços do seu workspace.
    public List<Servico> listar(Long usuarioId) {
        Usuario usuario = buscarUsuario(usuarioId);
        return servicoRepository.findByWorkspace_IdOrderByNomeAsc(usuario.getWorkspace().getId());
    }

    public Servico criar(Long adminId, ServicoRequest request) {
        Usuario admin = validarAdmin(adminId);
        Long workspaceId = admin.getWorkspace().getId();
        String nome = validarNome(request.getNome());

        if (servicoRepository.existsByWorkspace_IdAndNomeIgnoreCase(workspaceId, nome)) {
            throw new IllegalArgumentException("Já existe um serviço com esse nome.");
        }

        Servico servico = new Servico();
        servico.setWorkspace(admin.getWorkspace());
        servico.setNome(nome);
        servico.setCategoria(textoOuPadrao(request.getCategoria(), "Outros"));
        servico.setLogo(logoValido(request.getLogo()));
        return servicoRepository.save(servico);
    }

    public Servico editar(Long adminId, Long servicoId, ServicoRequest request) {
        Usuario admin = validarAdmin(adminId);
        Servico servico = buscarDoWorkspace(servicoId, admin);

        if (request.getNome() != null) {
            String nome = validarNome(request.getNome());
            boolean outroComMesmoNome = servicoRepository
                    .findByWorkspace_IdOrderByNomeAsc(admin.getWorkspace().getId()).stream()
                    .anyMatch(s -> !s.getId().equals(servicoId) && s.getNome().equalsIgnoreCase(nome));
            if (outroComMesmoNome) {
                throw new IllegalArgumentException("Já existe um serviço com esse nome.");
            }
            servico.setNome(nome);
        }
        if (request.getCategoria() != null) {
            servico.setCategoria(textoOuPadrao(request.getCategoria(), "Outros"));
        }
        if (request.getLogo() != null) {
            servico.setLogo(logoValido(request.getLogo()));
        }
        return servicoRepository.save(servico);
    }

    // As contas que já usam o serviço não são afetadas: cada uma guarda a sua cópia do logo.
    public void deletar(Long adminId, Long servicoId) {
        Usuario admin = validarAdmin(adminId);
        Servico servico = buscarDoWorkspace(servicoId, admin);
        servicoRepository.delete(servico);
    }

    // --- Auxiliares ---

    private Usuario buscarUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    }

    private Usuario validarAdmin(Long adminId) {
        Usuario admin = buscarUsuario(adminId);
        if (!"admin".equals(admin.getPapel())) {
            throw new IllegalArgumentException("Apenas o admin do workspace pode gerenciar serviços.");
        }
        return admin;
    }

    private Servico buscarDoWorkspace(Long servicoId, Usuario admin) {
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));
        if (!servico.getWorkspace().getId().equals(admin.getWorkspace().getId())) {
            throw new IllegalArgumentException("Este serviço não pertence ao seu workspace.");
        }
        return servico;
    }

    private String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do serviço é obrigatório.");
        }
        String limpo = nome.trim();
        if (limpo.length() > 80) {
            throw new IllegalArgumentException("O nome do serviço deve ter até 80 caracteres.");
        }
        return limpo;
    }

    private String textoOuPadrao(String texto, String padrao) {
        return (texto == null || texto.isBlank()) ? padrao : texto.trim();
    }

    // Aceita só imagem em data URL e de tamanho razoável; vazio vira "sem logo".
    private String logoValido(String logo) {
        if (logo == null || logo.isBlank()) {
            return null;
        }
        if (!logo.startsWith("data:image/")) {
            throw new IllegalArgumentException("O logo precisa ser uma imagem.");
        }
        if (logo.length() > LIMITE_LOGO) {
            throw new IllegalArgumentException("A imagem do logo é grande demais. Use uma menor.");
        }
        return logo;
    }
}