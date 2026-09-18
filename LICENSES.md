# LICENSES.md — 에셋 라이선스 정리

이 프로젝트가 사용하는 외부 에셋의 출처·라이선스 요약. 원문은 `CREDITS-brackeys.txt`, `CREDITS-manaseed.txt` 참고.

## 요약표

| 에셋 | 사용처 | 제작자 | 라이선스 |
|---|---|---|---|
| Brackeys Platformer Assets | 타일·발판·슬라임·코인·사운드·음악·폰트 | Brackeys 외 (아래 상세) | **CC0** (퍼블릭 도메인) |
| Mana Seed Character Base **Demo** 2.0 | 플레이어 캐릭터 (몸+옷+머리) | Seliel the Shaper | **데모 전용 라이선스** — 상업/비상업 사용 허용 |
| 절차 생성 그래픽 (가시, 하트, 픽셀) | 함정·HP UI | 이 프로젝트 코드(`Assets.java`)에서 생성 | 프로젝트와 동일 |

## 1. Brackeys Platformer Assets — CC0

팩 전체가 **Creative Commons Zero (CC0)**. 출처 표기 의무 없음(하지만 아래 크레딧 유지 권장). 이 프로젝트에서 사용 중인 파일과 원제작자:

- `sprites/platforms.png`, `sprites/coin.png` — analogStudios_ (Four Seasons Platformer Sprites)
- `sprites/slime_green.png`, `sprites/slime_purple.png` — analogStudios_ (Dungeon Sprites)
- `sprites/world_tileset.png` — RottingPixels (Four Seasons Platformer Tileset)
- `audio/*.wav` (coin, hurt, jump, tap, power_up) — Brackeys, Asbjørn Thirslund
- `audio/time_for_adventure.mp3` — Brackeys, Sofia Thirslund
- `fonts/PixelOperator8.ttf` — Jayvee Enaguas (HarvettFox96), Pixel Operator
  (https://www.dafont.com/pixel-operator.font)

## 2. Mana Seed Character Base Demo 2.0 — 데모 라이선스

- 제작자: **Seliel the Shaper** — https://seliel-the-shaper.itch.io/
- 사용 파일: `char/base.png`(몸, page 1), `char/outfit.png`(farmer 옷), `char/hair.png`(bob 머리)
- 원문 조건: *"You may use this demo asset commercially or non-commercially."*
  → **데모 범위 안에서는 상업적 사용도 허용**됨.
- 참고사항:
  - 정식(유료) 버전은 애니메이션 페이지가 훨씬 많음: https://seliel-the-shaper.itch.io/character-base
  - 제작자 권장: 포함된 옷/머리는 placeholder 용도이며, 고유한 게임을 원하면 몸 스프라이트 위에 자체 의상을 그리는 것을 추천.

## 3. 절차 생성 에셋

가시 타일, 하트 아이콘, 1px 화이트 텍스처는 외부 에셋이 아니라 `core/.../Assets.java`에서 Pixmap으로 런타임 생성한다. 별도 라이선스 제약 없음.

## 4. 기타

- 게임 프레임워크 **libGDX** — Apache License 2.0
- `lwjgl3/src/main/resources/libgdx*.png`, `assets/libgdx.png` — gdx-liftoff 템플릿 기본 창 아이콘(libGDX 로고). 배포 전 자체 아이콘으로 교체 권장.
