# syntax=docker/dockerfile:1

FROM sapmachine:jre-ubuntu-17
WORKDIR /app
COPY target/uberjar/* .
COPY resources .
# RUN java -jar br.bsb.liberdade.baas.api-0.1.5.jar migrate-up
CMD ["java", "-jar", "br.bsb.liberdade.baas.api-0.1.5.jar", "up"]
EXPOSE 7780

