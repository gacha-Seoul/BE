# 서울 가챠 BE — AI 코딩 도구용 규칙

## 프로젝트
가챠로 서울의 구를 뽑고, 구마다 5개 분야(역사·문화·예술·관광·생활자원) 스테이지에서
AI NPC와 한국어 상황 회화를 하며 미션을 깨는 외국인 대상 서비스. 해커톤 데모용.

## 기술
Spring Boot 3.3, Java 17, Spring Data JPA, MySQL 8.4, springdoc(Swagger). Lombok 사용 안 함.

## 규칙
- 패키지: controller / service / domain / repository / dto / llm / config / common
- DTO는 `dto/Dtos.java` 안의 record로 추가
- 에러는 `GameException(HttpStatus, 메시지)`로 던진다. 응답 형식 `{ "message": "..." }`
- 미션 완료·클리어·코인은 반드시 서버(ChatService)에서 판정. LLM 결과는 제안으로만 사용
- 프롬프트 문구는 `resources/prompts/*.txt`에서 수정. Java에 하드코딩하지 않는다
- 지역/스테이지 추가는 `resources/data.sql`에 INSERT만 추가
- API를 바꾸면 `docs/API.md`도 같이 수정 (3D 프론트 팀이 이 문서를 보고 연동)
- API 키는 `.env`에만. 코드나 application.yml에 키를 쓰지 않는다
