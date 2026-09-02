# DB 마이그레이션 가이드 (Flyway)

## 개요

이 프로젝트는 **Flyway**로 DB 스키마를 관리한다. `ddl-auto`는 모든 환경에서
`validate`이며, 스키마 변경은 반드시 `src/main/resources/db/migration/`의
버전드 SQL 파일로 이뤄진다. 앱 부팅 시 Flyway가 미적용 마이그레이션을 자동 실행한 뒤
Hibernate가 엔티티와 스키마를 대조(`validate`)한다.

운영 DB는 `2026-09-01` 시점 스키마를 `V1__baseline.sql`로 고정하고
`baseline-on-migrate`로 v1에 baseline 처리했다. 따라서 **운영에서 V1은 실행되지 않으며**,
빈 스키마(신규 로컬 / CI)에서만 V1부터 전체 실행된다.

## 마이그레이션 파일 규칙

| 항목 | 규칙 |
|---|---|
| 위치 | `src/main/resources/db/migration/` |
| 파일명 | `V<yyyyMMddHHmm>__<snake_case_설명>.sql` 예: `V202609021045__add_curator_applications.sql` |
| 버전 | **타임스탬프 기반.** `V2`, `V3` 같은 순번은 병렬 브랜치에서 충돌하므로 금지. `V1`은 baseline 전용 |
| 내용 | 순수 SQL만. Java 기반 마이그레이션 금지 |
| 불변성 | **머지된 파일은 절대 수정 금지** (Flyway 체크섬). 잘못됐으면 다음 버전으로 roll-forward |
| 리뷰 | 엔티티 변경과 **같은 PR**에 마이그레이션 포함 |

## 새 마이그레이션 작성 흐름 (로컬)

1. 엔티티 수정
2. `src/main/resources/db/migration/V<타임스탬프>__<설명>.sql` 작성
3. 로컬 앱 부팅 → Flyway가 자동 적용 → Hibernate `validate` 통과 확인
4. PR 생성 → CI가 빈 MySQL에 V1부터 전체 재적용 + `validate` 검증

> 로컬 DB가 `ddl-auto: update` 잔재로 엔티티와 어긋나 `validate`가 실패하면
> 로컬 스키마를 재생성한다:
> `DROP DATABASE muntum; CREATE DATABASE muntum CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;`
> → 앱 부팅 → Flyway가 V1부터 전체 적용.

## 파괴적 변경 — Expand / Contract (2회 배포)

한 번에 바꾸지 말고 배포를 쪼갠다. 롤백(이전 버전 앱) 시에도 DB가 호환되도록.

| 목표 | 1차 배포 | 2차 배포 (안정 확인 후) |
|---|---|---|
| 컬럼 이름 변경 | 새 컬럼 추가 + 데이터 백필. 코드가 새 컬럼 read/write | 기존 컬럼 `DROP` |
| 컬럼 / 테이블 삭제 | 코드에서 모든 참조 제거 후 배포 | 마이그레이션으로 `DROP` |
| `NOT NULL` 추가 | nullable로 추가 + 백필. 코드가 항상 값 기록 | `MODIFY ... NOT NULL` |
| 컬럼 타입 변경 | 새 타입 새 컬럼 추가 + 백필 | 기존 컬럼 제거 |

## 대용량 테이블 DDL

현재 전 테이블이 소형(최대 수백 행)이라 일반 `ALTER TABLE`로 충분하다.
향후 특정 테이블이 수십만 행 이상으로 커지면 그 테이블 변경 시
`pt-online-schema-change` / `gh-ost` / RDS Blue-Green Deployment를 검토한다.

## 롤백

1. **1차 (기본): roll-forward.** 변경을 되돌리는 새 마이그레이션
   (`V<타임스탬프>__revert_xxx.sql`)을 작성해 배포한다.
2. **2차 (비상): 스냅샷 복원.** 배포 파이프라인이 마이그레이션 직전 생성한
   `muntum-predeploy-*` 스냅샷을 새 RDS 인스턴스로 복원 →
   `secrets-prod.yml`의 `db.aws-rds.endpoint`를 새 엔드포인트로 교체 →
   `sudo systemctl restart muntum`. 데이터가 손상된 경우의 최후 수단이다.

> Flyway Community에는 자동 undo가 없다. `flyway repair`는 체크섬/실패 레코드
> 정리용이며 스키마를 되돌리지 않는다.

## 알려진 tech debt

- **`social_accounts` collation 불일치**: 이 테이블만 `utf8mb4_0900_ai_ci`
  (나머지 12개는 `utf8mb4_general_ci`). 운영 현실을 그대로 반영한 것.
  정규화하려면 `ALTER TABLE social_accounts CONVERT TO CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci` — `uk_social_provider_subject` 유니크 인덱스
  재생성과 문자열 비교 동작 변화를 검토한 뒤 별도 마이그레이션으로 진행.
- **`curator_applications` / `search_histories`**: 각각 `feat/curator/application`,
  검색 기능 브랜치에만 존재. 해당 브랜치가 Flyway 적용된 main을 rebase한 뒤
  자기 PR에 `V<타임스탬프>__create_xxx.sql`을 담아 반영한다. 로컬에
  `ddl-auto: update`가 만든 잔재 테이블이 있으면 마이그레이션 전에 `DROP TABLE`.

## 운영 배포 파이프라인

> Step 9(deploy.yml 재구성) 완료 후 이 섹션 채움:
> prepare(마이그레이션 요약·위험도) → 승인 → RDS 스냅샷 → 마이그레이션(앱 부팅) → health check