package com.lucas.organizador_contas.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailService.class);

    @Value("${resend.api.key}")
    private String resendApiKey;

    // Endereço do site (front-end). Se não existir no application.properties, usa o local.
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    // Endereço do servidor (back-end), usado nos links de confirmação e de convite.
    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    private static final String RESEND_URL = "https://api.resend.com/emails";

    // O nome "Argos" aparece como remetente. O endereço continua o do modo de teste do Resend
    // até existir um domínio próprio verificado.
    private static final String REMETENTE = "Argos <onboarding@resend.dev>";

    // Cores da identidade visual do Argos.
    private static final String COR_PRIMARIA = "#5e1a1e";
    private static final String COR_TEXTO = "#2a2220";
    private static final String COR_SUAVE = "#7a6e6a";

    // Logo do Argos (src/main/resources/email/argos-a.png). Se o arquivo não existir,
    // o e-mail usa um "A" desenhado em HTML no lugar.
    private String logoBase64;

    @PostConstruct
    void carregarLogo() {
        try (InputStream in = new ClassPathResource("email/argos-a.png").getInputStream()) {
            logoBase64 = Base64.getEncoder().encodeToString(in.readAllBytes());
            LOG.info("Logo dos e-mails carregada (email/argos-a.png).");
        } catch (Exception e) {
            logoBase64 = null;
            LOG.warn("Logo dos e-mails NÃO encontrada em src/main/resources/email/argos-a.png; usando o 'A' em HTML.");
        }
    }

    // ---------------------------------------------------------------------
    // E-mails
    // ---------------------------------------------------------------------

    public void enviarEmailVerificacao(String destinatario, String token) {
        String link = backendUrl + "/api/auth/verificar?token=" + token;

        String html = montar(
                "Confirme seu e-mail",
                "<p>Bem-vindo ao Argos! Falta só um passo: confirme que este e-mail é seu para ativar a conta.</p>",
                "Confirmar e-mail",
                link,
                "Este link expira em 24 horas. Se você não criou uma conta no Argos, pode ignorar este e-mail."
        );

        enviar(destinatario, "Confirme seu e-mail no Argos", html);
    }

    public void enviarEmailRedefinicaoSenha(String destinatario, String token) {
        String link = frontendUrl + "/redefinir-senha?token=" + token;

        String html = montar(
                "Redefinir sua senha",
                "<p>Recebemos um pedido para redefinir a senha da sua conta. Clique no botão para escolher uma nova.</p>",
                "Escolher nova senha",
                link,
                "O link expira em 1 hora e só pode ser usado uma vez. Se não foi você, ignore este e-mail: sua senha continua a mesma."
        );

        enviar(destinatario, "Redefinir sua senha do Argos", html);
    }

    public void enviarEmailConvite(String destinatario, String nomeWorkspace, String token) {
        String link = backendUrl + "/api/workspace/definir-senha?token=" + token;

        String html = montar(
                "Você foi convidado",
                "<p>Você foi convidado para participar do espaço <strong>" + escapar(nomeWorkspace)
                        + "</strong> no Argos, onde as contas e assinaturas são organizadas em conjunto.</p>"
                        + "<p>Clique no botão para definir sua senha e ativar o acesso.</p>",
                "Definir minha senha",
                link,
                "Este link expira em 24 horas. Se você não esperava este convite, pode ignorar este e-mail."
        );

        enviar(destinatario, "Você foi convidado para o Argos", html);
    }

    public void enviarEmailVencimento(String destinatario, String nomeAssinatura, long diasRestantes) {
        String quando;
        if (diasRestantes <= 0) {
            quando = "vence <strong>hoje</strong>";
        } else if (diasRestantes == 1) {
            quando = "vence <strong>amanhã</strong>";
        } else {
            quando = "vence em <strong>" + diasRestantes + " dias</strong>";
        }

        String nome = escapar(nomeAssinatura);

        String html = montar(
                "Lembrete de vencimento",
                "<p>Sua conta <strong>" + nome + "</strong> " + quando + ".</p>"
                        + "<p>Confira os detalhes e marque como paga quando quitar.</p>",
                "Abrir o Argos",
                frontendUrl,
                "Você recebe este aviso porque ativou lembretes para esta conta. Dá para mudar isso na própria conta, dentro do Argos."
        );

        enviar(destinatario, "Argos · " + semQuebras(nomeAssinatura) + " " + quandoTexto(diasRestantes), html);
    }

    // ---------------------------------------------------------------------
    // Visual dos e-mails (HTML com estilos inline, que é o que os e-mails aceitam)
    // ---------------------------------------------------------------------

    private String montar(String titulo, String conteudo, String textoBotao, String linkBotao, String nota) {
        return "<!DOCTYPE html><html lang=\"pt-BR\" xml:lang=\"pt-BR\"><head>"
                + "<meta charset=\"UTF-8\"><meta http-equiv=\"Content-Language\" content=\"pt-BR\">"
                + "<meta name=\"google\" content=\"notranslate\"><title>Argos</title></head>"
                + "<body style=\"margin:0;padding:0;background:#f3efee;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f3efee;padding:32px 12px;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:520px;background:#ffffff;border-radius:18px;overflow:hidden;font-family:Segoe UI,Helvetica,Arial,sans-serif;\">"
                // Faixa de cima com a marca
                + "<tr><td style=\"background:" + COR_PRIMARIA + ";padding:26px 32px;\">"
                + marcaDoTopo()
                + "<span style=\"color:#ffffff;font-size:22px;font-weight:700;letter-spacing:-0.3px;vertical-align:middle;margin-left:10px;\">Argos</span>"
                + "</td></tr>"
                // Conteúdo
                + "<tr><td style=\"padding:32px 32px 8px;color:" + COR_TEXTO + ";font-size:16px;line-height:1.6;\">"
                + "<h1 style=\"margin:0 0 16px;font-size:22px;line-height:1.3;color:" + COR_TEXTO + ";\">" + titulo + "</h1>"
                + conteudo
                + "</td></tr>"
                // Botão
                + "<tr><td style=\"padding:8px 32px 32px;\">"
                + "<a href=\"" + linkBotao + "\" style=\"display:inline-block;background:" + COR_PRIMARIA + ";color:#ffffff;"
                + "text-decoration:none;font-weight:600;font-size:15px;padding:13px 26px;border-radius:999px;\">" + textoBotao + "</a>"
                + "</td></tr>"
                // Rodapé
                + "<tr><td style=\"padding:18px 32px 26px;border-top:1px solid #ece6e4;color:" + COR_SUAVE + ";font-size:12px;line-height:1.5;\">"
                + nota
                + "<br><br><span style=\"color:#a89d99;\">Argos · organize suas contas e assinaturas</span>"
                + "</td></tr>"
                + "</table>"
                + "</td></tr></table></body></html>";
    }

    // Logo de verdade (anexada ao e-mail e referenciada por "cid:") ou, sem o arquivo, um "A" em HTML.
    private String marcaDoTopo() {
        if (logoBase64 != null) {
            return "<img src=\"cid:argos-logo\" alt=\"\" height=\"36\" "
                    + "style=\"height:36px;width:auto;border:0;vertical-align:middle;\">";
        }
        return "<span style=\"display:inline-block;width:34px;height:34px;line-height:34px;text-align:center;"
                + "background:#ffffff;color:" + COR_PRIMARIA + ";font-weight:800;font-size:19px;border-radius:10px;vertical-align:middle;\">A</span>";
    }

    private String quandoTexto(long diasRestantes) {
        if (diasRestantes <= 0) {
            return "vence hoje";
        }
        if (diasRestantes == 1) {
            return "vence amanhã";
        }
        return "vence em " + diasRestantes + " dias";
    }

    // Nomes digitados pelo usuário nunca entram crus no HTML do e-mail.
    private String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // O assunto do e-mail não pode ter quebra de linha.
    private String semQuebras(String texto) {
        return texto == null ? "" : texto.replaceAll("[\\r\\n]+", " ").trim();
    }

    // ---------------------------------------------------------------------
    // Envio
    // ---------------------------------------------------------------------

    private void enviar(String destinatario, String assunto, String html) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        Map<String, Object> corpo = new HashMap<>();
        corpo.put("from", REMETENTE);
        corpo.put("to", new String[]{destinatario});
        corpo.put("subject", assunto);
        corpo.put("html", html);

        if (logoBase64 != null) {
            Map<String, Object> logo = new HashMap<>();
            logo.put("filename", "argos-a.png");
            logo.put("content", logoBase64);
            logo.put("content_type", "image/png");
            logo.put("content_id", "argos-logo");
            corpo.put("attachments", List.of(logo));
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(corpo, headers);

        restTemplate.postForEntity(RESEND_URL, request, String.class);
    }
}