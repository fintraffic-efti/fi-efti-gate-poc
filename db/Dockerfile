FROM clojure:openjdk-11-tools-deps-1.11.1.1113

WORKDIR /app

COPY ./deps.edn /app/

# download dependencies
RUN mkdir -p -m 0600 ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts
RUN --mount=type=ssh clojure -P -M

COPY db.sh /app/
COPY ./src /app/src

ENTRYPOINT ["bash", "./db.sh"]
CMD ["migrate"]
