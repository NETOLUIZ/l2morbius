# Doações Pix via Mercado Pago Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar o sistema de doações voluntárias via Pix com Mercado Pago integrado ao site PHP do servidor L2 Korentech, com autenticação pela conta do jogo, geração server-side de QR Code Pix, webhook seguro e idempotente, e registro no banco de dados.

**Architecture:** Módulos PHP nativos integrados ao layout existente: autenticação com hash SHA-1/Base64 compatível com o jogo (`accounts`), geração de cobrança Pix na API v1 do Mercado Pago com idempotência e segredos em variáveis de ambiente, interface com QR Code e código copia-e-cola, consulta de status em tempo real e webhook com verificação server-to-server.

**Tech Stack:** PHP 8.x, MySQL/MariaDB (extensão mysqli com prepared statements), cURL, HTML5, CSS3 vanilla (design system existente).

**Spec:** `docs/superpowers/specs/2026-10-08-doacoes-mercado-pago-design.md`

## Global Constraints

- Compatibilidade estrita com o formato de senha do emulador L2 e `register.php`: `base64_encode(sha1($password, true))`.
- Nunca expor `MP_ACCESS_TOKEN` em HTML, JavaScript ou repositório.
- Prepared statements para todas as consultas SQL contra MariaDB.
- CSRF token obrigatório em requisições POST (login, doação).
- Cookies de sessão com `HttpOnly`, `SameSite=Lax` e `Secure` (quando HTTPS).
- Limites configuráveis de doação (mínimo R$ 5,00, máximo R$ 1.000,00).
- Idempotência no processamento do webhook por `external_reference` e verificação ativa na API do Mercado Pago.
- Aviso claro de contribuição voluntária para manutenção do servidor sem venda de vantagens.

## Review Focus

1. Tentativa de injeção SQL ou bypass de login: garantido por prepared statement e comparação de hash.
2. Forjamento de webhook: o webhook nunca confia no payload recebido; consulta a API do Mercado Pago diretamente usando o Access Token do servidor.
3. Requisições repetidas de criação de Pix ou cliques múltiplos: protegidas por token de formulário e `X-Idempotency-Key`.
4. Doação com valores fora do limite: validação estrita server-side rejeitando centavos negativos ou valores exorbitantes.
5. Vazamento de credenciais: nenhuma chave no código-fonte, suporte a variáveis de ambiente e fallback seguro.

---

### Task 1: Schema de Dados e Script de Migração

**Files:**
- Create: `site/data/donations_schema.sql`

- [ ] Criar arquivo SQL com a definição da tabela `donations` com chaves primárias, índices em `login`, `external_reference` único e campos para QR Code.
- [ ] Aplicar o schema no banco de dados MariaDB.

---

### Task 2: Configurações, Segurança e Helpers

**Files:**
- Modify: `site/config.php`
- Modify: `.env.example`

- [ ] Ajustar `config.php` para aceitar `DB_PASSWORD` (e fallback retrocompatível `DB_PASS`).
- [ ] Adicionar suporte a `MP_ACCESS_TOKEN`, `MP_WEBHOOK_URL`, limites de doação (`MIN_DONATION_AMOUNT`, `MAX_DONATION_AMOUNT`).
- [ ] Adicionar inicialização segura de sessão (`session_set_cookie_params`).
- [ ] Implementar helpers de autenticação (`isLoggedIn`, `getLoggedInUser`, `requireAuth`) e CSRF (`generateCsrfToken`, `validateCsrfToken`).
- [ ] Atualizar `.env.example` com as variáveis necessárias.

---

### Task 3: Autenticação de Usuários (`login.php` e `logout.php`)

**Files:**
- Create: `site/login.php`
- Create: `site/logout.php`

- [ ] Implementar `site/login.php` com validação de credenciais contra a tabela `accounts` (`base64_encode(sha1($pass, true))`).
- [ ] Implementar regeneração de ID de sessão após login com sucesso e proteção CSRF.
- [ ] Implementar `site/logout.php` com destruição limpa da sessão e redirecionamento.

---

### Task 4: Integração Visual e Navegação

**Files:**
- Modify: `site/header.php`
- Modify: `site/style.css`

- [ ] Adicionar item "Apoie o servidor" no menu de navegação em `header.php`.
- [ ] Exibir status da conta logada no topo ou botão de login/doar.
- [ ] Adicionar classes CSS para formulário de doação, botões de valor predefinido, aviso de voluntariado e caixa de QR Code Pix copia-e-cola.

---

### Task 5: Formulário e Criação de Cobrança Pix (`doar.php`, `donate_create.php`)

**Files:**
- Create: `site/doar.php`
- Create: `site/donate_create.php`

- [ ] Criar `site/doar.php` com valores sugeridos (R$ 10, R$ 25, R$ 50, R$ 100), campo livre, aviso legal sobre contribuição voluntária e histórico das últimas doações da conta.
- [ ] Criar `site/donate_create.php` validando autenticação, CSRF e faixa de valor.
- [ ] Integrar chamada à API do Mercado Pago (`POST /v1/payments`) com cURL e header `X-Idempotency-Key`.
- [ ] Salvar registro em `donations` com status `pending` e armazenar o QR Code / chave copia-e-cola.

---

### Task 6: Visualização do Pagamento e Verificação em Tempo Real (`donate_status.php`, `donate_check.php`)

**Files:**
- Create: `site/donate_status.php`
- Create: `site/donate_check.php`

- [ ] Criar `site/donate_status.php` exibindo o QR Code base64, o código copia-e-cola Pix com botão "Copiar Chave Pix" e status dinâmico.
- [ ] Criar `site/donate_check.php` em JSON para consulta assíncrona do status da doação via JavaScript para confirmação automática na tela.

---

### Task 7: Webhook Server-to-Server (`donate_webhook.php`)

**Files:**
- Create: `site/donate_webhook.php`

- [ ] Implementar receptor de notificações do Mercado Pago com captura de `data.id` / `id`.
- [ ] Consultar a API do Mercado Pago via cURL com `MP_ACCESS_TOKEN` para obter o status real do pagamento.
- [ ] Validar `external_reference`, moeda (`BRL`), valor pago e status (`approved`).
- [ ] Atualizar status da doação de forma transacional e idempotente, definindo `approved_at = NOW()`.
- [ ] Retornar HTTP 200 OK.

---

### Task 8: Testes Automatizados e Verificação

**Files:**
- Create: `site/tests/test_pix_donations.php`

- [ ] Criar script de testes validando:
  - Formato de hash de senha compatível.
  - Validação de CSRF token.
  - Validação de limites numéricos de doação.
  - Formatação e parsing de payload do Mercado Pago.
  - Idempotência na atualização de estado da doação.
- [ ] Executar checagem de sintaxe (`php -l`) em todos os arquivos PHP do projeto.
- [ ] Executar o script de teste e validar aprovação de 100% dos testes.
