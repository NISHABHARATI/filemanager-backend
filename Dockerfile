FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN ./mvnw clean package -DskipTests || mvn clean package -DskipTests

EXPOSE 10000

CMD ["sh","-c","java -jar target/*.jar"]
