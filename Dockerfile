FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/hrms-1.0.0.jar app.jar
ENV TZ=Asia/Kolkata
EXPOSE 8080
ENTRYPOINT ["java","-Xmx384m","-Duser.timezone=Asia/Kolkata","-jar","app.jar"]