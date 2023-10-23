FROM clojure:temurin-17-tools-deps-alpine

WORKDIR /app

COPY clj-kondo /bin/clj-kondo

RUN chmod +x /bin/clj-kondo

RUN apk --no-cache --update add curl ca-certificates tar && \
  curl -Ls https://github.com/sgerrand/alpine-pkg-glibc/releases/download/2.28-r0/glibc-2.28-r0.apk > /tmp/glibc-2.28-r0.apk && \
  apk add --allow-untrusted --force-overwrite /tmp/glibc-2.28-r0.apk
RUN echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' >> /etc/nsswitch.conf

# EXPOSE is merely a hint that a certain ports are useful
EXPOSE 3000

# Copy project to container
COPY . /app

# Start the clojure dev server
CMD ["clojure", "-X:dev"]
