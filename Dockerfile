FROM openjdk:13-jdk-slim
VOLUME /tmp
ARG JAR_FILE
WORKDIR /app

COPY pom.xml pom.xml
COPY quiz-server/pom.xml quiz-server/pom.xml
COPY quiz-server/src quiz-server/src
COPY quiz-server/mvnw.cmd mvnw.cmd
COPY quiz-server/mvnw mvnw
COPY quiz-server/.mvn .mvn

RUN ls -all
RUN chmod +x mvnw
RUN ls -all
RUN ls -all .mvn
RUN ls -all .mvn/wrapper

RUN ./mvnw clean package -DskipTests

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=docker","-jar","quiz-server/target/quiz-server-0.0.1-SNAPSHOT.jar"]