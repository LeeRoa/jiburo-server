# Jiburo 서버 백엔드

![Java](https://img.shields.io/badge/Language-Java-informational?style=flat&logo=java&logoColor=white)
![Gradle](https://img.shields.io/badge/Build-Gradle-02303A?style=flat&logo=gradle&logoColor=white)
![Docker](https://img.shields.io/badge/Container-Docker-2496ED?style=flat&logo=docker&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Framework-Spring%20Boot-6DB33F?style=flat&logo=springboot&logoColor=white)

## 🚀 프로젝트 소개

이 프로젝트는 'Jiburo' 서비스의 백엔드 서버 애플리케이션입니다. 사용자 인증(JWT, OAuth), 데이터베이스 연동, 비즈니스 로직 처리 및 API 제공 등 핵심적인 서버 기능을 담당합니다. 안정적이고 확장 가능한 백엔드 시스템을 구축하여 Jiburo 서비스의 원활한 운영을 지원하는 것을 목표로 합니다.

## ✨ 주요 기능

*   **사용자 인증 및 권한 관리:** JWT(JSON Web Token)를 이용한 토큰 기반 인증 및 OAuth를 통한 외부 로그인 연동 기능.
*   **데이터베이스 연동:** 애플리케이션 데이터를 안전하게 저장하고 관리합니다.
*   **API 제공:** 프론트엔드 및 다른 서비스와의 통신을 위한 RESTful API를 제공합니다.
*   **국제화(i18n) 지원:** 다국어 처리를 위한 기반이 마련되어 있습니다.

## ⚙️ 설치 방법

이 프로젝트는 Docker를 사용하여 쉽게 빌드하고 실행할 수 있습니다.

### 필수 요구사항

*   [Git](https://git-scm.com/)
*   [Docker](https://www.docker.com/get-started) 및 [Docker Compose](https://docs.docker.com/compose/install/)

### 단계별 설치

1.  **리포지토리 클론:**
    ```bash
    git clone [프로젝트_레포지토리_URL]
    cd readme_gen_jiburo-server_j903d1of
    ```
    (여기서 `[프로젝트_레포지토리_URL]`을 실제 GitHub URL로 대체하세요.)

2.  **Docker Compose를 이용한 빌드 및 실행:**
    프로젝트 루트 디렉토리에서 다음 명령어를 실행하여 애플리케이션을 빌드하고 실행합니다.
    ```bash
    docker compose up --build
    ```
    이 명령어는 `Dockerfile`과 `compose.yaml` 파일을 사용하여 애플리케이션 컨테이너를 빌드하고 실행합니다. 모든 의존성(예: 데이터베이스 컨테이너)이 함께 시작됩니다.

3.  **애플리케이션 종료:**
    애플리케이션을 종료하려면 다음 명령어를 사용합니다.
    ```bash
    docker compose down
    ```

## 🛠️ 사용 방법

Docker Compose를 통해 애플리케이션이 성공적으로 시작되면, 백엔드 서버는 기본적으로 `http://localhost:8080` 포트에서 실행됩니다. (환경 설정에 따라 포트가 달라질 수 있습니다.)

*   **API 엔드포인트 접근:**
    외부 클라이언트(예: 웹/모바일 프론트엔드, Postman 등)를 통해 서버의 API 엔드포인트로 요청을 보낼 수 있습니다. 구체적인 API 경로는 애플리케이션 내의 컨트롤러 정의를 참조해야 합니다.

*   **로그 확인:**
    실행 중인 애플리케이션의 로그를 보려면, Docker Compose가 실행 중인 터미널을 확인하거나 `docker logs [컨테이너_이름]` 명령어를 사용할 수 있습니다.

## 📁 프로젝트 구조

프로젝트의 주요 디렉토리 및 파일은 다음과 같습니다.

```
readme_gen_jiburo-server_j903d1of/
├── gradle                                  # Gradle Wrapper 관련 파일
│   └── wrapper
├── src                                     # 소스 코드 디렉토리
│   ├── main                                # 메인 애플리케이션 코드
│   │   ├── java                            # Java 소스 코드
│   │   │   └── com                         # 패키지 루트
│   │   └── resources                       # 리소스 파일
│   │       ├── i18n                        # 국제화(i18n) 관련 파일
│   │       ├── application-db.properties   # 데이터베이스 설정 파일
│   │       ├── application-jwt.properties  # JWT 설정 파일
│   │       ├── application-oauth.properties# OAuth 설정 파일
│   │       ├── application.properties      # 주요 애플리케이션 설정 파일
│   │       └── data.sql                    # 초기 데이터 스크립트
│   └── test                                # 테스트 코드 디렉토리
│       └── java
├── .gitattributes                          # Git 속성 설정 파일
├── .gitignore                              # Git 추적에서 제외할 파일 설정
├── build.gradle                            # Gradle 빌드 스크립트
├── compose.yaml                            # Docker Compose 설정 파일
├── Dockerfile                              # Docker 이미지 빌드 설정 파일
├── gradlew                                 # Gradle Wrapper (Unix/Linux)
├── gradlew.bat                             # Gradle Wrapper (Windows)
└── settings.gradle                         # Gradle 프로젝트 설정
```

## 💡 기술 스택

*   **언어:** Java
*   **프레임워크:** Spring Boot (추론)
*   **빌드 도구:** Gradle
*   **컨테이너화:** Docker, Docker Compose
*   **인증/인가:** JWT (JSON Web Token), OAuth
*   **데이터베이스:** 관계형 데이터베이스 (application-db.properties, data.sql 기반 추론)
*   **국제화:** i18n (resources/i18n 기반 추론)

## 📄 라이센스

이 프로젝트에 대한 라이센스 정보는 별도로 명시되어 있지 않습니다. 프로젝트를 사용하거나 기여할 경우, 라이센스 정책을 확인하거나 프로젝트 관리자에게 문의하여 주시기 바랍니다.

## 🤝 기여 방법

프로젝트에 기여하고자 하시면 다음 절차를 따를 수 있습니다:

1.  이 리포지토리를 포크(Fork)합니다.
2.  새로운 기능 또는 버그 수정을 위한 브랜치를 생성합니다 (`git checkout -b feature/your-feature-name` 또는 `bugfix/issue-description`).
3.  변경 사항을 커밋합니다 (`git commit -m 'Feat: Add some feature'`).
4.  원본 리포지토리로 푸시합니다 (`git push origin feature/your-feature-name`).
5.  풀 리퀘스트(Pull Request)를 제출합니다.

코드 스타일에 맞게 작성하고, 관련 테스트를 포함하여 변경 사항을 검증해 주시기 바랍니다.
