# 1. 빌드된 jar 파일을 실행할 환경 (Java 17 사용 시)
FROM eclipse-temurin:17-jdk-alpine

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 빌드된 jar 파일을 컨테이너 안으로 복사
# build/libs/ 폴더에 생성된 실제 jar 파일 이름을 확인하세요!
COPY build/libs/*.jar app.jar

# 4. 앱 실행
ENTRYPOINT ["java", "-jar", "app.jar"]