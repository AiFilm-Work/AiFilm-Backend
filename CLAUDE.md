# AI FILM

AI 생성 단편영화를 탐색하고 시청하며,
평점과 리뷰를 작성할 수 있는 플랫폼이다.

## Tech Stack

- Java 21
- Spring Boot
- Gradle
- MySQL
- Redis
- Flyway
- Docker

## Modules

Gradle Multi Module 구조를 사용한다.

- `common`: 공통 코드
- `domain`: 핵심 도메인 모델과 비즈니스 규칙
- `infra`: MySQL, Redis, 외부 API 등 인프라 연동
- `api`: 사용자/관리자 REST API
- `worker`: 수집, 동기화, 랭킹 집계 등 백그라운드 작업
- `app`: api와 worker를 조립해 실행하는 유일한 실행 모듈

`api`와 `worker`는 서로 직접 의존하지 않는다.
`api`와 `worker`는 main 클래스가 없는 라이브러리이며, 실행은 `app` 모듈의 `AiFilmApplication` 하나로만 한다.

### api

다음과 같은 HTTP 요청을 처리한다.

- 영화 조회/검색
- 리뷰/평점
- 찜
- 인증
- 관리자 승인/반려

### worker

사용자 HTTP 요청을 처리하지 않는다.

다음과 같은 백그라운드 작업을 담당한다.

- YouTube AI 영화 후보 수집
- YouTube AI 영화 메타데이터 동기화
- YouTube AI 영화 source 삭제/비공개/임베드 상태 검사
- 각종 랭킹 집계 (조회수 Top 20 등)
- Retry
- Scheduler

### app

main 클래스(`AiFilmApplication`)와 실행 설정(`application.yml`)만 둔다.

초기에는 API와 Worker를 하나의 JVM에서 함께 실행하며, 실행 모드는 스위치 두 개로 정한다.

| 실행 모드 | `aifilm.api.enabled` | `aifilm.worker.enabled` |
| --- | --- | --- |
| API + Worker (기본값) | true | true |
| API만 | true | false |
| Worker만 | false | true |

- 각 모듈의 빈은 진입 설정(`ApiModuleConfig`, `WorkerModuleConfig`)을 통해서만 등록된다.
- `AiFilmApplication`의 `scanBasePackages`에 `com.aifilm.api`, `com.aifilm.worker`를 추가하지 않는다. 추가하면 스위치와 무관하게 두 모듈의 빈이 모두 등록된다.

## Architecture Rules

- Controller에서 Repository를 직접 호출하지 않는다.
- Controller에 비즈니스 로직을 작성하지 않는다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- API request/response는 DTO를 사용한다.
- 외부 시스템 연동 구현은 `infra`에 둔다.
- HTTP endpoint는 `api`에 둔다.
- Scheduler 및 백그라운드 실행 진입점은 `worker`에 둔다.
- Scheduler 자체에는 비즈니스 로직을 작성하지 않는다.
- 실행 진입점(main 클래스)은 `app`에만 둔다. `api`·`worker`에는 main 클래스와 `application.yml`을 두지 않는다.
- JPA 스캔 같은 영역별 설정은 메인 클래스가 아니라 해당 모듈의 `@Configuration` 클래스에 둔다. 메인 클래스에 두면 슬라이스 테스트에도 강제로 적용된다.

## API Response

모든 API 응답은 `api` 모듈의 `ApiResponse`로 감싼다.

```
성공: {"status":200,"code":"SUCCESS","message":"요청에 성공했습니다.","data":{...}}
실패: {"status":404,"code":"<에러 코드>","message":"<에러 메시지>","data":null}
```

- 성공은 `ApiResponse.success(data)`, 실패는 `ApiResponse.fail(errorReason)`으로 만든다.
- 성공 응답의 `code`는 항상 `SUCCESS`다. 실패 응답의 `data`는 생략하지 않고 `null`로 내려간다.
- 응답 본문의 `status`와 실제 HTTP 상태 코드를 일치시킨다.
- `data`에는 Entity가 아니라 응답 DTO를 담는다.
- 실패 사유는 `common`의 `ErrorReason`을 구현한 에러 코드 enum으로 표현하고, enum은 도메인별로 나눈다.
- `ErrorReason`의 HTTP 상태는 `int`로 둔다. `common`은 스프링에 의존하지 않으므로 `HttpStatus`를 쓰지 않는다.
- 응답 필드 순서(`status`, `code`, `message`, `data`)는 `@JsonPropertyOrder`로 고정되어 있다. 필드나 순서를 바꾸면 `ApiResponseSerializationTest`도 함께 수정한다.

## API Documentation

API 문서는 Controller 테스트(Spring REST Docs + restdocs-api-spec)로 만들고 Swagger UI로 본다.

```
@ControllerTest 문서화 테스트 → build/generated-snippets
→ openapi3 태스크 → build/api-spec/openapi3.yaml
→ api jar의 static/swagger-ui/openapi3.yaml → /swagger-ui.html
```

- API를 추가하거나 바꾸면 `@ControllerTest` 문서화 테스트를 함께 작성한다. 성공 케이스와 실패 케이스(4xx)를 각각 문서화한다.
- Controller와 DTO에 Swagger(springdoc) 어노테이션을 쓰지 않는다. 문서 내용은 테스트의 `document()`에만 적는다.
- 응답 필드는 모두 문서화한다. 문서와 실제 응답이 다르면 테스트가 실패하며, 이를 피하려고 `relaxedResponseFields`를 쓰지 않는다.
- 같은 method + path의 성공·실패 테스트에는 같은 `tag`, `summary`, `description`을 준다. 스펙에서 하나의 API 아래 응답 코드별로 합쳐진다.
- `document()` 식별자는 영문 kebab-case `{리소스}-{동작}[-{실패 상황}]`로 짓고 겹치지 않게 한다. (예: `docs-sample-get`, `docs-sample-get-not-found`)
- 테스트 소스 `com.aifilm.api.docs`의 샘플 컨트롤러와 테스트는 문서화 파이프라인 확인용이다. 첫 실제 API를 문서화할 때 삭제한다.

import 규칙:

| 용도 | 사용할 클래스 |
| --- | --- |
| `document()` | `com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper` |
| `resource()`, `parameterWithName()` | `com.epages.restdocs.apispec.ResourceDocumentation` (REST Docs의 같은 이름 메서드를 쓰지 않는다) |
| `fieldWithPath()` | `org.springframework.restdocs.payload.PayloadDocumentation` |
| 요청 생성 (`get()` 등) | `org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders` (`MockMvcRequestBuilders`를 쓰면 경로 변수가 문서화되지 않는다) |

스펙 생성과 확인:

```bash
./gradlew :api:openapi3   # api 테스트 후 api/build/api-spec/openapi3.yaml 생성
./gradlew :api:jar        # 생성된 스펙을 jar의 static/swagger-ui/에 포함
```

- `openapi3`는 `check`에 의존하므로 테스트가 실패하면 스펙과 jar가 만들어지지 않는다.
- 스니펫 폴더는 테스트를 시작할 때마다 비워진다. 지운 테스트의 문서는 다음 빌드에서 사라진다.
- 앱 실행 후 `http://localhost:8080/swagger-ui.html`에서 확인한다. IntelliJ로 실행할 때는 먼저 `./gradlew :api:jar`를 실행해야 스펙이 보인다.

Swagger UI 정적 파일(`api/src/main/resources/static/swagger-ui/`):

- swagger-ui dist(v5.33.1) 원본이다. 직접 고치지 않는다.
- 원본에서 바꾼 곳은 `swagger-ui.html`(`index.html` 이름 변경, 리소스를 `/static/swagger-ui/` 절대 경로로)과 `swagger-initializer.js`의 `url` 두 군데다. 버전을 올릴 때는 dist를 통째로 바꾼 뒤 이 두 곳만 다시 고친다.

## Code Convention

- 주석은 짧은 일반 주석(`//`)으로 "무엇을, 왜"의 핵심만 적는다. Javadoc(`/** */`)과 HTML 태그는 쓰지 않는다.
- 자세한 배경과 결정 이유는 커밋 본문에 남긴다.

## Database

- MySQL을 사용한다.
- 스키마 변경은 Flyway migration으로 관리한다.
- `ddl-auto=create/update`는 사용하지 않는다.
- 운영 환경에서는 `validate`를 사용한다.
- 이미 적용된 migration 파일은 수정하지 않는다.
- 스키마 변경이 필요한 경우 새로운 migration 파일을 추가한다.

## Testing

코드 변경 시 변경된 동작에 대한 테스트를 작성하거나 수정하고 실행한다.

사용 도구:

- JUnit 5
- AssertJ
- Mockito

기본 원칙:

- 비즈니스 로직 변경 시 관련 테스트를 함께 작성한다.
- 버그 수정 시 회귀 테스트를 추가한다.
- 테스트 대상 객체 자체를 mock으로 만들지 않는다.
- 외부 의존성만 필요한 범위에서 mock 처리한다.
- 테스트를 통과시키기 위해 assertion을 약화하거나 테스트를 삭제하지 않는다.
- 외부 운영 API를 실제로 호출하는 테스트는 기본 테스트에 포함하지 않는다.

Worker에서는 특히 다음을 검증한다.

- 중복 실행 방지
- Retry 조건 및 최대 횟수
- 실패 상태 전이
- 멱등성
- 외부 API 실패 처리

테스트 실행:

```bash
./gradlew test
./gradlew :app:test
./gradlew :api:test
./gradlew :worker:test
./gradlew :domain:test
./gradlew :infra:test
```

- `app`의 테스트(`@SpringBootTest`)는 로컬 MySQL(`docker compose up -d`)과 `.env`의 DB 환경변수가 필요하다.
- `api`의 웹 계층 테스트는 테스트 전용 설정(`ApiTestApplication`)으로 `@WebMvcTest`를 사용하며, DB 없이 실행된다.

작업 완료 전 최소한 변경된 모듈의 테스트를 실행한다.
테스트를 실행하지 못한 경우 그 사실과 이유를 명시한다.

## Git Convention

Branch:

feat/#12-movie-search
fix/#34-ranking-cache
refactor/#52-movie-service

Commit:

feat(movie): 영화 검색 기능 구현 (#12)
fix(cache): 랭킹 캐시 만료 처리 수정 (#34)

## Working Rules
코드를 수정하기 전에 기존 구현과 패턴을 먼저 확인한다.
새 라이브러리를 추가하기 전에 기존 의존성으로 해결 가능한지 확인한다.
대규모 아키텍처 변경을 임의로 수행하지 않는다.
요구사항이 불분명한 경우 중요한 설계 결정을 임의로 내리지 않는다.