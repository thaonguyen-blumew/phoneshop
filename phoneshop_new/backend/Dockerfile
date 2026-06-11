# Stage 1: Build ứng dụng bằng Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml và tải dependencies (tận dụng cache của Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy toàn bộ source code và tiến hành build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Tạo image siêu nhẹ chỉ chứa JRE để chạy ứng dụng
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy file .jar đã build từ Stage 1 sang Stage 2
COPY --from=build /app/target/*.jar app.jar

# Mở port 8080
EXPOSE 8080

# Lệnh chạy ứng dụng khi container khởi động
ENTRYPOINT ["java", "-jar", "app.jar"]
