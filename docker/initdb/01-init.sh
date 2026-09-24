#!/bin/bash
set -e

echo "=========================================================="
echo " Inicializando Banco de Dados L2J Mobius (Primeira Execução)"
echo "=========================================================="

DB_NAME="${MYSQL_DATABASE:-l2jmobiusinterlude}"

# 1. Se existir algum arquivo customizado .sql colocado pelo usuário na pasta initdb, executa ele
for custom_sql in /docker-entrypoint-initdb.d/backup*.sql; do
    if [ -f "$custom_sql" ]; then
        echo "[*] Importando backup customizado: $custom_sql..."
        mariadb -u root -p"${MYSQL_ROOT_PASSWORD}" "${DB_NAME}" < "$custom_sql"
        echo "[+] Backup importado com sucesso!"
        exit 0
    fi
done

# 2. Caso contrário, instala a base padrão do datapack
echo "[*] Importando tabelas do LoginServer..."
if [ -d "/opt/l2j/sql/login" ]; then
    for sql in /opt/l2j/sql/login/*.sql; do
        if [ -f "$sql" ]; then
            mariadb -u root -p"${MYSQL_ROOT_PASSWORD}" "${DB_NAME}" < "$sql"
        fi
    done
fi

echo "[*] Importando tabelas do GameServer..."
if [ -d "/opt/l2j/sql/game" ]; then
    for sql in /opt/l2j/sql/game/*.sql; do
        if [ -f "$sql" ]; then
            mariadb -u root -p"${MYSQL_ROOT_PASSWORD}" "${DB_NAME}" < "$sql"
        fi
    done
fi

echo "[+] Banco de dados L2J Mobius inicializado com sucesso!"
