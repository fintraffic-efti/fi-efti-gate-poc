FROM clojure:temurin-11-tools-deps-1.11.1.1435-bullseye
RUN apt-get update && apt-get -y upgrade && \
    groupadd -r xxxx && useradd -m -r -g xxxx xxxx && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
EXPOSE 8080

COPY ./deps.edn /app/
COPY start.sh /app/
COPY ./src/main /app/src/main
# RUN chown -R xxxx:xxxx /app
USER xxxx
# download dependencies
RUN clojure -P -M

ENTRYPOINT ["bash", "./start.sh"]
