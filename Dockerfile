FROM clojure:openjdk-11-tools-deps AS base
RUN mkdir -p /usr/src/app/
WORKDIR /usr/src/app/
COPY deps.edn /usr/src/app/deps.edn
RUN clj -e ":ok"
COPY . /usr/src/app
CMD ["clj", "-A:run"]
