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

# Render uses dynamic port
ENV PORT=10000

EXPOSE 10000

# Use shell form so ${PORT} is properly expanded at runtime
CMD ["sh", "-c", "java \
  -XX:TieredStopAtLevel=1 \
  -XX:+UseSerialGC \
  -Xms64m \
  -Xmx256m \
  -Djava.security.egd=file:/dev/./urandom \
  -Dserver.port=${PORT} \
  -jar target/backend.jar"]