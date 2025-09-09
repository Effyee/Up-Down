# 사용할 기본 이미지 (Java 17 JRE)
FROM eclipse-temurin:17-jre-focal

# 애플리케이션 JAR 파일 경로 지정
ARG JAR_FILE=build/libs/*.jar

# JAR 파일을 컨테이너에 복사
COPY ${JAR_FILE} app.jar

# 컨테이너가 실행될 때 JAR 파일을 실행하도록 설정
ENTRYPOINT ["java","-jar","/app.jar"]

# 컨테이너 8080 포트 노출
EXPOSE 8080
