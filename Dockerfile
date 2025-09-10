FROM amazoncorretto:17-alpine-jdk AS builder

WORKDIR /workspace/app

# Gradle 의존성 캐싱을 위해 설정 파일 먼저 복사
COPY build.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew dependencies

# 소스코드 전체 복사
COPY . .

# Gradlew에 실행 권한 부여
RUN chmod +x ./gradlew
# 프로젝트 빌드
RUN ./gradlew build -x test

FROM amazoncorretto:17-alpine

ARG JAR_FILE=/workspace/app/build/libs/*.jar

# 빌드 단계에서 생성된 .jar 파일을 최종 이미지로 복사
COPY --from=builder ${JAR_FILE} app.jar

# 8080 포트를 외부에 노출
EXPOSE 8080

# 컨테이너 시작 시 애플리케이션 실행
ENTRYPOINT ["java","-jar","/app.jar"]
