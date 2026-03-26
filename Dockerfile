FROM amazoncorretto:17-alpine

WORKDIR /app

# copy maven wrapper files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# give permission
RUN chmod +x mvnw

# download dependencies first (faster build caching)
RUN ./mvnw dependency:go-offline -B

# copy source
COPY src src

# build jar
RUN ./mvnw clean package -DskipTests

# Render uses dynamic port → important
ENV PORT=8080

EXPOSE 8080

# start spring boot
CMD ["java","-Dserver.port=${PORT}","-jar","target/backend-0.0.1-SNAPSHOT.jar"]