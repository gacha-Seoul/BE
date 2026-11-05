# 서울 가챠 BE

가챠로 서울의 구를 뽑고 AI NPC와 한국어 상황 회화를 하며 퀘스트를 깨는 서비스의 백엔드.

Spring Boot 3.3 / Java 17 / Spring Data JPA / MySQL 8.4. Lombok과 H2는 사용하지 않는다.

- [API 명세](docs/API.md)
- [DB 구조와 무결성 규칙](docs/ERD.md)
- 실행 후 Swagger: http://localhost:8080/swagger-ui.html

## 실행

Java 17과 Docker Desktop이 필요하다. Docker Desktop을 먼저 실행한다.
프로젝트는 Java 17 toolchain을 사용하므로 Java 21/26만 설치되어 있으면 실행할 수 없다.
macOS에서 Java 17이 없다면 설치한다.

```bash
brew install --cask temurin@17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

`/usr/libexec/java_home -v 17`로 설치를 확인한다. IntelliJ의 Gradle JVM도 Java 17로 선택한다.

```bash
cp .env.example .env
```

`.env`에서 `DB_PASSWORD`와 `MYSQL_ROOT_PASSWORD`를 설정한다. `.env`는 Git에 포함되지 않는다.
기존 `.env`가 있다면 덮어쓰지 말고 `.env.example`의 DB 항목을 추가한다.

```bash
docker compose up -d --wait
./gradlew bootRun
```

IntelliJ에서도 Gradle JVM과 Project SDK를 Java 17로 맞추고 `SeoulGachaApplication`을 실행할 수 있다.
`GET http://localhost:8080/api/health`로 연결을 확인한다.
로컬 MySQL이 이미 있으면 Docker 대신 `.env`의 `DB_URL / DB_USERNAME / DB_PASSWORD`를 설정한다.
DB `seoul_gacha`와 계정을 미리 만들고, 계정에 해당 DB의 테이블 생성·변경·읽기·쓰기 권한을 부여해야 한다.

## 데이터 관리

- JPA 엔티티가 테이블과 관계를 정의한다. 개발 환경에서는 `ddl-auto: update`로 스키마를 반영한다.
- `data.sql`은 콘텐츠 초기 데이터 전용이다. 기존 ID를 건너뛰므로 재시작해도 중복 삽입하지 않는다.
- 지역·장소·퀘스트 추가는 `data.sql`에 INSERT를 추가한다. 기존 ID의 콘텐츠를 수정하려면 해당 DB 행도 갱신해야 한다.
- MySQL 데이터는 Docker의 `mysql_data` 볼륨에 저장된다. 앱 재시작과 `docker compose down` 후에도 유지된다.
- 기존 H2 메모리 데이터는 이전하지 않는다. 새 MySQL DB에서 시작한다.
- `POST /api/demo/reset`은 사용자 기록을 초기화하고 지역·장소·퀘스트 콘텐츠는 유지한다.

현재 초기 데이터는 서울 25개 구, 장소 10개, 퀘스트 13개다.
전통시장(장소 5)은 문화 분야이며 501~504 퀘스트를 순서대로 진행한다. 나머지 장소는 기존 미션을 단일 퀘스트로 유지한다.
명세의 추가 장소는 콘텐츠가 확정되면 등록하면 된다. 진행률과 전체 개수는 데이터로 계산한다.

## AI 설정

- `LLM_MOCK=true`: 키 없는 연동용 고정 시나리오. 사용자 발화 의미와 무관하게 정해진 순서로 진행한다.
- `LLM_MOCK=false`와 `.env`의 `LLM_API_KEY`: 실제 AI 대화.
- NPC 규칙·표현 카드·기본 응답·mock 대사는 `src/main/resources/prompts/*.txt`에서 수정한다.
- LLM은 의도·미션 달성·주문 정보 변경을 제안한다. 서버가 순서·허용 필드·완료 조건을 확인하고 코인을 지급한다.

## 게임 규칙

| 항목 | 기본값 |
|---|---|
| 최초 무료 코인 | 1 |
| 가챠 | 1코인 |
| 퀘스트 최초 완료 | 퀘스트의 `reward_coin` (기본 1) |
| 지역의 모든 퀘스트 완료 | 정복 보너스 +2 |
| 첫 가챠 | 서대문구 |

가입 시 코인 1개를 한 번 지급하고, 첫 뽑기를 포함해 가챠마다 1개를 사용한다. 별도의 무료 뽑기 횟수는 없다.

가챠 요청은 `requestId`가 필수다. 같은 요청을 재시도할 때 같은 ID를 사용하면 다시 차감하지 않는다.
진행·코인 변경은 사용자 행 잠금과 DB 유일 제약으로 보호하며, 완료·보상·잔액을 같은 트랜잭션에 저장한다.

## 검증

```bash
./gradlew test
```

Testcontainers가 별도의 MySQL 8.4 컨테이너에서 JPA·시드·퀘스트 상태·보상·뽑기 재시도·동시 요청·초기화를 검증한다.
Docker가 실행 중이어야 하며 기존 개발 DB는 사용하지 않는다.

앱이 mock 모드로 실행 중이면 다음으로 실제 HTTP 흐름도 확인할 수 있다.

```bash
./scripts/demo-check.sh
```
