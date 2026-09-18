# AGENTS.md — AI 에이전트/개발자 작업 가이드

libGDX(gdx 1.14.2, gdx-liftoff 템플릿) 데스크톱 게임. 게임 설계는 `DESIGN.md`, 개발 이력은 `PROCESS.md` 참고.

## 빌드 / 실행

```
gradlew :core:compileJava      # 컴파일 (빠른 문법 확인)
gradlew :lwjgl3:run            # 게임 실행 (창 480x800)
gradlew :lwjgl3:jar            # 배포용 러너블 JAR (lwjgl3/build/libs/)
```

- Java 8 타깃. gradle daemon 꺼져 있어 매 빌드 JVM 기동 시간이 있음.
- `assets/`가 클래스패스 루트로 포함됨. 에셋 경로는 `assets/` 기준 상대 경로.

## 코드 구조 (core/src/main/java/io/github/some_example_name/)

| 파일 | 역할 |
|---|---|
| `Main.java` | 앱 라이프사이클 + READY/PLAY/PAUSE/OVER 상태머신, 최고기록 저장 |
| `GameWorld.java` | 시뮬레이션 전부: 플레이어 물리, 충돌, 생성(makeBar/makeFloor), 카메라, 데미지 규칙. 튜닝 상수도 여기 |
| `GameRenderer.java` | 렌더링 전부(월드+HUD+메뉴). 게임 로직 넣지 말 것 |
| `Assets.java` | 텍스처/애니/사운드/폰트 로드·소유, 절차 생성 텍스처(가시·하트) |
| `Theme.java` | 깊이→티어/색/난이도 헬퍼 (전부 static) |
| `Platform.java`, `Slime.java` | 엔티티 데이터 |

원칙: 로직 변경은 GameWorld, 그리기 변경은 GameRenderer, 리소스는 Assets에만. 서로 새 의존성을 만들지 말 것.

## 스프라이트 시트 좌표 (수동 검증됨 — 다시 파악하지 말 것)

- **Mana Seed p1** (512×512, 64px 셀): 행 순서 남/북/동/서. 행0-3 = stand(열0)/push(1-2)/pull(3-4)/jump(5-7, 6=상승 7=공중). 행4-7 = walk 열0-5. 페이퍼돌 레이어: base→outfit→hair 순서로 겹쳐 그림.
- **platforms.png** (16px 바 4행): 행 0 초록, 1 모래, 2 주황, 3 파랑. 열: 캡L/미드/미드/캡R.
- **world_tileset.png** (16px 격자): 지형 col0 잔디, col1 바위흙, col2 사암, col3 균열돌, col6 얼음, col8 어두운돌. 행0=표면 타일, 행1=속 타일. 가시(spike) 타일은 **없음** → `Assets.buildSpikeTexture()` 절차 생성.
- **slime_green/purple.png** (24px 셀 4×3): 걷기 애니는 가운데 행(행1) 4프레임.
- **coin.png**: 16×16 12프레임.

## 창 자동 테스트 (Windows)

- 캡처: `PrintWindow(hwnd, hdc, 3)` — 창이 가려져 있어도 됨. 창 핸들은 `Get-Process java | ? MainWindowTitle -eq "2D-Fall"`.
- 키 주입: `PostMessage(WM_KEYDOWN/WM_KEYUP)` 사용 (SendKeys는 포커스 문제로 실패). 화살표 키는 확장 키 플래그가 필요하므로 W/S/SPACE로 대신할 것.

## 주의사항

- 발판 충돌은 위에서만 통과 가능한 one-way. 착지 판정은 "이전 프레임 발바닥이 top 이상 && 현재 top 이하" — 이 로직 수정 시 점프(위로 통과)와 프레임당 재착지(standingOn 갱신)가 같이 깨질 수 있음.
- 클래식 생성 규칙 불변식: 바닥층엔 구멍 ≥1, 가시 패치는 세그먼트 전체를 덮지 않음(진행 불가 방지). 생성 코드를 바꿀 때 이 두 가지를 유지할 것.
- Mana Seed는 무료 데모 — 상업 배포 전 정식 라이선스 필요 (`CREDITS-manaseed.txt`).
- UI 폰트(PixelOperator8)는 라틴 전용. 한글 UI가 필요하면 freetype으로 시스템 한글 폰트를 별도 생성해야 함.
