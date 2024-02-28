FROM clojure:temurin-11-tools-deps-1.11.1.1435-bullseye
RUN apt-get update && apt-get -y upgrade && \
    groupadd -r efti && useradd -m -r -g efti efti && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
EXPOSE 8080

COPY ./deps.edn /app/
COPY start.sh /app/
COPY ./src/main /app/src/main
RUN chmod -R a+r /app
USER efti
# download dependencies
RUN clojure -P -M

ENTRYPOINT ["bash", "./start.sh"]
