FROM eclipse-temurin:25-jdk

# Instala utilitários essenciais
RUN apt-get update && apt-get install -y --no-install-recommends \
    mariadb-client \
    netcat-traditional \
    procps \
    dos2unix \
    sed \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copia script de inicialização
COPY docker/entrypoint.sh /entrypoint.sh
RUN dos2unix /entrypoint.sh && chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
