# Argos API

API do **Argos**, um organizador de contas e assinaturas recorrentes: cadastre aluguel, streaming, energia, cartão e o que mais se repete todo mês ou todo ano, veja quanto isso pesa no orçamento e receba avisos antes dos vencimentos.

> Projeto de portfólio desenvolvido por **Lucas Eduardo Onofre**.
> Interface web: [argos-web](https://github.com/LucassEdu/argos-web)

![Painel do Argos](https://raw.githubusercontent.com/LucassEdu/argos-web/main/docs/contas.png)

## Funcionalidades

- **Contas e assinaturas** com ciclos mensal, trimestral, semestral e anual. O gasto fixo mensal divide as contas longas pelos meses do ciclo.
- **Catálogo de serviços** com marcas conhecidas e a opção de criar serviços próprios.
- **Controle de pagamentos:** marcar como paga, desfazer e consultar o histórico de cada conta.
- **Avisos de vencimento** por e-mail e por notificações dentro do app, com dias de antecedência configuráveis.
- **Espaço individual ou em família**, com convite de membros e contas privadas ou compartilhadas.
- **Painel de gráficos:** gasto por categoria e pagamentos dos últimos meses.
- **Autenticação completa:** cadastro, confirmação de e-mail com reenvio, login com JWT, recuperação e troca de senha.
- **Perfil** com foto e preferências de aviso.

## Tecnologias

| Camada | Tecnologias |
| --- | --- |
| Linguagem | Java 17 |
| Framework | Spring Boot 4.1.1, Spring Web, Spring Data JPA |
| Segurança | Spring Security, JWT stateless |
| Banco de dados | PostgreSQL |
| E-mails | Resend |
| Build | Maven (wrapper incluso) |

## Como rodar

**Pré-requisitos:** Java 17, PostgreSQL e uma conta gratuita no [Resend](https://resend.com) para a chave de API.

1. Crie o banco de dados:

   ```sql
   CREATE DATABASE organizador_contas;
   ```

2. Copie o arquivo de exemplo e preencha com os seus valores (o `.env` fica fora do Git):

   ```bash
   cp .env.example .env
   ```

   | Variável | Obrigatória | Descrição |
   | --- | --- | --- |
   | `DB_PASSWORD` | sim | Senha do usuário do PostgreSQL |
   | `RESEND_API_KEY` | sim | Chave da API do Resend |
   | `JWT_SECRET` | sim | Texto aleatório com **32 caracteres ou mais** |
   | `DB_URL`, `DB_USER` | não | Padrão: `jdbc:postgresql://localhost:5432/organizador_contas` e `postgres` |
   | `FRONTEND_URL` | não | Origem liberada no CORS. Padrão: `http://localhost:4200` |
   | `BACKEND_URL` | não | Usada nos links dos e-mails. Padrão: `http://localhost:8080` |
   | `JWT_EXPIRACAO_HORAS` | não | Validade do login. Padrão: 10 |

3. Suba a API (as tabelas são criadas automaticamente na primeira execução):

   ```bash
   ./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
   ```

A API fica em `http://localhost:8080`. Depois, rode o [frontend](https://github.com/LucassEdu/argos-web).

## Decisões técnicas

- **Segredos fora do código.** Senha do banco, chave do Resend e segredo do JWT vêm de variáveis de ambiente (`.env` local, ignorado pelo Git). O `application.properties` não guarda nenhum valor sensível.
- **API stateless com JWT.** Nada de sessão no servidor: cada requisição traz o token, e o usuário autenticado é resolvido a partir dele.
- **Isolamento por dono.** Cada operação confere se o recurso pertence ao usuário ou ao espaço dele. Por exemplo, só o dono de uma notificação consegue marcá-la como lida.
- **CORS configurável** por variável de ambiente, em vez de liberar qualquer origem.
- **Tokens de uso único** para confirmação de e-mail e recuperação de senha, com prazo de validade.
- **Pagamentos gerados sob demanda.** O histórico é criado conforme o período é consultado, e não de uma vez para todos os meses.
- **Estrutura em camadas:** `controller` → `service` → `repository`, com `dto` separando o que entra e sai da API das entidades.

## Limitações conhecidas

- O Resend, no modo de testes, só entrega e-mails para o dono da conta. Para enviar a outros destinatários é preciso verificar um domínio próprio.
- Testes automatizados ainda estão em construção (hoje há apenas o teste de carregamento do contexto).

## Autor

**Lucas Eduardo Onofre**, estudante de Análise e Desenvolvimento de Sistemas, Manaus - AM.
GitHub: [@LucassEdu](https://github.com/LucassEdu)
