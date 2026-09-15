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

`api`와 `worker`는 서로 직접 의존하지 않는다.

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

## Architecture Rules

- Controller에서 Repository를 직접 호출하지 않는다.
- Controller에 비즈니스 로직을 작성하지 않는다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- API request/response는 DTO를 사용한다.
- 외부 시스템 연동 구현은 `infra`에 둔다.
- HTTP endpoint는 `api`에 둔다.
- Scheduler 및 백그라운드 실행 진입점은 `worker`에 둔다.
- Scheduler 자체에는 비즈니스 로직을 작성하지 않는다.

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
./gradlew :api:test
./gradlew :worker:test
./gradlew :domain:test
./gradlew :infra:test
```

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