#!/bin/bash
set -e

DB_HOST=${DB_HOST:-mariadb}
DB_PORT=${DB_PORT:-3306}
DB_USER=${DB_USER:-root}
DB_PASSWORD=${DB_PASSWORD:-l2jrootpass}
DB_NAME=${DB_NAME:-l2jmobiusinterlude}
EXTERNAL_IP=${EXTERNAL_IP:-2.24.108.110}

echo "=========================================================="
echo " Starting L2J Mobius Server: ${SERVER_TYPE^^}"
echo "=========================================================="

# 1. Aguarda o banco de dados estar pronto
echo "[*] Aguardando banco de dados em ${DB_HOST}:${DB_PORT}..."
until nc -z -v -w5 "${DB_HOST}" "${DB_PORT}" 2>/dev/null; do
    echo "[!] Banco de dados indisponível, aguardando 3s..."
    sleep 3
done
echo "[+] Conexão com o banco de dados estabelecida com sucesso!"

# 2. Verifica se o banco de dados precisa ser populado
if command -v mariadb &> /dev/null; then
    TABLE_COUNT=$(mariadb -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -sse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}';" 2>/dev/null || echo "0")
    if [ "$TABLE_COUNT" = "0" ] || [ -z "$TABLE_COUNT" ]; then
        echo "[*] Banco de dados vazio. Instalando tabelas do Mobius automaticamente..."
        if [ -d "/app/dist/db_installer/sql/login" ]; then
            for f in /app/dist/db_installer/sql/login/*.sql; do
                [ -f "$f" ] && mariadb -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" < "$f" 2>/dev/null || true
            done
        fi
        if [ -d "/app/dist/db_installer/sql/game" ]; then
            for f in /app/dist/db_installer/sql/game/*.sql; do
                [ -f "$f" ] && mariadb -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" < "$f" 2>/dev/null || true
            done
        fi
        echo "[+] Tabelas instaladas com sucesso!"
    else
        echo "[+] Banco de dados já possui ${TABLE_COUNT} tabelas."
    fi
fi

# Função para atualizar Database.ini
update_database_config() {
    local config_file="$1"
    if [ -f "$config_file" ]; then
        echo "[*] Ajustando configurações em ${config_file}..."
        sed -i "s|^URL = .*|URL = jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true\&characterEncoding=utf-8\&allowPublicKeyRetrieval=true\&useSSL=false\&connectTimeout=10000\&interactiveClient=true\&sessionVariables=wait_timeout=600,interactive_timeout=600\&autoReconnect=true|g" "$config_file"
        sed -i "s|^Login = .*|Login = ${DB_USER}|g" "$config_file"
        sed -i "s|^Password = .*|Password = ${DB_PASSWORD}|g" "$config_file"
    fi
}

if [ "$SERVER_TYPE" = "login" ]; then
    cd /app/dist/login

    # Configura o banco
    update_database_config "config/Database.ini"

    # Garante binding em todas as interfaces para permitir conexão do GameServer
    if [ -f "config/Server.ini" ]; then
        sed -i "s|^LoginserverHostname = .*|LoginserverHostname = 0.0.0.0|g" "config/Server.ini"
        sed -i "s|^LoginHostname = .*|LoginHostname = 0.0.0.0|g" "config/Server.ini"
        sed -i "s|^AutoCreateAccounts = .*|AutoCreateAccounts = True|g" "config/Server.ini"
    fi

    mkdir -p log

    JAVA_OPT="-server -Dfile.encoding=UTF-8 -XX:+UseZGC -Xms${JAVA_XMS:-128m} -Xmx${JAVA_XMX:-512m}"
    echo "[+] Iniciando LoginServer com JVM options: ${JAVA_OPT}"
    exec java ${JAVA_OPT} -jar ../libs/LoginServer.jar

elif [ "$SERVER_TYPE" = "game" ]; then
    cd /app/dist/game

    # Configura o banco
    update_database_config "config/Database.ini"

    # Remove qualquer spawn residual do mob 60001 no banco
    if command -v mariadb &> /dev/null; then
        mariadb -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" -e "DELETE FROM spawnlist WHERE npc_templateid=60001; DELETE FROM custom_spawnlist WHERE npc_templateid=60001;" 2>/dev/null || true
    fi

    # Configura o endereço do LoginServer
    LOGIN_HOST=${LOGIN_HOST:-loginserver}
    LOGIN_PORT=${LOGIN_PORT:-9014}
    if [ -f "config/Server.ini" ]; then
        sed -i "s|^LoginHost = .*|LoginHost = ${LOGIN_HOST}|g" "config/Server.ini"
        sed -i "s|^LoginPort = .*|LoginPort = ${LOGIN_PORT}|g" "config/Server.ini"
        sed -i "s|^GameserverHostname = .*|GameserverHostname = 0.0.0.0|g" "config/Server.ini"
        if [ -n "$PACKET_ENCRYPTION" ]; then
            sed -i "s|^PacketEncryption = .*|PacketEncryption = ${PACKET_ENCRYPTION}|g" "config/Server.ini"
        fi
    fi

    # Gera o arquivo ipconfig.xml com o IP externo da VPS
    echo "[*] Gerando ipconfig.xml com EXTERNAL_IP = ${EXTERNAL_IP}..."
    cat <<EOF > config/ipconfig.xml
<?xml version="1.0" encoding="UTF-8"?>
<gameserver address="${EXTERNAL_IP}" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="../data/xsd/ipconfig.xsd">
	<!-- Localhost -->
	<define subnet="127.0.0.0/8" address="127.0.0.1" />
	<!-- Redes internas / Docker / LAN -->
	<define subnet="10.0.0.0/8" address="10.0.0.0" />
	<define subnet="172.16.0.0/12" address="172.16.0.0" />
	<define subnet="192.168.0.0/16" address="192.168.0.0" />
</gameserver>
EOF

    # Aguarda o LoginServer abrir a porta de comunicação interna (9014)
    echo "[*] Aguardando LoginServer em ${LOGIN_HOST}:${LOGIN_PORT}..."
    until nc -z -v -w5 "${LOGIN_HOST}" "${LOGIN_PORT}" 2>/dev/null; do
        echo "[!] LoginServer ainda não está pronto para receber conexões. Aguardando 4s..."
        sleep 4
    done
    echo "[+] LoginServer detectado!"

    mkdir -p log

    JAVA_OPT="-server -Dfile.encoding=UTF-8 -Djava.util.logging.manager=org.l2jmobius.log.ServerLogManager -Dorg.slf4j.simpleLogger.log.com.zaxxer.hikari=warn -XX:+UseZGC -Xms${JAVA_XMS:-2g} -Xmx${JAVA_XMX:-4g}"
    echo "[+] Iniciando GameServer com JVM options: ${JAVA_OPT}"
    exec java ${JAVA_OPT} -jar ../libs/GameServer.jar

else
    echo "[-] Erro: SERVER_TYPE desconhecido ($SERVER_TYPE). Use 'login' ou 'game'."
    exit 1
fi
