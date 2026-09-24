# 🚀 Guia de Deploy L2J Mobius em Docker (VPS / Linux)

Este guia explica como preparar e subir o seu servidor Lineage 2 Interlude (L2J Mobius) em qualquer VPS Linux (Ubuntu 22.04 / 24.04, Debian, etc.) utilizando Docker e Docker Compose.

---

## 📦 Estrutura dos Arquivos Criados

* **`docker-compose.yml`**: Orquestra os 3 serviços:
  * `mariadb`: Banco de dados MariaDB 11.4 com volume persistente e auto-inicialização.
  * `loginserver`: LoginServer rodando com Java 25 LTS (Porta `2106`).
  * `gameserver`: GameServer rodando com Java 25 LTS (Porta `7777`).
* **`docker/Dockerfile`**: Imagem base Java 25 LTS com utilitários.
* **`docker/entrypoint.sh`**: Script que:
  * Aguarda o banco de dados estar 100% pronto.
  * Configura automaticamente os arquivos `Database.ini`.
  * Gera o `ipconfig.xml` com o IP externo da VPS automaticamente.
* **`docker/initdb/01-init.sh`**: Instala as tabelas padrão do Mobius automaticamente na primeira inicialização (ou restaura seu backup customizado se colocado na pasta).
* **`.env`**: Configuração central (IP da VPS, senhas, portas, memória RAM).

---

## 1️⃣ (Opcional) Levar seu Banco de Dados Local Atual para a VPS

Se você já criou contas, NPCs ou itens no seu banco local do XAMPP e quer levar tudo pronto para a VPS:

1. No terminal do Windows, faça o dump do seu banco local:
   ```cmd
   "C:\xampp\mysql\bin\mysqldump.exe" -u root -p l2jmobiusinterlude > "docker\initdb\backup.sql"
   ```
2. Quando o MariaDB do Docker subir pela primeira vez, ele detectará o arquivo `backup.sql` e importará seu banco exatamente como está!

---

## 2️⃣ Preparação da VPS (Ubuntu / Debian)

Conecte-se na sua VPS via SSH e instale o Docker:

```bash
# Atualiza pacotes
sudo apt update && sudo apt upgrade -y

# Instala Docker e Docker Compose
sudo apt install -y docker.io docker-compose-plugin git curl

# Habilita o Docker para iniciar com o sistema
sudo systemctl enable --now docker
```

---

## 3️⃣ Enviar o Projeto para a VPS

Você pode enviar os arquivos do projeto para a VPS de duas formas:

### Opção A: Via Git (Recomendado)
Faça commit e push para um repositório privado no GitHub/GitLab, e na VPS clone:
```bash
git clone <URL_DO_SEU_REPOSITORIO> l2server
cd l2server
```

### Opção B: Via FileZilla / WinSCP / SCP
Envie a pasta do projeto (especialmente as pastas `dist`, `docker`, `docker-compose.yml`, `.env`) para uma pasta na VPS (ex: `/home/ubuntu/l2server`).

---

## 4️⃣ Configurar o `.env` na VPS

Edite o arquivo `.env` na VPS:
```bash
nano .env
```

Ajuste as configurações principais:
```ini
# COLOQUE O IP PÚBLICO DA SUA VPS AQUI:
EXTERNAL_IP=203.0.113.50

# Senha do banco (defina uma senha segura)
DB_PASSWORD=sua_senha_segura_aqui

# Ajuste a memória de acordo com a sua VPS (ex: 4g, 6g, 8g)
GAME_JAVA_XMX=4g
```

---

## 5️⃣ Liberar as Portas no Firewall da VPS

No terminal da VPS, libere as portas necessárias para os jogadores se conectarem:

```bash
# Porta do LoginServer
sudo ufw allow 2106/tcp

# Porta do GameServer
sudo ufw allow 7777/tcp

# SSH (para não perder o acesso)
sudo ufw allow 22/tcp

# Ativar firewall (se ainda não estiver ativo)
sudo ufw enable
```
*(Se sua VPS estiver na Oracle Cloud, AWS, GCP ou Azure, lembre-se de liberar essas portas também no painel web de Security Lists / Security Groups).*

---

## 6️⃣ Iniciar o Servidor

Para iniciar todos os serviços em segundo plano:

```bash
docker compose up -d --build
```

---

## 7️⃣ Comandos Úteis de Gerenciamento

* **Ver os logs do GameServer em tempo real:**
  ```bash
  docker compose logs -f gameserver
  ```

* **Ver os logs do LoginServer em tempo real:**
  ```bash
  docker compose logs -f loginserver
  ```

* **Ver o status dos containers:**
  ```bash
  docker compose ps
  ```

* **Reiniciar apenas o GameServer:**
  ```bash
  docker compose restart gameserver
  ```

* **Parar o servidor:**
  ```bash
  docker compose down
  ```
