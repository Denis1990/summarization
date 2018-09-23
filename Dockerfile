FROM maven:3.5.4-slim as maven

RUN mkdir --parents /usr/src/summarization
WORKDIR /usr/src/summarization

COPY lib .
COPY pom.xml /usr/src/summarization

RUN mvn install:install-file -Dfile=nnkstemmer.jar \
    -DgroupId=nnkstemmer -DartifactId=nnkstemmer -Dpackaging=jar \
    -Dversion=1.0

RUN mvn dependency:go-offline -B
RUN mvn verify clean --fail-never
ADD src /usr/src/summarization/src

RUN mvn -o -Prelease clean package

FROM openjdk:8u171-alpine3.8

WORKDIR /summarization
COPY --from=maven /usr/src/summarization/target/summarization.jar ./
COPY --from=maven /usr/src/summarization/target/dependency ./dependency

CMD ["java", "-jar", "summarization.jar"]

