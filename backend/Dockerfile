FROM clojure:temurin-21-tools-deps-1.11.1.1435-bullseye
RUN apt-get update && apt-get -y upgrade && \
    groupadd -r efti && useradd -m -r -g efti efti && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
EXPOSE 8080

COPY --chmod=444 ./deps.edn /app/
# download dependencies
USER efti
RUN clojure -P -M

USER root
COPY --chmod=444 start.sh /app/
COPY ./src/main /app/src/main
RUN chmod -R a+rX /app

USER efti
ENTRYPOINT ["bash", "./start.sh"]
