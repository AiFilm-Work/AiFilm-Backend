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

main 클래스(`AiFilmApplication`)와 실행 설정(`application*.yml`)만 둔다. 설정 파일 구성은 Runtime Configuration을 참고한다.

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
- 실행 진입점(main 클래스)은 `app`에만 둔다. `api`·`worker`에는 main 클래스와 `application*.yml`을 두지 않는다.
- JPA 스캔 같은 영역별 설정은 메인 클래스가 아니라 해당 모듈의 `@Configuration` 클래스에 둔다. 메인 클래스에 두면 슬라이스 테스트에도 강제로 적용된다.
- API 요청을 처리하는 서비스는 `api`의 기능 패키지(예: `com.aifilm.api.home`)에 두고, 요청·응답 DTO는 그 아래 `dto`에 둔다.
- 비즈니스 판단에 쓰는 현재 시각(예: 배너 노출 기간)은 `LocalDateTime.now()` 대신 `Clock` 빈을 주입받아 `LocalDateTime.now(clock)`으로 구한다. 테스트에서 시각을 고정하기 위해서다. `Clock` 빈은 API와 Worker가 함께 쓰므로 `infra`의 `ClockConfig`에 있다.

Entity / Repository:

- 엔티티, 상태 enum, 리포지토리 인터페이스는 `domain`의 `com.aifilm.domain.<도메인>`에 둔다. (예: `Movie`, `ScreeningStatus`, `MovieRepository`)
- `domain`은 JPA 애너테이션 스펙(`jakarta.persistence-api`)에만 의존한다. Hibernate 전용 애너테이션(`@DynamicUpdate`, `@JdbcTypeCode` 등)과 Spring Data 타입은 `domain`에서 쓰지 않는다.
- Spring Data 인터페이스(`Jpa<엔티티>Repository`)와 domain 리포지토리 구현(`<엔티티>RepositoryImpl`, `@Repository`)은 `infra`의 `com.aifilm.infra.mysql.<도메인>`에 둔다. 서비스는 domain 리포지토리 인터페이스만 쓴다.
- 다른 애그리거트는 엔티티 연관관계 대신 ID로 참조한다. (예: `Movie.genreIds`는 `movie_genre`를 `@ElementCollection`으로 매핑) 다른 애그리거트 정보가 필요한 조회는 조회 쿼리에서 조인한다.
- 엔티티는 기본 생성자를 `protected`로 두고, 생성자(또는 빌더)에서 필수 값과 생성 규칙을 검증한다. 상태·집계 값의 시작값은 호출자가 아니라 엔티티가 정한다.
- `created_at`, `updated_at`은 엔티티의 `@PrePersist`, `@PreUpdate`에서 채운다.
- `TEXT` 컬럼은 `@Column(columnDefinition = "TEXT")`를 붙인다. 없으면 `ddl-auto=validate`가 `VARCHAR`를 기대해 실패한다.

영화와 영상 출처:

- `Movie`는 출처와 무관한 공통 정보(제목, 썸네일, 러닝타임, 노출 상태, 재생 가능 여부, 집계값)만 갖는다. 영상 출처는 출처별 엔티티(`YoutubeMovieSource`, 이후 `UploadedMovieSource`)가 따로 가진다. 한 영화에 출처가 여러 종류 붙을 수 있어 상속이 아니라 조합으로 매핑하고, `movie`에 출처 종류 컬럼을 두지 않는다.
- 출처 테이블은 `movie`와 PK를 공유한다(`movie_id = movie.id`, 출처 종류마다 영화당 최대 하나). 출처 엔티티는 `Movie` 연관관계 없이 `movieId`만 가지며, 영화와의 무결성은 DB FK가 지킨다.
- PK를 직접 넣는 엔티티(출처 엔티티)는 Spring Data `save()`로 저장하지 않는다. ID가 채워져 있으면 `merge`로 동작해 같은 PK의 기존 행을 오류 없이 덮어쓴다. `<엔티티>RepositoryImpl`에서 `EntityManager.persist`로 새 행만 넣고, 중복이면 PK 위반으로 실패하게 한다.
- 영화와 출처는 한 트랜잭션에서 함께 저장한다. 영화의 재생 가능 여부(`availability_status`)는 출처들의 상태를 종합해 서비스가 갱신한다.

조회 모델:

- 화면에 보여주는 조회는 엔티티를 꺼내 조립하지 않는다. 화면에 필요한 값만 담은 조회 모델(record)과 조회 메서드를 `domain`에 두고, `infra`에서 JPQL 생성자 표현식(`select new ...`)이나 네이티브 쿼리로 필요한 컬럼만 가져온다. (예: `MovieCard`, `VisibleHomeBanner`, `MovieGenre`) 엔티티를 넘기면 지연 로딩 때문에 항목마다 쿼리가 늘어나거나(N+1), 트랜잭션 밖에서 `LazyInitializationException`이 난다.
- 목록 항목의 연관 데이터(예: 영화별 장르)는 항목마다 조회하지 않는다. ID를 모아 `IN` 한 번으로 가져와 메모리에서 나눈다. 넘길 ID가 없으면 쿼리 없이 빈 목록을 반환한다. (`IN ()`은 SQL 문법 오류)
- 개수 제한은 Spring Data의 `Limit`으로 넘긴다(SQL `LIMIT`이 된다). 더보기 여부는 한 개 더 조회해 판단하고 `COUNT` 쿼리를 따로 두지 않는다.
- 사용자에게 보여주는 영화 조회는 노출 조건(`screening_status = ACTIVE`, `availability_status = AVAILABLE`)을 쿼리에서 건다. 노출 여부는 출처 테이블이 아니라 `movie`의 두 컬럼으로만 판단한다. 조건 값은 리포지토리 구현에서 넘기고, domain 메서드 이름에는 노출 가능한 영화만 조회한다는 뜻을 드러낸다. (예: `findLatestScreenable`)
- 목록·홈 섹션·랭킹은 `movie`만 읽고 출처 테이블을 조인하지 않는다. 출처 값이 필요한 조회(상세, 배너)만 `movie` 기준으로 출처 테이블을 LEFT JOIN한다. INNER JOIN을 쓰면 그 출처가 없는 영화가 결과에서 빠진다.
- 여러 쿼리로 한 화면을 만드는 서비스는 `@Transactional(readOnly = true)` 하나로 묶어 같은 시점의 데이터를 읽게 한다.
- 단건 조회에서 "없음"과 "볼 수 없음"을 다르게 응답해야 하면(예: 영화 상세) 노출 조건을 쿼리에 걸지 않고 노출 상태를 함께 조회해, domain 조회 모델의 판단 메서드(예: `MovieDetail.isScreenable()`)로 구분한다. 목록 조회는 지금처럼 쿼리에서 거른다.

## API Response

모든 API 응답은 `api` 모듈의 `ApiResponse`로 감싼다. Actuator(`/actuator/health`)는 예외다. (Runtime Configuration 참고)

```
성공: {"status":200,"code":"SUCCESS","message":"요청에 성공했습니다.","data":{...}}
실패: {"status":404,"code":"<에러 코드>","message":"<에러 메시지>","data":null}
검증 실패: {"status":400,"code":"COMMON_400_1","message":"입력값이 올바르지 않습니다.","data":null,"errors":[{"field":"name","reason":"<사유>"}]}
```

- 성공은 `ApiResponse.success(data)`로 만든다. 실패 응답은 컨트롤러에서 직접 만들지 않고 예외를 던진다. (Exception Handling 참고)
- 성공 응답의 `code`는 항상 `SUCCESS`다. 실패 응답의 `data`는 생략하지 않고 `null`로 내려간다.
- `data`는 성공 결과만 담는다. 입력값 검증 실패의 필드별 사유는 `errors`(`field`, `reason`)에 담고, `errors`는 값이 없으면 응답에서 빠진다.
- 응답 본문의 `status`와 실제 HTTP 상태 코드를 일치시킨다.
- `data`에는 Entity가 아니라 응답 DTO를 담는다.
- 응답의 날짜·시각은 서버 시간대(KST)의 `LocalDateTime`을 오프셋 없이 초까지 내린다. DTO 필드에 `@JsonFormat(pattern = ApiDateTimeFormat.PATTERN)`(`yyyy-MM-dd'T'HH:mm:ss`)을 붙인다. DB는 마이크로초까지 저장해서, 붙이지 않으면 값마다 소수점 아래 자릿수가 달라진다.
- 아직 채울 수 없는 응답 필드(예: 로그인 전의 영화 상세 `viewer`)는 항상 null로 미리 넣지 않고 기능이 생길 때 추가한다. 필드 추가는 기존 클라이언트를 깨지 않는다.
- 실패 사유는 `common`의 `ErrorReason`을 구현한 에러 코드 enum으로 표현하고, enum은 도메인별로 나눈다.
- `ErrorReason`의 HTTP 상태는 `int`로 둔다. `common`은 스프링에 의존하지 않으므로 `HttpStatus`를 쓰지 않는다.
- 응답 필드 순서(`status`, `code`, `message`, `data`, `errors`)는 `@JsonPropertyOrder`로 고정되어 있다. 필드나 순서를 바꾸면 `ApiResponseSerializationTest`도 함께 수정한다.

## Exception Handling

- 실패는 `common`의 `BusinessException`에 `ErrorReason`을 담아 던진다. 컨트롤러에서 `ResponseEntity`로 실패 응답을 직접 만들지 않는다.
- 외부 API, DB 예외를 감쌀 때는 `BusinessException(errorReason, cause)`로 원인 예외를 넘긴다.
- HTTP 응답 변환은 `api`의 `GlobalExceptionHandler`에서만 한다. `ResponseEntityExceptionHandler`를 상속하므로 스프링 MVC 예외는 따로 `@ExceptionHandler`를 만들지 않고 해당 `handleXxx`를 오버라이드한다.
- 에러 코드는 `{도메인}_{HTTP상태}_{순번}` 형식이다. (예: `MOVIE_404_1`) 도메인에 속하지 않는 오류는 `CommonErrorCode`(`COMMON_*`)를 쓴다.
- 도메인 에러 코드 enum은 `domain`의 해당 도메인 패키지에 둔다. (예: `com.aifilm.domain.movie.MovieErrorCode`) `domain`은 `common`에 의존하므로 `ErrorReason`을 구현할 수 있고, API와 Worker가 함께 쓴다.
- 에러 코드 enum의 HTTP 상태는 `HttpStatusCodes` 상수를 쓴다. 없는 상태는 상수부터 추가한다.
- 입력값 검증은 컨트롤러의 `@Valid`와 파라미터 제약 어노테이션으로만 한다. 서비스에 `@Validated`를 붙이지 않는다. (`ConstraintViolationException`은 처리하지 않으므로 500이 된다)
- 응답에 예외 메시지, 스택트레이스, 사용자가 보낸 값을 담지 않는다.
- 로그: 4xx는 `warn` 한 줄, 5xx는 `error`와 스택트레이스.
- 새 예외 유형을 처리하면 `GlobalExceptionHandlerTest`에 HTTP 상태와 응답 본문 검증을 추가한다.
- 검증 실패 케이스를 문서화할 때는 `errors[].field`, `errors[].reason`도 응답 필드로 문서화한다.

## API Documentation

API 문서는 Controller 테스트(Spring REST Docs + restdocs-api-spec)로 만들고 Swagger UI로 본다.

```
@ControllerTest 문서화 테스트 → build/generated-snippets
→ openapi3 태스크 → build/api-spec/openapi3.yaml
→ api jar의 static/swagger-ui/openapi3.yaml → /swagger-ui.html
```

- API를 추가하거나 바꾸면 `@ControllerTest` 문서화 테스트를 함께 작성한다. 성공 케이스와 실패 케이스(4xx)를 각각 문서화한다. 입력값도 인증도 없어 4xx가 생기지 않는 API는 성공 케이스만 문서화하고, 그 이유를 테스트 주석에 적는다.
- Controller와 DTO에 Swagger(springdoc) 어노테이션을 쓰지 않는다. 문서 내용은 테스트의 `document()`에만 적는다.
- 응답 필드는 모두 문서화한다. 문서와 실제 응답이 다르면 테스트가 실패하며, 이를 피하려고 `relaxedResponseFields`를 쓰지 않는다.
- 같은 method + path의 성공·실패 테스트에는 같은 `tag`, `summary`, `description`을 준다. 스펙에서 하나의 API 아래 응답 코드별로 합쳐진다.
- `document()` 식별자는 영문 kebab-case `{리소스}-{동작}[-{실패 상황}]`로 짓고 겹치지 않게 한다. (예: `home-get`, `movie-get-not-found`)
- null이 올 수 있는 필드는 타입을 지정하고 `optional()`을 붙인다. (예: `fieldWithPath("data.sections[].movies[].rankPosition").type(JsonFieldType.NUMBER).optional()`) 타입을 지정하지 않으면 예시 값이 모두 null일 때 스펙에 null 타입으로 나간다.
- 문서화 테스트의 예시 데이터는 배열마다 항목을 하나 이상 넣는다. 배열이 비면 그 안의 필드를 응답에서 찾지 못해 문서화가 실패한다.
- 실패 응답은 `status`, `code`, `message`와 `data`(`JsonFieldType.NULL`)를 문서화한다. 경로 변수는 `pathParameters(parameterWithName(...))`로 문서화하고, 숫자면 `.type(SimpleType.INTEGER)`를 붙인다(지정하지 않으면 스펙에 string으로 나간다). 같은 API의 테스트끼리 태그·요약·설명·경로 변수를 공유하는 빌더 메서드(`ResourceSnippetParametersBuilder`)로 묶는다. (예: `MovieControllerTest`)

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
- Swagger UI는 배포 서버에서도 연다. OpenAPI `servers`는 상대 경로 `/`라서 Swagger UI가 스펙을 받은 서버로 요청을 보낸다. 서버 주소를 `build.gradle`에 따로 적지 않는다.

Swagger UI 정적 파일(`api/src/main/resources/static/swagger-ui/`):

- swagger-ui dist(v5.33.1) 원본이다. 직접 고치지 않는다.
- 원본에서 바꾼 곳은 `swagger-ui.html`(`index.html` 이름 변경, 리소스를 `/static/swagger-ui/` 절대 경로로)과 `swagger-initializer.js`의 `url` 두 군데다. 버전을 올릴 때는 dist를 통째로 바꾼 뒤 이 두 곳만 다시 고친다.
- 정적 파일은 `SwaggerUiConfig`에서 `Cache-Control: no-cache`와 파일 내용으로 만든 ETag로 내려 보낸다. jar 안 파일의 시각은 빌드할 때 1980-02-01로 고정되어(Gradle 9 기본값) `Last-Modified`로는 배포 전후를 구분할 수 없으므로 쓰지 않는다. 경로를 추가하거나 바꿀 때도 이 설정을 함께 건다.

## Code Convention

- 주석은 짧은 일반 주석(`//`)으로 "무엇을, 왜"의 핵심만 적는다. Javadoc(`/** */`)과 HTML 태그는 쓰지 않는다.
- 자세한 배경과 결정 이유는 커밋 본문에 남긴다.

## Database

- MySQL을 사용한다.
- 스키마 변경은 Flyway migration으로만 관리한다. `ddl-auto`는 모든 환경에서 `validate`다.
- `ddl-auto=create/update`는 사용하지 않는다.

Migration:

- 위치: `infra/src/main/resources/db/migration/`
- 파일 이름: `V{번호}__{설명}.sql` (예: `V1__create_movie.sql`, `V2__add_movie_runtime.sql`)
  - 번호는 1부터 차례로 올린다. 설명은 영문 소문자 snake_case로 쓴다.
- 테이블은 ERD 전체를 한 번에 만들지 않는다. 기능을 개발할 때 필요한 테이블을 마이그레이션으로 추가하고, 엔티티·리포지토리 테스트와 같은 PR에 넣는다.
- ERD(2026-09-28 스냅샷)는 출발점이다. 옮기면서 바꾼 점(컬럼 이름·타입, 빠진 제약, 아직 없는 테이블 참조)은 migration 파일 위 주석에 남긴다.
- main에 머지되기 전, 로컬에만 적용된 migration은 고쳐도 된다. 고친 뒤 `docker compose down -v`로 로컬 DB를 초기화한다.
- main에 머지된 migration은 CD 배포 때 운영 RDS에 적용되므로 수정하지 않는다. 스키마 변경은 새 migration 파일로 추가한다.
- 제약과 인덱스에는 이름을 붙인다: `pk_<테이블>`, `uk_<테이블>_<컬럼>`, `fk_<테이블>_<참조 테이블>`, `idx_<테이블>_<컬럼>`, `ck_<테이블>_<컬럼>`. 컬럼이 여러 개면 컬럼 이름을 줄여 이어 붙이고 64자를 넘지 않게 한다. (예: `idx_movie_screening_availability_approved_at`)
- 테이블은 `ENGINE = InnoDB`, `utf8mb4`, `utf8mb4_unicode_ci`를 명시한다.
- PK는 `BIGINT AUTO_INCREMENT`이고 엔티티는 `GenerationType.IDENTITY`를 쓴다. `movie`와 PK를 공유하는 출처 테이블은 예외다(AUTO_INCREMENT 없음, 엔티티에 `@GeneratedValue` 없음).
- 같은 데이터가 두 번 들어가면 안 되는 컬럼(예: `youtube_movie_source.youtube_video_id`)은 애플리케이션 검사와 별개로 DB UNIQUE로 막는다.
- 기준 데이터(예: 장르)는 클라이언트가 ID로 참조하므로 migration에서 ID를 지정해 넣는다. 추가할 때는 기존 파일을 고치지 않고 새 migration으로 다음 ID를 넣는다.
- 운영 데이터는 migration에 넣지 않는다(ID를 지정하는 기준 데이터는 예외). 관리자 API가 생기기 전에는 운영 데이터를 Session Manager에서 SQL로 넣으므로, 엔티티 검증과 같은 규칙을 DB의 `CHECK`, `UNIQUE`, `FK`로도 건다. 리포지토리 테스트는 비즈니스 규칙인 제약(예: 같은 영화에 출처 중복, 같은 영상의 중복 등록)을 확인하고, 제약마다 하나씩 테스트를 두지는 않는다.
- 한 migration 파일에는 하나의 목적만 담는다. MySQL DDL은 트랜잭션으로 묶이지 않아서, 중간에 실패하면 일부만 적용된 채 `flyway_schema_history`에 실패 기록이 남고 이전 버전 앱도 뜨지 못한다. 이때는 남은 변경을 손으로 정리하고 `flyway repair`로 기록을 지운다.

인덱스:

- 인덱스는 실행 계획 근거가 있을 때 migration으로 추가한다. 로컬 DB에 대량 데이터(예: 10만 행)를 넣고 `EXPLAIN ANALYZE`로 추가 전후를 비교하며, 수치는 커밋 본문에 남긴다. 판단은 `EXPLAIN`의 추정치(`rows`, `filtered`)가 아니라 `EXPLAIN ANALYZE`의 실제 읽은 행과 시간으로 한다.
- 정렬 + `LIMIT` 조회의 복합 인덱스는 `=` 조건 컬럼, `ORDER BY` 컬럼 순서로 만든다. 그러면 정렬된 순서대로 읽다가 `LIMIT`에서 멈춘다(`Using filesort`가 사라진다). 정렬 방향이 모두 같으면 오름차순 인덱스를 거꾸로 읽는다(`Backward index scan`).
- `ORDER BY` 마지막의 `id`는 인덱스에 넣지 않는다. InnoDB는 보조 인덱스 끝에 PK를 붙여 정렬에도 쓴다.
- 인덱스를 추가하면 `FlywayMigrationTest`에서 이름과 컬럼 순서를 `information_schema.statistics`로 확인한다. 컬럼 순서가 바뀌면 정렬에 쓰지 못한다.
- 측정용으로 로컬에 임시 인덱스를 만들었다면, 같은 이름의 migration을 적용하기 전에 지운다. 남아 있으면 앱이 `Duplicate key name`으로 뜨지 않는다.

롤백을 고려한 스키마 변경:

- CD 롤백(Run workflow)은 이전 이미지만 다시 띄우고 DB는 되돌리지 않는다. 그래서 새 스키마에서 이전 버전 앱이 `ddl-auto=validate`를 통과하고 동작해야 한다.
- 새 테이블, NULL 허용 컬럼, `DEFAULT`가 있는 NOT NULL 컬럼 추가는 한 번에 해도 된다. 이전 버전 앱은 모르는 테이블·컬럼을 무시하고, `DEFAULT`가 있으면 그 컬럼 없이 INSERT해도 실패하지 않는다.
- 이전 버전 앱도 Flyway를 실행한다. 이전 앱이 모르는 새 migration은 Flyway 기본 설정(`ignoreMigrationPatterns = *:future`)으로 검증에서 건너뛴다.
- 컬럼 삭제·이름 변경·타입 축소는 한 번에 하지 않고 배포를 나눈다.
  1. 새 컬럼을 추가하고 앱이 두 컬럼에 모두 쓰게 한다.
  2. 기존 데이터를 옮기고 앱이 새 컬럼만 읽게 한다.
  3. 롤백할 일이 없다고 판단한 다음 배포에서 기존 컬럼을 지운다.
- 영화 출처 분리(#39)의 전환 기간: `movie`의 옛 YouTube 컬럼(`youtube_video_id` 등)과 `youtube_availability_status`를 지우기 전까지, 운영 SQL로 영화를 넣거나 상태를 바꿀 때 `movie`의 옛 YouTube 컬럼과 `youtube_movie_source`를, `youtube_availability_status`와 `availability_status`를 함께 쓴다. 롤백된 이전 앱이 옛 컬럼을 읽기 때문이다. 앱 코드는 `youtube_movie_source`와 `availability_status`만 읽고 쓴다.
- 전환 기간에 롤백했다가 다시 배포할 때도 `SELECT COUNT(*) FROM movie WHERE availability_status <> youtube_availability_status;`가 0건인지 확인한다. 불일치가 있으면 롤백 중에 옛 컬럼만 바뀐 것이므로 `youtube_availability_status` 기준으로 `availability_status`를 맞춘다.

로컬 샘플 데이터:

- 로컬 개발용 샘플은 `infra/src/main/resources/db/sample/`의 반복 실행 migration(`R__*.sql`)에 둔다. `application-local.yml`의 `spring.flyway.locations`에만 이 경로가 있어서 운영 DB에는 들어가지 않는다.
- `R__` 파일은 내용이 바뀌면 다시 실행되므로 몇 번 실행해도 결과가 같게 쓴다. 행은 `ON DUPLICATE KEY UPDATE`로 덮어쓰고, 연결 테이블처럼 빠진 항목이 남으면 안 되는 데이터는 샘플 범위만 지운 뒤 다시 넣는다.
- 샘플 YouTube 영상 ID는 `local-sample-*` 자리표시자라 실제로 재생되지 않는다.
- `@IntegrationTest`는 `spring.flyway.locations`를 `db/migration`으로 덮어써 테스트 DB에 샘플을 넣지 않는다.
- 터미널에서 로컬 DB를 조회할 때는 `docker compose exec mysql mysql -uaifilm -p --default-character-set=utf8mb4 aifilm`으로 접속한다. 문자셋을 지정하지 않으면 한글이 `?`로 보인다(저장된 값은 정상).

## Runtime Configuration

설정 파일은 `app/src/main/resources/`에 둔다.

| 파일 | 내용 |
| --- | --- |
| `application.yml` | 모든 환경에 공통인 설정, 기본 프로파일(`local`), Actuator 노출 범위, 실행 모드 스위치 |
| `application-local.yml` | 로컬 실행용. DB 접속 정보의 로컬 기본값, 실행 SQL 로그 |
| `application-prod.yml` | 배포 서버용. DB 접속 정보를 기본값 없이 환경변수로만 받는다 |

- 프로파일을 지정하지 않으면 `local`로 뜬다. 배포 서버는 `SPRING_PROFILES_ACTIVE=prod`로 실행한다.
- 환경마다 값이 다른 설정만 프로파일 파일에 두고, 나머지는 `application.yml`에 둔다.
- DB 접속 정보는 환경변수 `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`로 받는다. 로컬 값은 `.env.example`을 참고한다.
- 비밀번호, API 키 같은 비밀 값은 설정 파일에 쓰지 않고 환경변수로만 받는다. prod 파일에는 기본값도 두지 않는다.
- `spring.jpa.open-in-view`는 `false`다. 지연 로딩은 서비스 트랜잭션 안에서 끝내고, 컨트롤러에서 엔티티를 탐색하지 않는다.

Actuator:

- Actuator는 `app` 모듈에 있어서 실행 모드(API만, Worker만)와 상관없이 동작한다.
- `health`만 노출하고 상세 정보는 숨긴다. 다른 엔드포인트를 열려면 노출 범위와 접근 제한부터 정한다.
- `/actuator/health` 응답(`{"status":"UP", ...}`)은 Docker 헬스체크와 로드밸런서가 읽는 형식이라 `ApiResponse`로 감싸지 않는다.
- Spring Boot 4.1은 liveness/readiness 그룹이 기본으로 켜져 있어 응답에 `groups`가 함께 나오고, `/actuator/health/liveness`, `/actuator/health/readiness`도 응답한다.

CORS:

- 허용 출처는 `aifilm.cors.allowed-origins`로 받는다. 로컬은 `application-local.yml`에, 운영은 서버 `.env`의 `AIFILM_CORS_ALLOWED_ORIGINS`(쉼표 구분)에 둔다. 비어 있으면 다른 출처의 브라우저 요청을 허용하지 않는다.
- 인증은 Authorization 헤더로 하므로 credentials(쿠키)는 허용하지 않는다.
- 허용하지 않은 출처의 사전 요청은 Spring이 403으로 직접 응답해 `ApiResponse` 형식이 아니다. 브라우저가 CORS 단계에서 막는 응답이라 감싸지 않는다.

프록시 헤더:

- prod 프로파일은 Nginx 뒤에서 실행되므로 `server.forward-headers-strategy=native`와 Tomcat `remoteip` 설정으로 `X-Forwarded-Proto`, `X-Forwarded-For`를 읽는다.
- `RemoteIpValve`는 실제 Tomcat에서만 동작해서 MockMvc로는 확인되지 않는다. 이 동작은 `@IntegrationTest(webEnvironment = RANDOM_PORT)`로 띄우는 `ForwardedHeadersTest`로 검증한다.

## Docker

앱 이미지는 미리 빌드한 jar로 만든다. 이미지 안에서는 Gradle을 돌리지 않는다. (Swagger 스펙이 api 테스트로 만들어지기 때문)

```bash
./gradlew :app:bootJar                       # app/build/libs/aifilm-app.jar 생성 (api 테스트와 Swagger 스펙 생성 포함)
docker compose --profile app up -d --build   # 앱 + MySQL 실행
docker compose up -d                         # MySQL만 실행 (IntelliJ로 앱을 띄울 때)
```

- `Dockerfile`은 jar를 jarmode tools로 레이어별로 풀어서 담는다. jar 이름(`aifilm-app.jar`)을 바꾸면 `Dockerfile`과 `.dockerignore`도 함께 고친다.
- `.dockerignore`는 jar만 빌드 컨텍스트로 보낸다. 이미지에 `.env`나 비밀 값을 넣지 않는다.
- 이미지의 기본 프로파일은 `prod`다. DB 접속 정보는 실행할 때 환경변수로 넘기고, compose의 `app` 서비스는 `.env` 값과 `DB_HOST=mysql`을 쓴다.
- 컨테이너 헬스체크는 `/actuator/health`다. `docker compose --profile app ps`에서 `healthy`인지 확인한다.
- `app` 서비스는 `app` 프로파일에 묶여 있어서 `ps`, `down` 같은 명령에도 `--profile app`을 붙인다.
- 앱 컨테이너와 IntelliJ 실행은 둘 다 8080 포트를 쓰므로 동시에 띄우지 않는다.
- 실행 모드는 `AIFILM_API_ENABLED`, `AIFILM_WORKER_ENABLED` 환경변수(또는 `.env`)로 바꾼다.

## Deployment

배포 서버는 EC2 한 대에서 앱 컨테이너를 띄우고, DB는 RDS(MySQL 8.4)를 쓴다. 절차는 `deploy/README.md`를 따른다.

- main에 머지되고 CI가 성공하면 CD(`.github/workflows/cd.yml`)가 CI의 jar로 이미지를 만들어 ECR에 올리고, SSM Run Command로 서버에서 `deploy/deploy.sh`를 실행한다. 손으로 배포하는 건 `deploy/README.md`의 비상 절차뿐이다.
- 이미지는 ECR `aifilm-app`에 git 커밋 해시(짧은 형식) 태그로 올린다. 저장소는 태그 변경 불가로 설정되어 있어 같은 태그를 다시 쓰지 않는다.
- `deploy.sh`는 새 이미지를 먼저 받은 뒤에 서버 `.env`를 바꾼다. 받지 못하면 서버를 건드리지 않고, 컨테이너를 띄우지 못하거나 healthy가 되지 않으면 이전 이미지로 되돌린 뒤 CD를 실패로 끝낸다.
- `deploy.sh`를 고치면 정상 배포뿐 아니라 이미지 받기 실패, 컨테이너 기동 실패, healthy 실패 때 서버가 이전 상태로 돌아오는지도 확인한다.
- `deploy.sh`의 단계마다 시간 제한을 두고, 제한을 모두 더한 값(지금 최악 750초)은 SSM 실행 제한(900초)보다 작게 둔다. 단계를 추가하거나 제한을 늘리면 합을 다시 계산한다.
- 서버 설정은 `deploy/compose.yml`이 기준이다. 서버의 파일을 직접 고치지 않고 저장소에서 고친 뒤 옮긴다.
- 서버의 `.env`(이미지 주소, DB 접속 정보)는 서버에만 둔다. 예시는 `deploy/.env.example`이고 실제 값은 git에 올리지 않는다.
- 앱은 RDS 마스터 계정이 아니라 앱 전용 사용자(`aifilm`)로 접속한다.
- RDS는 퍼블릭 접근을 끄고, 3306은 EC2 보안 그룹에서만 연다. 서버 접속은 Session Manager를 쓰고 SSH(22)는 열지 않는다.
- 외부 요청은 `https://api.aifilm.life`로 받는다. 서버의 Nginx가 80을 HTTPS로 넘기고, 443에서 TLS를 끝낸 뒤 앱(`127.0.0.1:8080`)으로 프록시한다. 보안 그룹은 80, 443만 연다.
- Nginx 설정은 `deploy/nginx/aifilm.conf`가 기준이다. 인증서는 certbot이 자동 갱신하고, 갱신 후 Nginx를 reload한다.
- 앱 포트는 `127.0.0.1`에만 연다. prod 프로파일은 Tomcat이 사설·루프백 대역에서 온 `X-Forwarded-*`를 믿고 읽으므로, 8080을 밖에 열면 클라이언트가 보낸 헤더가 그대로 반영될 수 있다.
- 되돌릴 때는 Actions의 CD를 Run workflow로 실행하고 `image_tag`에 이전 태그를 넣는다. 서버 `.env`를 직접 고치는 건 비상 절차다.
- 배포는 concurrency(`queue: max`)로 한 번에 하나씩, 대기를 시작한 순서대로 실행한다. 대기열은 최대 100개이고 넘치면 새 실행이 취소된다. 서버에서도 `deploy.sh`가 잠금을 걸어 겹쳐 실행되지 않는다.
- 자동 배포는 서버에 반영하기 직전에 대상 커밋이 main의 최신 커밋인지 확인하고, 아니면 건너뛴다. 최신 커밋은 그 커밋의 CI가 성공해야 배포된다. 수동 배포(롤백)는 이 검사를 하지 않는다.
- CD는 SSM 전달 제한과 실행 제한을 합친 시간보다 오래 기다린다. 두 제한은 `cd.yml`의 `delivery_timeout`, `execution_timeout`에서 고치고, `deploy.sh`의 시간 제한 합이 실행 제한을 넘지 않게 함께 맞춘다.
- Actions의 AWS 인증은 OIDC 배포 역할(`production` 환경 변수 `AWS_DEPLOY_ROLE_ARN`)만 쓴다. 장기 액세스 키를 저장소 secret에 넣지 않는다.
- 배포 역할의 신뢰 조건이 `production` 환경이라, 역할을 받는 job에는 `environment: production`을 붙인다. 이 환경은 main 브랜치에서만 배포할 수 있다. 신뢰 조건의 `sub`는 소유자·저장소 ID가 붙은 형식이다. (`deploy/README.md` 운영 메모 참고)
- 첫 배포 이후에는 RDS에 적용된 migration 파일을 고치지 않는다. (Database 참고)

## Testing

코드 변경 시 변경된 동작에 대한 테스트를 작성하거나 수정하고 실행한다.

사용 도구:

- JUnit 5
- AssertJ
- Mockito
- Testcontainers (MySQL)

기본 원칙:

- 비즈니스 로직 변경 시 관련 테스트를 함께 작성한다.
- 버그 수정 시 회귀 테스트를 추가한다.
- 테스트 대상 객체 자체를 mock으로 만들지 않는다.
- 외부 의존성만 필요한 범위에서 mock 처리한다.
- 테스트를 통과시키기 위해 assertion을 약화하거나 테스트를 삭제하지 않는다.
- 외부 운영 API를 실제로 호출하는 테스트는 기본 테스트에 포함하지 않는다.
- 테스트는 판단이 있는 코드(분기, 경계값, 여러 방법 중 고른 구현, 틀려도 조용히 넘어가는 동작)에 쓴다. getter, Lombok이 만든 코드, Spring Data 기본 메서드, DB가 당연히 보장하는 동작은 따로 테스트하지 않는다.

테스트 작성 형식:

- 테스트 메서드 이름은 확인하는 규칙을 한글 문장으로 쓰고(단어는 `_`로 잇는다), `@DisplayName`은 쓰지 않는다. 성공 경우는 `~할_수_있다`, 실패 경우는 `~하면_실패한다`처럼 쓴다. 고유명사는 `YouTube`, `ID`로 표기한다. (예: `이미_YouTube_출처가_있는_영화에_다시_저장하면_덮어쓰지_않고_실패한다`)
- 본문은 `// given`, `// when`, `// then`으로 나눈다. 실행과 검증이 한 문장이면(`assertThatThrownBy` 등) `// when & then`으로 쓰고, 준비할 것이 없으면 `// given`을 생략한다.
- 테스트 데이터는 실제 값처럼 만든다(영상 ID, URL 등은 영문 소문자와 하이픈). 비즈니스 판단에 쓰지 않는 시각도 `LocalDateTime.now()` 대신 고정값을 쓴다.
- 아직 이 형식이 아닌 기존 테스트는 고칠 때 함께 맞춘다.

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

- `app`의 통합 테스트에는 `@SpringBootTest` 대신 `@IntegrationTest`를 붙인다. Testcontainers가 `mysql:8.4` 컨테이너를 띄우므로 Docker만 실행 중이면 되고, 로컬 MySQL과 `.env`는 필요 없다. (`docker compose`는 앱을 로컬에서 실행할 때만 쓴다)
- `@IntegrationTest`는 프로파일을 지정하지 않아 `local`로 뜬다. 프로파일별 동작을 확인할 때만 `@ActiveProfiles`를 붙인다.
- `@IntegrationTest`의 웹 환경은 기본 `MOCK`이다. Tomcat 밸브처럼 실제 서버에서만 동작하는 기능을 확인할 때만 `@IntegrationTest(webEnvironment = RANDOM_PORT)`로 띄운다. `@SpringBootTest`를 직접 붙이지 않는다.
- 테스트 MySQL 컨테이너는 JVM 안에서 하나를 공유한다. 컨텍스트가 닫히면 컨테이너도 멈추므로 app 통합 테스트에서는 `@DirtiesContext`를 쓰지 않는다.
- `api`의 웹 계층 테스트는 테스트 전용 설정(`ApiTestApplication`)으로 `@WebMvcTest`를 사용하며, DB 없이 실행된다.
- `@WebMvcTest`(`@ControllerTest`)에는 테스트할 컨트롤러를 `controllers`로 지정한다. 지정하지 않으면 모든 컨트롤러가 올라와서, 서비스가 필요한 컨트롤러가 생기는 순간 관계없는 테스트까지 뜨지 않는다. 컨트롤러 없이 설정만 확인하는 테스트는 `useDefaultFilters = false`와 `@Import(<설정 클래스>)`를 쓴다.
- `api`의 서비스는 domain 리포지토리를 Mockito로 대신하는 단위 테스트(`@ExtendWith(MockitoExtension.class)`)로 검증한다. 현재 시각은 `Clock.fixed(...)`로 고정한다.
- `domain`의 엔티티 생성 규칙은 스프링 없이 단위 테스트한다.
- `infra`의 리포지토리 테스트에는 `@RepositoryTest`를 붙이고 테스트할 `<엔티티>RepositoryImpl`을 `@Import`한다. `@DataJpaTest`로 JPA 빈만 띄우고, Testcontainers MySQL에 Flyway migration을 적용한 뒤 `ddl-auto=validate`로 엔티티와 스키마가 같은지 검증한다. 각 테스트는 끝나면 롤백된다.
- `infra` 테스트는 테스트 전용 설정(`InfraTestApplication`)을 쓴다. 엔티티가 `domain`에 있어서 `@EntityScan("com.aifilm.domain")`으로 스캔 범위를 지정한다.
- DB 제약(UNIQUE, FK) 위반은 예외 타입이 감싸는 위치에 따라 달라지므로, 원인 예외(`SQLIntegrityConstraintViolationException`)로 확인한다. `CHECK` 위반은 원인 예외의 메시지에 제약 이름(`ck_...`)이 들어 있는지로 확인한다.
- 노출 상태·평점처럼 바꾸는 domain 메서드가 아직 없는 값은 리포지토리 테스트에서 `MovieFixture`의 SQL 헬퍼(`changeStatus`, `changeRating`)로 바꾼다.
- 데이터 양과 상관없이 쿼리 수가 같아야 하는 조회 API(홈, 목록)는 app 통합 테스트에서 SELECT 수를 검증한다. MySQL `Com_select` 카운터(`SHOW GLOBAL STATUS`)를 요청 전후로 읽어 차이를 세고, 같은 MySQL을 쓰는 다른 컨텍스트의 잡음을 피하려고 여러 번 재서 최솟값을 쓴다. 데이터가 적을 때와 많을 때의 값이 같은지, 그리고 기대한 쿼리 수인지 확인한다. (예: `HomeApiIntegrationTest`)
- app 통합 테스트에서 데이터가 필요하면 JDBC로 넣는다(app은 JPA에 컴파일 의존이 없다). 컨테이너를 다른 테스트와 함께 쓰므로 고유 접두사(예: `home-it-`)를 붙여 넣고 `@AfterEach`에서 지운다.

작업 완료 전 최소한 변경된 모듈의 테스트를 실행한다.
테스트를 실행하지 못한 경우 그 사실과 이유를 명시한다.

## CI

main을 대상으로 하는 PR과 main push마다 GitHub Actions가 `./gradlew build`를 실행한다. 설정은 `.github/workflows/ci.yml`에 있다.

- CI는 로컬의 `./gradlew build`와 같은 명령을 돌린다. PR을 올리기 전에 로컬에서 먼저 통과시킨다. (Docker 실행 필요)
- app 통합 테스트는 러너의 Docker에서 Testcontainers로 MySQL을 띄운다. CI에 DB나 `.env`를 따로 준비하지 않는다.
- CI가 실패하면 Actions 실행 화면의 `test-reports` 아티팩트에서 모듈별 테스트 리포트를 내려받아 원인을 확인한다.
- main push 빌드의 jar는 `app-jar` 아티팩트로 올라가고, CD가 이 jar로 이미지를 만든다. jar 이름이나 경로를 바꾸면 `ci.yml`, `cd.yml`, `Dockerfile`, `.dockerignore`를 함께 고친다.
- `deploy/deploy.sh`는 CI에서 빌드 전에 shellcheck로 검사한다.
- CI와 CD 러너는 `ubuntu-24.04`로 고정한다. 올릴 때는 `ci.yml`과 `cd.yml`을 함께 바꾸고 따로 확인한다.
- `gradlew`는 실행 권한(100755)으로 커밋되어 있어야 한다. Windows에서는 권한이 기록되지 않으므로, 권한이 빠지면 `git update-index --chmod=+x gradlew`로 맞춘다.

## Code Review

Draft가 아닌 PR이 열리면 CodeRabbit이 자동으로 리뷰한다. 설정은 루트의 `.coderabbit.yaml`에 있다.
Draft PR은 자동 리뷰 대상이 아니며, 필요하면 PR 코멘트에 `@coderabbitai review`를 남겨 직접 요청한다.

- CodeRabbit 리뷰는 참고 의견이다. 머지 필수 조건으로 쓰지 않으며, 반영 여부는 작성자가 판단한다.
- CodeRabbit은 이 CLAUDE.md를 리뷰 기준으로 읽는다. 리뷰 기준을 바꾸려면 이 문서의 규칙을 고친다.
- 직접 고치지 않는 파일(외부 dist, Gradle wrapper 등)은 `.coderabbit.yaml`의 `reviews.path_filters`에 `!`로 제외한다.

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