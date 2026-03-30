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

# Render uses dynamic port ? important
ENV PORT=10000

EXPOSE 10000

# start spring boot
CMD ["java","-Dserver.port=${PORT}","-jar","target/backend.jar"]
