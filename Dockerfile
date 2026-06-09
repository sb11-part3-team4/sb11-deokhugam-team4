# 빌드 스테이지
FROM amazoncorretto:17 AS builder

WORKDIR /app

#설정 파일 복사
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle

# 의존성
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

#소스코드 복사
COPY src src
#빌드
RUN ./gradlew bootJar --no-daemon


#실행환경 스테이지
FROM amazoncorretto:17-alpine

WORKDIR /app

#위에서 빌드한 파일만 가져오기
COPY --from=builder /app/build/libs/*.jar app.jar

# non-root 그룹, 사용자 생성
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app/logs \
    && chown -R appuser:appgroup /app/logs
USER appuser

# 환경변수
ENV APP_NAME=deokhugam

ENV JVM_OPTS=""

EXPOSE 80

ENTRYPOINT ["sh", "-c", "java ${JVM_OPTS} -jar app.jar"]


