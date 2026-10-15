# 앱 실행 이미지. 미리 빌드한 jar(./gradlew :app:bootJar)를 레이어별로 풀어서 담는다.
# Swagger 스펙이 api 테스트로 만들어져서, 이미지 안에서는 Gradle을 돌리지 않는다.

# 1단계: jar를 의존성, 로더, 애플리케이션 코드 레이어로 푼다
FROM eclipse-temurin:21-jre-noble AS extractor
WORKDIR /extractor
COPY app/build/libs/aifilm-app.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# 2단계: 실행 이미지
FROM eclipse-temurin:21-jre-noble
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring
WORKDIR /application

# 잘 바뀌지 않는 레이어부터 복사해서, 코드만 바뀌면 마지막 레이어만 다시 받게 한다
COPY --from=extractor /extractor/extracted/dependencies/ ./
COPY --from=extractor /extractor/extracted/spring-boot-loader/ ./
COPY --from=extractor /extractor/extracted/snapshot-dependencies/ ./
COPY --from=extractor /extractor/extracted/application/ ./

# 배포용 이미지라 기본 프로파일은 prod다. DB 접속 정보는 실행할 때 환경변수로 넘긴다
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Asia/Seoul

USER spring
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "application.jar"]
