# syntax=docker/dockerfile:1

FROM clojure
# TODO build jarfile before copying it to docker file
# RUN lein uberjar
COPY target/uberjar/br.bsb.liberdade.baas.api.jar .
ADD resources resources
# ENV JDBC_DATABASE_URL="jdbc:postgresql://db:5432/baas?user=liberdade&password=password"
EXPOSE 7780
CMD ["java", "-jar", "br.bsb.liberdade.baas.api.jar", "migrate-up", "up"]
# ENTRYPOINT ["java", "-jar", "br.bsb.liberdade.baas.api.jar"]

