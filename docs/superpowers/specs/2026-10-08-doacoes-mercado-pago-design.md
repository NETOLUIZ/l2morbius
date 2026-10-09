# Doações Pix via Mercado Pago

## Objetivo

Adicionar ao site PHP uma área autenticada para contribuições voluntárias via Pix, usando Mercado Pago, vinculada à conta do jogo. A contribuição será destinada exclusivamente à manutenção da infraestrutura do servidor; não haverá venda, entrega ou desbloqueio de itens e vantagens.

## Escopo

- Autenticação no site usando `accounts.login` e o formato de senha já usado pelo cadastro: Base64 de SHA-1 binário.
- Sessão PHP segura, logout e proteção CSRF.
- Página de doação com valor livre dentro de limites configuráveis.
- Criação server-side de pagamento Pix via Mercado Pago.
- Exibição de QR Code e código copia-e-cola sem expor o Access Token.
- Webhook que consulta o pagamento na API do Mercado Pago antes de confirmar.
- Persistência idempotente das doações no MariaDB.
- Aviso claro de contribuição voluntária, sem compra de itens ou benefícios.
- Variáveis de ambiente para credenciais e configuração; nenhuma credencial hardcoded.

Fora do escopo: checkout com cartão, reembolso automático, painel administrativo, emissão fiscal, recompensas no jogo e autenticação social.

## Fluxo

1. Usuário abre `login.php` e informa login e senha do jogo.
2. O servidor consulta `accounts` com prepared statement e compara o hash esperado sem registrar a senha.
3. Após sucesso, o servidor regenera o ID da sessão e guarda apenas o login autenticado.
4. `doar.php` exige sessão, token CSRF, valor numérico válido e limite de requisições.
5. `donate_create.php` gera um `external_reference` aleatório, grava a doação como `pending` e cria o pagamento Pix no Mercado Pago usando cURL e `X-Idempotency-Key`.
6. O navegador recebe apenas os dados necessários do Pix e o identificador público da doação.
7. `donate_webhook.php` aceita notificações, extrai o ID, consulta o pagamento na API e valida status, moeda, valor e `external_reference`.
8. Uma transação atualiza a doação uma única vez para `approved`, `rejected` ou `cancelled`; notificações repetidas são inofensivas.
9. A página de retorno consulta o estado no servidor e nunca considera o pagamento confirmado apenas pela URL de retorno.

## Modelo de dados

Tabela `donations` no banco do jogo:

- `id` BIGINT auto_increment primary key
- `login` VARCHAR(45) not null, índice
- `external_reference` CHAR(64) not null unique
- `payment_id` VARCHAR(64) nullable unique
- `amount` DECIMAL(10,2) not null
- `currency` CHAR(3) not null default `BRL`
- `status` VARCHAR(20) not null default `pending`
- `pix_qr_code` TEXT nullable
- `pix_qr_code_base64` MEDIUMTEXT nullable
- `created_at`, `updated_at`, `approved_at` timestamps

Não serão armazenados dados de cartão, senha, Access Token ou payload completo desnecessário.

## Segurança

- Access Token somente em variável de ambiente (`MP_ACCESS_TOKEN`); nunca em HTML, JavaScript ou repositório.
- URL do webhook configurável (`MP_WEBHOOK_URL`) e respostas sem detalhes internos.
- Prepared statements para banco, escape de saída HTML e validação estrita de valores.
- CSRF em login, logout e criação de pagamento; cookies `HttpOnly`, `SameSite=Lax` e `Secure` quando HTTPS estiver ativo.
- Regeneração de sessão após login e expiração por inatividade.
- Limite de tentativas de login e criação de pagamentos por IP/sessão.
- Idempotência por `external_reference`, `payment_id` e chave de requisição.
- Verificação server-to-server do pagamento no webhook e na página de estado.
- Valor mínimo e máximo configuráveis; somente BRL e Pix.
- Logs sem senha, token ou QR Code completo.
- A configuração de banco usará `DB_PASSWORD`, eliminando a divergência atual com `DB_PASS` e removendo fallback de senha hardcoded em produção.

## Integração visual

- Adicionar “Apoie o servidor” ao menu existente.
- Reutilizar classes de `style.css`, com estados de formulário, erro, pagamento pendente e aprovado.
- Texto destacado: “Esta é uma contribuição voluntária para ajudar a manter o servidor online. Não é uma compra de itens ou vantagens. Todos os itens especiais são obtidos gratuitamente no servidor.”

## Operação e testes

- Verificar sintaxe PHP de todos os novos arquivos.
- Testar login correto, senha incorreta, sessão expirada e CSRF inválido.
- Testar valor inválido, limites, repetição de requisição e falha da API.
- Simular webhook duplicado, pagamento pendente, rejeitado e aprovado.
- Confirmar que o token não aparece na resposta HTTP, HTML ou logs.
- Documentar variáveis de ambiente, URL do webhook e migração SQL.

