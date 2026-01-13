# Plugin DreamWork: Architecture Plan

## "The Development Framework"

작성일: 2025-01-14

목표: 모든 게임 로직(직업, 아이템, 미션, GUI)을 데이터(Config) 기반으로 구동하여, 컴파일 없이 기획 사항을 수정할 수 있는 유연한 플러그인 구축.

## 1. 핵심 아키텍처 철학 (Core Philosophy)

이 플러그인은 하드코딩된 RPG가 아니라, **"설정 파일을 읽어 게임 규칙을 생성하는 엔진"**이 되어야 합니다.

- **Logic (Code):** "A 행위를 하면 B 조건에 따라 C 결과를 낸다"는 메커니즘만 보유.
    
- **Data (YAML):** "A는 돌 캐기, B는 10% 확률, C는 다이아몬드 지급"이라는 구체적 내용은 파일로 분리.
    
- **Hot Reload:** 서버 리로드 없이 `/dw reload` 명령어로 밸런스 패치 가능.
    

## 2. 파일 시스템 구조 (File System Structure)

플러그인 폴더(`plugins/DreamWork`) 내부는 다음과 같이 구성되어야 합니다. 기획서 파일들과 1:1로 매칭되는 구조입니다.

```
DreamWork/
├── config.yml              # 전체 시스템 설정 (DB, Debug 모드 등)
├── messages.yml            # 모든 채팅 메시지 및 프리픽스
├── items/                  # 사용자 정의 아이템 (PDC 데이터 포함)
│   ├── minerals.yml        # 광부 자원 (참고: 광부 세부 테이블.pdf)
│   ├── crops.yml           # 농부 작물/요리 (참고: 농부 세부 테이블.pdf)
│   ├── fishes.yml          # 어부 물고기 (참고: 어부 세부 테이블.pdf)
│   └── tools.yml           # 커스텀 도구 (합금 곡괭이 등)
├── jobs/                   # 직업별 로직 설정
│   ├── miner.yml           # 채굴 경험치, 스킬 쿨타임 (참고: 광부 변경.pdf)
│   ├── farmer.yml          # 수확 확률, 비료 설정 (참고: 농부 변경.pdf)
│   ├── fisher.yml          # 낚시 확률, 미끼 설정 (참고: 어부 변경.pdf)
│   ├── hunter.yml          # 몹 경험치, 도감 설정 (참고: 사냥꾼 변경.pdf)
│   └── adventurer.yml      # 바이옴/구조물 보상 (참고: 탐험가 변경.pdf)
├── missions/               # 미션 체인 및 보상
│   ├── daily_missions.yml  # 일일 미션 (참고: 공통.pdf)
│   └── job_missions.yml    # 직업별 승급 미션 (참고: 각 직업 미션.pdf)
├── gui/                    # GUI 레이아웃 배치
│   ├── main_dashboard.yml  # (참고: GUI.pdf)
│   └── shop_layouts.yml    
└── database/               # (자동생성) 유저 데이터 json/sqlite
```

## 3. 모듈별 상세 구현 계획 (Implementation Details)

### 3.1. Item Manager (Custom Item Engine)

- **목표:** `Plan2.0.pdf`에 언급된 PDC(PersistentDataContainer)를 활용한 커스텀 아이템 생성기.
    
- **기능:**
    
    - 바닐라 아이템에 NBT/PDC 태그를 부여하여 새로운 아이템으로 정의.
        
    - 예: "3성 감자"는 감자 아이템에 `quality: 3` 태그가 붙은 것.
        
- **Config 구조 예시 (`crops.yml`):**
    
    ```
    potato_3star:
      material: POTATO
      name: "&6[3성] 황금빛 감자"
      lore:
        - "&7최고급 토양에서 자란 감자입니다."
      pdc_data:
        quality: 3
        type: "crop"
      custom_model_data: 1001
    ```
    

### 3.2. Job Experience Engine (직업 엔진)

각 직업의 `변경.pdf`와 `세부 테이블.pdf`를 참조하여, 행위에 대한 보상을 테이블화합니다.

#### A. 광부 (Miner Module)

- **참고:** `광부 변경.pdf`, `광부 세부 테이블.pdf`
    
- **구현:** `BlockBreakEvent` 리스너가 설정 파일을 참조.
    
- **Config 로직:**
    
    - `Target`: 부순 블록 (COAL_ORE)
        
    - `Conditions`: 도구 요구사항, 실크터치 여부.
        
    - `Rewards`: 경험치, 돈, **확률적 추가 드롭(커스텀 아이템)**.
        
    - **개발 포인트:** `DropManager`를 만들어 "미지의 광석"이나 "드림 스톤" 드롭 확률을 YAML에서 수정 가능하게 함.
        

#### B. 농부 (Farmer Module)

- **참고:** `농부 변경.pdf`, `농부 세부 테이블.pdf`
    
- **구현:** `BlockBreakEvent` (수확), `PlayerInteractEvent` (심기).
    
- **Config 로직:**
    
    - `QualityChance`: 농부 레벨에 따른 1~3성 작물 등장 확률 공식. (`y = ax + b` 형태의 수식을 파싱하거나, 구간별 확률 테이블 사용)
        
    - `Fertilizer`: 비료 아이템 ID와 적용 효과 매핑.
        

#### C. 어부 (Fisher Module)

- **참고:** `어부 변경.pdf`, `어부 세부 테이블.pdf`
    
- **구현:** `PlayerFishEvent`.
    
- **Config 로직:**
    
    - `Pools`: 바이옴(WARM_OCEAN) + 시간(NIGHT) + 미끼(KRIL) -> 낚을 수 있는 물고기 목록 리스트.
        
    - **개발 포인트:** 낚시 확률 테이블을 가중치(Weight) 기반 랜덤 시스템으로 구현.
        

#### D. 사냥꾼 (Hunter Module)

- **참고:** `사냥꾼 변경.pdf`, `사냥꾼 세부 테이블.pdf`
    
- **구현:** `EntityDeathEvent`.
    
- **Config 로직:**
    
    - `SpawnReason`: `SPAWNER`면 보상 0, `NATURAL`이면 보상 100% (설정으로 제어).
        
    - `EliteMob`: 엘리트 몹 등장 확률 및 버프(체력 배수, 공격력 배수) 설정.
        

#### E. 탐험가 (Adventurer Module)

- **참고:** `탐험가 변경.pdf`, `탐험가 세부 테이블.pdf`
    
- **구현:** `PlayerMoveEvent` (청크 변경 감지).
    
- **Config 로직:**
    
    - `Biomes`: 각 바이옴별 최초 발견 경험치/돈 설정.
        
    - `Structures`: 구조물 감지 범위 및 보상.
        

### 3.3. GUI System (Dynamic Inventory)

- **참고:** `GUI.pdf`
    
- **목표:** 인벤토리 슬롯 위치를 하드코딩하지 않고 YAML로 배치.
    
- **구현:**
    
    - `GuiManager`가 `gui/*.yml`을 로드하여 인벤토리 생성.
        
    - 각 버튼은 `Action`을 가짐 (예: `OPEN:job_miner`, `CMD:spawn`).
        
    - **Config 예시:**
        
        ```
        main_menu:
          title: "DreamWork Dashboard"
          size: 27
          items:
            11:
              material: DIAMOND_PICKAXE
              name: "&e광부 정보"
              action: "OPEN_GUI:miner_detail"
        ```
        

### 3.4. Mission System (Universal Quest Engine)

- **참고:** `공통.pdf` 및 각 직업 `미션.pdf`
    
- **목표:** 새로운 퀘스트를 코드 수정 없이 추가 가능.
    
- **구현:**
    
    - `MissionType` Enum 정의: `BREAK`, `FISH`, `KILL`, `WALK`, `CRAFT`, `EAT`.
        
    - `MissionObject` 정의: Config에서 목표량, 대상 아이템, 보상 등을 파싱.
        
    - **Config 예시 (`daily_missions.yml`):**
        
        ```
        miner_daily_1:
          type: BREAK
          target: IRON_ORE
          amount: 50
          rewards:
            money: 100
            exp: 20
        ```
        

## 4. 데이터 연동 및 외부 API (Integration)

### 4.1. 외부 플러그인 (External Hooks)

- **참고:** `외부 Plugin Hook.pdf`
    
- **Vault:** `EconomyProvider` 인터페이스 구현. 모든 돈 지급 로직을 여기로 통일.
    
- **Towny:** `RegionChecker` 클래스 구현. "타운 내부에서만 농작물 성장" 옵션을 켜고 끌 수 있게 함.
    
- **PlaceholderAPI:** `DreamWorkExpansion` 클래스.
    
    - 모든 직업 레벨, 경험치, 랭크 등을 PAPI 변수로 노출.
        
    - 자동 등록 로직 구현.
        

### 4.2. 개발자용 디버그 도구 (Dev Tools)

- **Developer Mode:** `config.yml`에서 `debug_mode: true` 설정 시.
    
    - 플레이어가 블록을 캘 때마다 콘솔에 "Block: STONE, Matched Rule: None" 같은 로그 출력.
        
    - 확률 로직이 어떻게 작동했는지 추적 가능.
        
- **Admin Commands:**
    
    - `/dw debug setlevel <user> <job> <level>`: 즉시 레벨 변경.
        
    - `/dw debug give <item_id>`: `items/*.yml`에 정의된 커스텀 아이템 지급.
        
    - `/dw reload`: 모든 Config 핫 리로드.
        

## 5. 개발 로드맵 (Development Steps)

1. **Skeleton Build:** 플러그인 메인 클래스, Config Manager, Command Executor 구축.
    
2. **Core Systems:**
    
    - `UserDataManager` (플레이어 정보 저장/로드)
        
    - `ItemManager` (YAML -> ItemStack 변환)
        
3. **Job Engines:** 광부 -> 농부 -> 어부 -> 사냥꾼 -> 탐험가 순으로 리스너 및 로직 구현.
    
4. **Mission & GUI:** 미션 체크 로직과 GUI 렌더러 구현.
    
5. **Integration:** Vault, Towny, PAPI 연동.
    
6. **Testing:** `debug` 모드를 켜고 YAML 수치를 조절하며 밸런스 테스트.


<br>
사용자님께서 요청하신 **Item Manager (Custom Item Engine)** 모듈의 상세 아키텍처 설계안입니다.

이 모듈은 DreamWork 플러그인의 '심장'과 같습니다. 광부의 광물, 농부의 작물, 어부의 물고기 등 모든 파생 아이템이 이 엔진을 통해 생성되고 식별됩니다.

---

# 🏗️ Module: Item Manager Architecture

**"DreamWork Item Engine (DW-IE)"**

## 1. 설계 목적 및 핵심 개념

단순히 아이템의 이름과 설명(Lore)만 바꾸는 것이 아니라, **PersistentDataContainer (PDC)**를 활용하여 아이템에 **'디지털 지문(Fingerprint)'**을 새기는 것이 핵심입니다. 이를 통해 이름이 변경되거나 모루질을 당해도 시스템은 해당 아이템이 "3성 감자"임을 정확히 식별할 수 있습니다.

## 2. 데이터 구조 (Data Structure)

### 2.1. YAML Configuration (`items/*.yml`)

사용자가 정의할 수 있는 아이템 속성의 전체 스펙입니다.

|**속성 (Field)**|**타입**|**설명**|
|---|---|---|
|**key**|String|고유 식별자 (ID). 예: `potato_3star`, `miner_pickaxe_ver2`|
|**material**|Material|마인크래프트 바닐라 아이템 재질 (예: `POTATO`, `DIAMOND_PICKAXE`)|
|**display_name**|String|색상 코드가 포함된 아이템 이름|
|**lore**|List<String>|아이템 설명 줄글|
|**custom_model_data**|Integer|(옵션) 리소스팩 텍스처 매핑용 ID|
|**pdc_data**|Map|**[핵심]** 아이템 내부에 숨겨진 데이터 태그 (NBT 대체)|
|**enchantments**|Map|기본 부여될 인챈트 목록|
|**item_flags**|List|바닐라 속성 숨김 옵션 (예: `HIDE_ATTRIBUTES`, `HIDE_ENCHANTS`)|
|**attributes**|Map|공격력, 이동 속도 등 속성 수정자 (AttributeModifier)|

### 2.2. 내부 객체 모델 (Java Class Design)

코드를 작성하지 않지만, 필요한 클래스 객체의 역할을 정의합니다.

1. **`DreamItemTemplate` (Class)**
    
    - YAML에서 로드된 정보를 메모리에 들고 있는 **청사진(Blueprint)** 객체입니다.
        
    - 서버 실행 시점에 `items/*.yml`을 모두 읽어 `HashMap<String, DreamItemTemplate>` 형태로 캐싱합니다.
        
2. **`DreamItemBuilder` (Factory Class)**
    
    - `DreamItemTemplate`을 입력받아 실제 버킷 `ItemStack`을 생성해내는 공장입니다.
        
    - PDC 주입, 색상 코드 변환, 플레이어 이름 치환 등의 작업을 수행합니다.
        

---

## 3. 핵심 로직 흐름 (Logical Flow)

### 3.1. 로딩 및 캐싱 (Initialization)

1. 플러그인 활성화(`onEnable`) 시 `items/` 폴더 내의 모든 `.yml` 파일을 스캔.
    
2. 각 섹션(Key)을 파싱하여 `DreamItemTemplate` 객체 생성.
    
3. `ItemManager` 내의 `itemCache` 맵에 저장.
    
4. 만약 중복된 Key가 발견되면 경고 로그 출력 (Override 방지).
    

### 3.2. 아이템 생성 (Item Generation)

**시나리오:** 명령어나 이벤트로 유저에게 `potato_3star`를 지급해야 함.

1. **요청:** `ItemManager.createItem("potato_3star", amount)` 호출.
    
2. **조회:** 캐시에서 "potato_3star" 템플릿 검색.
    
3. **빌딩 (Building Process):**
    
    - `ItemStack` 생성 (Material: POTATO).
        
    - `ItemMeta` 추출.
        
    - **PDC 주입 (Data Injection):**
        
        - `NamespacedKey(plugin, "id")` -> 값: `"potato_3star"` (필수)
            
        - `NamespacedKey(plugin, "type")` -> 값: `"crop"` (카테고리 분류용)
            
        - `NamespacedKey(plugin, "quality")` -> 값: `3` (로직 처리용)
            
    - **시각화:** Display Name, Lore, CustomModelData 적용.
        
    - **속성 부여:** Enchantment, Attributes 적용.
        
4. **반환:** 완성된 `ItemStack` 리턴.
    

### 3.3. 아이템 식별 (Item Identification)

**시나리오:** 플레이어가 어떤 아이템을 손에 들고 우클릭했거나, 상점에 판매하려 함. 이것이 일반 감자인지, 3성 감자인지 구별해야 함.

1. **이벤트 발생:** `PlayerInteractEvent` 등.
    
2. **검증 요청:** `ItemManager.getDreamItemId(ItemStack item)` 호출.
    
3. **PDC 판독:**
    
    - 해당 `ItemStack`의 `ItemMeta`에서 PDC 컨테이너 조회.
        
    - `NamespacedKey(plugin, "id")` 값이 존재하는지 확인.
        
4. **결과 반환:**
    
    - 값이 있으면: `"potato_3star"` 문자열 반환.
        
    - 값이 없으면: `null` (바닐라 아이템으로 간주).
        
5. **로직 처리:** 반환된 ID를 기반으로 `crops.yml`이나 `shop.yml`의 가격/효과 정보를 참조하여 로직 실행.
    

---

## 4. 고급 기능 상세 (Advanced Features)

### 4.1. 동적 플레이스홀더 (Dynamic Placeholders)

아이템 생성 시점에 특정 문구를 동적으로 변경해야 하는 경우를 지원합니다.

- **YAML 예시:** `lore: - "제작자: <player_name>"`
    
- **빌더 로직:** `createItem` 호출 시 `Map<String, String> placeholders`를 인자로 받아, Lore 내부의 `<player_name>`을 실제 닉네임으로 `replace` 후 아이템 생성.
    
- **활용:** 탐험가가 제작한 지도에 "발견자: 홍길동"을 영구히 박제할 때 사용.
    

### 4.2. 텍스처 매핑 (Custom Model Data)

리소스팩 없이도 다양한 아이템을 구분하기 위해 `CustomModelData`를 적극 활용합니다.

- YAML에서 `custom_model_data: 1001`로 정의.
    
- 나중에 리소스팩이 준비되면, 해당 ID에 텍스처만 연결하면 즉시 적용됨. (코드 수정 불필요)
    

### 4.3. 매터리얼 폴백 (Material Fallback)

잘못된 `Material` 이름이 설정 파일에 적혔을 때를 대비한 안전장치.

- `Material.getMaterial(name)`이 `null`을 반환하면, 콘솔에 **[ERROR]**를 띄우고 기본값(`STONE` 또는 `BARRIER`)으로 아이템을 생성하여 서버 셧다운 방지.
    

---

## 5. PDC 데이터 스키마 표준안 (PDC Schema Standard)

모든 커스텀 아이템은 아래의 PDC Key 규칙을 따릅니다. (Namespace는 플러그인 이름 사용)

|**Key Name**|**Data Type**|**용도**|**예시 값**|
|---|---|---|---|
|`dw_id`|STRING|아이템의 고유 ID (Config Key와 일치)|`potato_3star`|
|`dw_category`|STRING|아이템의 대분류 (필터링 최적화용)|`CROP`, `MINERAL`, `FISH`|
|`dw_quality`|INTEGER|등급/티어 정보 (계산 로직용)|`1`, `2`, `3`|
|`dw_timestamp`|LONG|아이템 생성 시각 (유효기간 체크용)|`1705234000000`|
|`dw_owner`|STRING|귀속 아이템일 경우 소유자 UUID|`uuid-string...`|

---

## 6. 개발 시 고려사항 (Tips for Implementation)

1. **성능 최적화:** `ItemMeta` 생성과 수정은 비용이 드는 작업입니다. 자주 쓰이는 "기본형 아이템"은 `createItem` 내부에서 매번 새로 만들지 말고, `DreamItemTemplate` 내부에 완성된 `ItemStack` 원본을 하나 캐싱해두고 `.clone()` 해서 쓰는 것이 훨씬 빠릅니다. (단, 플레이스홀더 교체가 필요한 경우는 제외)
    
2. **디버깅:** `/dw getitem <id>` 명령어를 만들 때, 탭 컴플리션(Tab Completion)이 `itemCache`의 Key Set을 반환하도록 하여 운영 편의성을 높이세요.
    
3. **버전 호환성:** PDC는 1.14+ 부터 지원합니다. 혹시 모를 하위 버전 호환성 이슈가 없도록 `PersistentDataType.STRING` 등 타입을 명확히 지정해야 합니다.
    

이 설계안은 **데이터(YAML)가 로직(Java)을 지배하는 구조**를 완벽하게 지원하며, 추후 아이템 밸런스 수정 시 서버 재시작 없이 설정 파일 수정만으로 게임 내용을 바꿀 수 있는 강력한 기반이 될 것입니다.

<br>
**Job Experience Engine (직업 성장 엔진)**의 상세 아키텍처 설계안입니다.

이 모듈은 플레이어의 행위(채광, 농사, 사냥 등)를 감지하고, 설정 파일(YAML)에 정의된 규칙에 따라 **경험치, 돈, 아이템**을 지급하는 **'보상 처리 장치'**입니다.

---

# 🏗️ Module: Job Experience Engine

**"DreamWork Logic Core (DW-LC)"**

## 1. 핵심 설계 철학 (Core Philosophy)

1. **이벤트의 추상화 (Abstraction):** 코드는 "광부가 돌을 깼다"를 알지 못합니다. 단지 "A 행위가 발생했고, 대상은 B이다"라는 사실만 인식하여 Config와 대조합니다.
    
2. **중앙집중식 보상 처리:** 각 직업 리스너(Listener)가 직접 돈/경험치를 주는 것이 아니라, `RewardCalculator`에 요청을 보내고 승인받는 구조입니다.
    
3. **어뷰징 원천 차단:** 모든 행위는 '유효성 검증기(Validator)'를 통과해야만 보상이 지급됩니다.
    

---

## 2. 엔진 구조도 (Architecture Overview)

이 엔진은 크게 **Listener Layer (감지)**, **Validator Layer (검증)**, **Calculation Layer (계산)**, **Execution Layer (지급)** 4단계로 작동합니다.

### 2.1. 데이터 모델 (DTO)

메모리에 상주하며 YAML 데이터를 들고 있는 객체들입니다.

- **`JobActionTemplate`**: 특정 행위에 대한 정의
    
    - `target`: 감지 대상 (Material, EntityType, Biome 등)
        
    - `conditions`: 요구 조건 (도구 종류, 실크터치 금지, 타운 내부 여부 등)
        
    - `base_exp`: 기본 경험치
        
    - `base_money`: 기본 돈
        
    - `drops`: 추가 드롭 테이블 (확률 포함)
        

---

## 3. 직업별 상세 구현 전략 (Job Specifics)

### 3.1. ⛏️ 광부 (Miner Handler)

- **Trigger:** `BlockBreakEvent`
    
- **Core Logic:**
    
    1. **Block Map Lookup:** 부순 블록(`event.getBlock().getType()`)이 `miner.yml`의 `rewards` 섹션 키(Key)에 존재하는지 `O(1)`로 조회.
        
    2. **Validator (PDC Check):**
        
        - 해당 블록에 `placed_by_player` PDC 태그가 있는지 확인. (있으면 보상 0).
            
        - `Fortune`(행운) 인챈트가 있어도 경험치/돈은 1회만 지급.
            
    3. **Drop Calculation:**
        
        - 설정된 확률에 따라 `ItemManager`를 통해 "미지의 광석"이나 "드림 스톤" 생성 후 월드에 드롭.
            
    4. **Auto-Smelt (Optional):** 고레벨 광부의 경우, 인벤토리에 들어오는 아이템을 즉시 구운 형태로 변환 (Config 옵션).
        

### 3.2. 🌾 농부 (Farmer Handler)

- **Trigger:** `BlockBreakEvent` (수확), `PlayerInteractEvent` (심기/비료)
    
- **Core Logic:**
    
    1. **Age Check:** `block.getBlockData()`가 `Ageable`인지 확인. `getAge() != getMaximumAge()`라면 보상 지급 중단 (덜 자란 작물 수확 방지).
        
    2. **Quality Logic (RNG):**
        
        - 수확 시 농부 레벨에 따른 가중치 계산.
            
        - 일반 작물 드롭을 취소(`event.setDropItems(false)`)하고, `ItemManager`에서 생성한 "3성 감자(PDC 포함)"를 자연 드롭(`dropItemNaturally`).
            
    3. **Fertilizer Intercept:**
        
        - 플레이어가 작물에 아이템을 우클릭할 때, 해당 아이템이 `ItemManager`에 등록된 '비료'인지 확인. 맞다면 즉시 성장(`setAge`) 또는 수확량 부스트 적용.
            

### 3.3. 🎣 어부 (Fisher Handler)

- **Trigger:** `PlayerFishEvent` (`CAUGHT_FISH` 상태)
    
- **Core Logic:**
    
    1. **Loot Table Override:** 바닐라 마인크래프트의 낚시 루팅 테이블을 무시하거나 덮어씌움.
        
    2. **Condition Check:**
        
        - 현재 바이옴(Biome), 시간(낮/밤), 날씨(비), Y좌표(깊은 바다)를 파악.
            
        - 낚싯대 미끼 슬롯(Offhand 혹은 GUI)에 있는 '특수 미끼' 아이템 감지.
            
    3. **Weighted Random Selection:**
        
        - 조건에 맞는 물고기 리스트 중 가중치(Weight) 기반으로 하나를 추첨.
            
        - 예: `크릴새우` 미끼 사용 + `깊은 바다` = `참치(10%)`, `대구(90%)`.
            
    4. **Alive State:** 낚인 물고기 아이템에 `is_alive: true` PDC 태그 부착 (아쿠아리움 납품용).
        

### 3.4. 🏹 사냥꾼 (Hunter Handler)

- **Trigger:** `EntityDeathEvent`
    
- **Core Logic:**
    
    1. **SpawnReason Filter:**
        
        - 가장 중요. 엔티티의 메타데이터나 PDC에 저장된 `spawn_reason`을 확인.
            
        - `SPAWNER`, `SPAWNER_EGG`, `SLIME_SPLIT` 등은 보상 제외. 오직 `NATURAL`만 인정.
            
    2. **Killer Check:** `entity.getKiller()`가 플레이어인지 확인. (낙사, 용암사 제외).
        
    3. **Elite Mob Roll:**
        
        - `CreatureSpawnEvent`에서 미리 1% 확률로 엘리트 몹으로 지정(PDC 태그)된 몬스터였다면, 추가 보상 지급.
            

### 3.5. 🗺️ 탐험가 (Adventurer Handler)

- **Trigger:** `PlayerMoveEvent` (Chunk 단위), `PlayerInteractEvent` (지도/나침반)
    
- **Core Logic (최적화 필수):**
    
    1. **Chunk Hashing:** `PlayerMoveEvent`는 틱당 발생하므로, `FromChunk == ToChunk`이면 즉시 리턴하여 연산 최소화.
        
    2. **Visit Cache:**
        
        - `HashSet<Long> visitedChunks`를 메모리에 유지 (User Session).
            
        - 이미 방문한 청크 키(Coordinate Key)가 있으면 무시.
            
        - 없으면 DB/File 확인 후 보상 지급 -> 메모리 및 DB 업데이트.
            
    3. **Structure Detection:**
        
        - 서버 렉 유발 1순위. `StructureBoundingBox` 체크는 반드시 **비동기(Async)**로 처리하거나, 플레이어가 "탐사 도구"를 클릭했을 때만 발동하도록 제한.
            

---

## 4. 통합 레벨링 및 보상 처리 (Universal Leveling)

모든 직업 핸들러는 최종적으로 **`JobManager.grantReward()`** 메서드를 호출합니다.

### 4.1. 레벨업 공식 (Exp Formula)

하드코딩하지 않고 Config의 수식을 파싱합니다 (Java `ScriptEngine` 또는 간단한 파서 이용).

- _Config 예시:_ `expression: "100 * (level ^ 1.5)"`
    
- 현재 경험치가 요구량을 초과하면:
    
    1. Level++
        
    2. 남은 경험치 이월.
        
    3. **Action:** `LevelUpEvent` 발생 -> 칭호 변경, 스탯 적용, 축하 메시지 출력.
        

### 4.2. 경제 연동 (Vault Hook)

- Config에 설정된 `base_money`에 유저의 `Money Multiplier`(랭크 보너스 등)를 곱하여 `Vault` API로 입금.
    

---

## 5. 데이터 흐름 시나리오 (Data Flow Scenario)

**상황:** Lv.5 광부 유저가 '철 광석'을 캤을 때.

1. **Event:** `BlockBreakEvent` 발생 (Material: IRON_ORE).
    
2. **Listener:** `MinerListener`가 감지.
    
3. **Validator:**
    
    - 유저가 Creative 모드인가? (No)
        
    - 블록에 `placed_by_player` 태그가 있는가? (No)
        
    - 도구가 곡괭이인가? (Yes)
        
4. **Template Fetch:** `miner.yml`에서 `IRON_ORE` 데이터 로드.
    
    - Base Exp: 5, Base Money: 2
        
    - Drops: {Unknown_Ore: 5%}
        
5. **Calculator:**
    
    - Exp = 5 * 1.0(배율) = 5
        
    - Money = 2 * 1.2(거주민 랭크 보너스) = 2.4
        
6. **Action:**
    
    - `JobManager.addExp(User, MINER, 5)`
        
    - `Vault.deposit(User, 2.4)`
        
    - `Random(0~100)` < 5 ? -> `ItemManager.createItem("unknown_ore")` 드롭.
        
7. **Feedback:** 액션바(Action Bar)에 `"+5 Exp | +2.4 Gold"` 출력.
    

---

## 6. 개발 시 주의사항 (Tech Notes)

1. **PDC 관리:** 블록 설치 시(`BlockPlaceEvent`) 반드시 `placed_by_player` 태그를 청크 데이터나 블록 상태에 저장해야 경제 붕괴를 막습니다. (가장 중요)
    
2. **비동기 처리:** DB 저장이나 복잡한 계산(탐험가 구조물 찾기)은 메인 스레드를 멈추지 않도록 `BukkitRunnable.runTaskAsynchronously`를 활용해야 합니다.
    
3. **핫 리로드:** `Map<Material, JobActionTemplate>` 캐시는 `/dw reload` 시 깔끔하게 `clear()` 후 다시 `put()` 되어야 합니다.
    

이 설계는 **확장성**에 초점을 맞추었습니다. 나중에 "벌목꾼" 직업을 추가하고 싶다면, 코드를 뜯어고칠 필요 없이 `JobActionTemplate`을 사용하는 `WoodcutterListener` 하나만 추가하면 됩니다.
<br>

사용자님께서 요청하신 **GUI System (Dynamic Inventory)** 모듈의 상세 아키텍처 설계안입니다.

이 모듈은 DreamWork 플러그인의 **"얼굴"**입니다. 사용자가 복잡한 명령어 대신 직관적인 아이콘 클릭으로 모든 게임 컨텐츠를 즐길 수 있도록, **'설정 파일(YAML)이 그리는 인터페이스'**를 구축합니다.

---

# 🏗️ Module: GUI System (Dynamic Inventory)

**"DreamWork Display Engine (DW-DE)"**

## 1. 설계 목적 및 핵심 철학

일반적인 플러그인처럼 인벤토리 소스 코드를 하드코딩하면, 아이템 위치 하나를 바꾸기 위해 플러그인을 다시 컴파일해야 합니다.

이 엔진은 **"GUI의 레이아웃(View)과 작동 로직(Controller)을 완벽하게 분리"**하여, 기획자가 YAML 파일만 수정하면 즉시 서버 내의 메뉴 디자인이 바뀌도록 설계합니다.

## 2. 데이터 구조 (Data Structure)

### 2.1. YAML Configuration (`gui/*.yml`)

화면 구성을 정의하는 설계도입니다.

|**속성 (Field)**|**타입**|**설명**|
|---|---|---|
|**key**|String|GUI 식별자 (예: `main_dashboard`, `miner_job_info`)|
|**title**|String|인벤토리 상단 제목 (PlaceholderAPI 지원 필수)|
|**size**|Integer|슬롯 크기 (9, 18, 27, 36, 45, 54)|
|**fill_item**|Material|(옵션) 빈 공간을 채울 배경 아이템 (예: `GRAY_STAINED_GLASS_PANE`)|
|**buttons**|Map|각 슬롯에 들어갈 버튼 정의|

#### **버튼(Button) 상세 스펙**

- **`slot`**: 위치 (0~53) 또는 리스트 (여러 칸 차지)
    
- **`material`**: 아이콘 재질
    
- **`name`**: 버튼 이름
    
- **`lore`**: 설명 (동적 데이터 포함)
    
- **`model_data`**: (옵션) 커스텀 텍스처 ID
    
- **`action`**: **[핵심]** 클릭 시 실행할 동작 트리거
    
- **`condition`**: (옵션) 버튼이 보이기 위한 조건 (예: `permission:dreamwork.admin`)
    

---

## 3. 내부 객체 모델 (Java Class Architecture)

### 3.1. `DreamGui` (Abstract Class)

모든 커스텀 GUI의 부모 클래스입니다. `InventoryHolder`를 상속받습니다.

- **역할:** 이 인벤토리가 "DreamWork의 GUI임"을 증명하는 신분증.
    
- **필드:**
    
    - `Player viewer`: 이 창을 보고 있는 플레이어.
        
    - `String guiId`: YAML에서 로드한 GUI 식별자.
        
    - `Map<Integer, GuiAction> actions`: 슬롯별 클릭 이벤트 매핑 테이블.
        

### 3.2. `GuiManager` (Singleton Controller)

- **역할:**
    
    - 서버 시작 시 `gui/*.yml` 파일을 모두 읽어 `GuiTemplate` 형태로 메모리에 캐싱.
        
    - `/dw reload` 시 캐시 초기화 및 재로딩.
        
    - `openGui(Player p, String id)` 메서드 제공.
        

### 3.3. `GuiBuilder` (Factory)

- **역할:** 정적인 템플릿(Template)에 동적인 데이터(User Data)를 입혀 실제 `Inventory` 객체를 생성.
    
- **프로세스:**
    
    1. 템플릿 로드 (`main_dashboard`).
        
    2. 플레이어 정보 조회 (직업 레벨, 돈 등).
        
    3. **Placeholder Parsing:** 제목과 Lore에 있는 `%dreamwork_miner_level%` 등을 실제 숫자로 변환.
        
    4. 인벤토리 생성 및 아이템 배치.
        

---

## 4. 액션 시스템 (Action Trigger System)

버튼 클릭 시 어떤 일이 벌어질지를 정의하는 문자열 프로토콜입니다. YAML의 `action` 필드에 작성합니다.

|**액션 코드**|**파라미터 예시**|**동작 설명**|
|---|---|---|
|**`OPEN`**|`OPEN:job_miner`|다른 GUI(`job_miner.yml`)를 연다. (메뉴 이동)|
|**`CMD`**|`CMD:spawn`|플레이어가 해당 명령어를 친 것처럼 처리.|
|**`CONSOLE`**|`CONSOLE:give %player% diamond 1`|콘솔 권한으로 명령어 실행.|
|**`CLOSE`**|`CLOSE`|인벤토리 닫기.|
|**`SOUND`**|`SOUND:ENTITY_EXPERIENCE_ORB_PICKUP`|효과음 재생.|
|**`MSG`**|`MSG:&a저장되었습니다.`|채팅 메시지 전송.|
|**`CHECK`**|`CHECK:cost_money:1000`|(조건부) 돈이 1000원 이상일 때만 다음 액션 진행.|

복합 액션 예시:

action: [ "CHECK:cost_money:500", "CMD:say 구매 완료!", "SOUND:BLOCK_ANVIL_USE", "OPEN:shop_main" ]

-> 순차적으로 실행되며, CHECK 실패 시 중단됨.

---

## 5. 핵심 GUI 유형별 구현 전략 (Types of GUI)

### 5.1. 📋 메인 대시보드 (`MainDashboard`)

- **기능:** 플레이어의 현재 상태 요약 및 네비게이션.
    
- **특징:**
    
    - 자신의 머리(Head) 아이템을 사용하여 프로필 표시.
        
    - 동적 Lore: "현재 직업 랭킹: 5위" 등의 정보가 실시간 갱신되어야 함.
        

### 5.2. 🛠️ 기능성 머신 GUI (`MachineGui`)

- **대상:** 대장간(광부), 요리 솥(농부) 등 아이템을 넣고 결과를 받는 GUI.
    
- **구조:**
    
    - **Input Slot:** 유저가 아이템을 놓을 수 있는 슬롯 (이벤트 캔슬 안 함).
        
    - **Output Slot:** 결과물을 가져갈 수만 있는 슬롯 (놓기 금지).
        
    - **Process Button:** 클릭 시 로직(Job Engine)을 호출하여 결과 계산.
        
- **주의사항:** `InventoryCloseEvent` 발생 시 Input 슬롯에 남은 아이템을 반드시 플레이어에게 돌려주거나 바닥에 드롭해야 함 (아이템 증발/복사 방지).
    

### 5.3. 🏪 상점 및 교환소 (`ShopGui`)

- **기능:** 아이템 구매/판매.
    
- **구조:**
    
    - 좌클릭: 구매 (1개), 우클릭: 구매 (1세트), Shift+클릭: 판매.
        
    - YAML 액션 예시: `action_left: "BUY:potato:1"`, `action_right: "BUY:potato:64"`
        

---

## 6. 이벤트 리스너 로직 (Event Flow)

**시나리오:** 유저가 메뉴에서 '광부 정보' 버튼을 클릭함.

1. **`InventoryClickEvent` 발생.**
    
2. **Holder Check:** `event.getInventory().getHolder()`가 `DreamGui` 인스턴스인지 확인. (아니면 리턴)
    
3. **Cancel Interaction:** 기본적으로 `event.setCancelled(true)` 하여 아이템을 빼가지 못하게 함. (Input 슬롯 예외 처리)
    
4. **Slot Lookup:** 클릭한 슬롯 번호(Slot ID)에 매핑된 `GuiAction`이 있는지 확인.
    
5. **Action Execute:**
    
    - 액션 파서가 `OPEN:miner_detail` 문자열을 해석.
        
    - `GuiManager.openGui(player, "miner_detail")` 호출.
        
6. **Refresh:** 만약 랭크 업 등으로 정보가 바뀌었다면, 새 인벤토리를 빌드하여 열어줌.
    

---

## 7. 개발 시 고려사항 (Implementation Tips)

1. **더블 버퍼링 방지 (Flickering):** GUI를 갱신할 때 인벤토리를 닫았다가(`closeInventory`) 다시 열면 화면이 깜빡입니다. 대신 `inventory.setContents(newItems)`를 사용하여 열린 상태에서 내용물만 교체해야 부드럽습니다.
    
2. **비동기 로딩 (Async Head):** 플레이어 머리(`SKULL_ITEM`)는 스킨을 불러오는 데 시간이 걸려 서버 렉(Lag)을 유발합니다. GUI는 먼저 열고, 머리 아이템만 **비동기 스레드**에서 로딩한 뒤 나중에 슬롯을 업데이트하는 방식을 권장합니다.
    
3. **메타데이터 활용:** 복잡한 기능(예: 페이징의 '다음 페이지')은 아이템의 PDC(PersistentDataContainer)에 `next_page_index: 2` 같은 데이터를 숨겨두면, 리스너에서 쉽게 다음 페이지를 계산할 수 있습니다.
    

이 설계안은 기획자가 **`gui/` 폴더의 파일만 수정하면 게임의 UI를 마음대로 바꿀 수 있는 환경**을 제공하며, DreamWork 프로젝트의 "데이터 주도(Config-Driven)" 철학을 완벽하게 따릅니다.
<br>
사용자님께서 요청하신 **Mission System (Universal Quest Engine)** 모듈의 상세 아키텍처 설계안입니다.

이 모듈은 DreamWork 플러그인의 **'동기 부여(Motivation)'**를 담당합니다. 단순한 노가다를 넘어, 플레이어에게 끊임없는 목표를 제시하고 성장을 체감하게 만드는 **"설정 파일 기반의 퀘스트 엔진"**입니다.

---

# 🏗️ Module: Mission System (Universal Quest Engine)

**"DreamWork Quest Core (DW-QC)"**

## 1. 설계 목적 및 핵심 철학

1. **무한 확장성 (Infinite Extensibility):** "좀비 100마리 잡기" 퀘스트를 추가하기 위해 자바 코드를 수정해선 안 됩니다. 오직 YAML 파일 추가만으로 새로운 시즌, 새로운 일일 퀘스트가 생성되어야 합니다.
    
2. **체인 리액션 (Chain Reaction):** 퀘스트는 단발성으로 끝날 수도 있지만, `입문 -> 숙련 -> 전문가` 단계로 이어지는 **'연계 퀘스트(Chain Mission)'**를 지원해야 합니다.
    
3. **통합 처리 (Universal Processing):** 광부의 '채굴', 사냥꾼의 '토벌', 탐험가의 '발견' 등 서로 다른 행위를 하나의 표준화된 규격으로 처리합니다.
    

---

## 2. 데이터 구조 (Data Structure)

### 2.1. YAML Configuration (`missions/*.yml`)

퀘스트의 내용을 정의하는 스크립트입니다.

|**속성 (Field)**|**타입**|**설명**|
|---|---|---|
|**key**|String|미션 고유 ID (예: `miner_daily_iron_1`, `hunter_chain_zombie_3`)|
|**display_name**|String|UI에 표시될 미션 제목 (예: "&e[일일] 철 광석 수급")|
|**type**|Enum|미션 목표 유형 (`BREAK`, `KILL`, `FISH`, `CRAFT`, `EAT`, `WALK`, `SUBMIT`)|
|**target**|List|목표 대상 ID (예: `IRON_ORE`, `ZOMBIE`, `potato_3star`)|
|**amount**|Integer|목표 달성 수치|
|**conditions**|List|(옵션) 미션 진행을 위한 제약 조건 (예: `BIOME:DESERT`, `TOOL:IRON_PICKAXE`)|
|**rewards**|Map|완료 보상 (돈, 경험치, 아이템, 명령어)|
|**next_mission**|String|(옵션) 클리어 시 자동으로 수주될 다음 미션 ID (체인 시스템)|
|**reset_cycle**|Enum|초기화 주기 (`DAILY`, `WEEKLY`, `ONE_TIME`, `NONE`)|

#### **YAML 예시 (`daily_missions.yml`)**

YAML

```
miner_daily_iron:
  type: BREAK
  target: [ "IRON_ORE", "DEEPSLATE_IRON_ORE" ]
  amount: 50
  display_name: "&7[일일] 대장간의 불꽃"
  lore:
    - "&f대장장이에게 줄 철광석을 캐오세요."
  conditions:
    - "WORLD:world"  # 야생 월드에서만 카운트
  rewards:
    money: 500
    job_exp:
      miner: 100
    items:
      - "material:COAL amount:10"
  reset_cycle: DAILY
```

### 2.2. 내부 객체 모델 (Java Class Design)

1. **`MissionTemplate` (DTO)**
    
    - YAML 정보를 읽기 전용(Read-Only)으로 메모리에 저장하는 객체.
        
    - 서버 로딩 시 `MissionManager`에 `Map<String, MissionTemplate>`으로 캐싱됨.
        
2. **`PlayerMissionData` (User State)**
    
    - 플레이어가 현재 진행 중인 미션의 상태를 저장하는 객체.
        
    - **Fields:**
        
        - `String missionId`: 진행 중인 미션 ID.
            
        - `int currentProgress`: 현재 달성도 (예: 35/50).
            
        - `boolean isCompleted`: 보상 수령 여부.
            
        - `long assignedTime`: 미션 받은 시간 (초기화 계산용).
            
    - DB나 JSON 파일(`userdata/uuid.json`)에 저장됨.
        

---

## 3. 핵심 로직 흐름 (Logical Flow)

이 엔진은 **"이벤트 발생 -> 필터링 -> 진행도 업데이트 -> 완료 체크"**의 4단계 파이프라인으로 작동합니다.

### 3.1. 이벤트 감지 (The Universal Listener)

각 직업 리스너나 별도의 `MissionListener`에서 이벤트를 감지하여 **`MissionManager`**로 정보를 던집니다.

- **상황:** 플레이어가 돌(STONE)을 캠.
    
- **코드 호출:** `MissionManager.processEvent(player, MissionType.BREAK, Material.STONE, 1);`
    
    - 리스너는 구체적인 미션 내용을 모릅니다. 단지 "무엇을 했다"는 사실만 전달합니다.
        

### 3.2. 진행도 업데이트 (Progress Update)

`MissionManager` 내부 로직입니다.

1. **검색:** 해당 플레이어(`player`)가 현재 보유한 `PlayerMissionData` 목록을 가져옵니다.
    
2. **매칭:** 목록 중 `type == BREAK`이고 `target`에 `STONE`이 포함된 미션이 있는지 찾습니다.
    
3. **조건 검사 (Condition Check):**
    
    - 미션에 `conditions`가 있다면 검증합니다. (예: 현재 바이옴이 사막인가?)
        
    - 조건 불만족 시 무시(Return).
        
4. **업데이트:** 조건이 맞으면 `currentProgress`를 `+1` 합니다.
    
5. **피드백:** 액션바(Action Bar)에 진행 상황 출력.
    
    - `"대지의 선물: 35 / 50 (+1)"`
        

### 3.3. 완료 처리 (Completion)

업데이트 후 `currentProgress >= amount`가 되면 발동합니다.

1. **상태 변경:** `isCompleted = true`.
    
2. **알림:** 화면 중앙 타이틀(Title)로 "미션 완료!" 출력 및 효과음 재생.
    
3. **보상 지급:** `rewards` 섹션을 파싱하여 돈(Vault), 경험치(JobEngine), 아이템 등을 지급.
    
4. **체인 시스템 (Chain System):**
    
    - `next_mission` 필드가 있다면, 현재 미션을 목록에서 지우고 **다음 단계 미션을 즉시 등록**합니다.
        
    - 예: `miner_1` 완료 -> `miner_2` 자동 수주.
        

---

## 4. 미션 유형별 구현 상세 (Mission Types)

다양한 직업 활동을 커버하기 위해 `MissionType` Enum을 정의하고 각각의 처리 로직을 마련해야 합니다.

|**Type**|**Trigger Event**|**Target 데이터 예시**|**비고**|
|---|---|---|---|
|**`BREAK`**|`BlockBreakEvent`|`IRON_ORE`, `OAK_LOG`|`placed_by_player` PDC 체크 필수 (어뷰징 방지)|
|**`KILL`**|`EntityDeathEvent`|`ZOMBIE`, `ENDER_DRAGON`|스포너 몹 제외 로직 적용|
|**`FISH`**|`PlayerFishEvent`|`COD`, `PUFFERFISH`|잡은 물고기 아이템 검사|
|**`CRAFT`**|`CraftItemEvent`|`BREAD`, `IRON_PICKAXE`|2성 이상의 결과물 요구 가능 (PDC 체크)|
|**`EAT`**|`PlayerItemConsumeEvent`|`COOKED_BEEF`, `potion_speed`|음식 섭취 미션|
|**`WALK`**|`PlayerMoveEvent`|`DESERT`, `PLAINS` (Biome)|특정 지역 탐험 (좌표/바이옴 체크)|
|**`SUBMIT`**|`InventoryClickEvent`|`npc_blacksmith`|NPC GUI에 아이템 납품 (가장 복잡함)|

---

## 5. 고급 기능: 납품 시스템 (Submission System)

단순 행동 반복이 아니라, NPC에게 아이템을 가져다주는 **'납품(SUBMIT)'** 미션은 별도의 GUI 로직이 필요합니다.

1. **트리거:** Citizens NPC 우클릭 -> `MissionManager.openSubmitGui(player, npcId)`
    
2. **GUI 구성:**
    
    - 유저가 아이템을 올릴 수 있는 슬롯 존재.
        
    - '제출' 버튼 클릭.
        
3. **검증 로직:**
    
    - 올려진 아이템이 현재 진행 중인 미션의 `target` 아이템(예: "3성 감자")과 정확히 일치하는지 `ItemManager`를 통해 비교.
        
    - 수량이 `amount` 이상인지 확인.
        
4. **처리:** 아이템 회수(삭제) 후 미션 완료 처리.
    

---

## 6. 개발 시 고려사항 (Tips)

### 6.1. 데이터 초기화 (Daily Reset)

- **문제:** 모든 유저의 일일 미션을 자정(00:00)에 어떻게 초기화할 것인가?
    
- **해결:**
    
    - 서버에 `BukkitRunnable` 스케줄러를 돌려 매분 `LocalTime`을 체크할 수도 있지만, 접속 시 체크하는 것이 효율적입니다.
        
    - **Lazy Reset:** 플레이어가 접속할 때(`PlayerJoinEvent`), 마지막 미션 갱신 날짜(`last_reset`)를 확인합니다. 날짜가 어제라면 그때 DB를 초기화하고 새 미션을 할당합니다.
        

### 6.2. 조건 파서 (Condition Parser)

문자열로 된 조건을 코드로 변환하는 유틸리티 클래스가 필요합니다.

- `String condition = "HP_BELOW:5"`
    
- 파싱: `key = "HP_BELOW"`, `value = 5`
    
- 로직: `if (player.getHealth() <= 5) return true;`
    

### 6.3. 성능 최적화

- `PlayerMoveEvent`에 연결된 `WALK` 미션은 서버 부하의 주범이 될 수 있습니다.
    
- **해결:** 매 틱마다 검사하지 말고, 플레이어가 '청크'를 이동했을 때(`fromChunk != toChunk`)만 `MissionManager`를 호출하도록 리스너 단계에서 컷해야 합니다.
    

이 설계는 기획서(`*.pdf`)에 있는 광부의 "단계별 성장", 농부의 "고품질 납품", 탐험가의 "지역 발견" 등 모든 미션 형태를 **단 하나의 엔진**으로 소화할 수 있게 해줍니다.
<br>
사용자님께서 요청하신 **통합 레벨링 및 보상 처리 (Universal Leveling)** 모듈의 상세 아키텍처 설계안입니다.

이 모듈은 DreamWork 플러그인의 **'중앙 은행(Central Bank)'**이자 **'인사팀(HR Department)'**입니다. 5개 직업(광부, 농부, 어부, 사냥꾼, 탐험가)에서 발생한 모든 성과를 집계하고, 계산하며, 적절한 보상을 분배하는 핵심 제어 장치입니다.

---

# 🏗️ Module: Universal Leveling & Reward System

**"DreamWork Growth Core (DW-GC)"**

## 1. 설계 목적 및 핵심 철학

1. **수식 기반의 성장 (Formula-Driven):** 1레벨부터 100레벨까지의 경험치 요구량을 일일이 적는 비효율을 제거합니다. 수학 공식($y = ax^2 + b$)을 설정 파일에 적으면 엔진이 자동으로 레벨 테이블을 계산합니다.
    
2. **보상 파이프라인의 중앙화:** 모든 직업의 보상 지급 로직을 이 모듈 하나로 통일하여, "주말 경험치 2배 이벤트"나 "VIP 골드 부스트" 같은 기능을 단 한 줄의 설정 변경으로 서버 전체에 적용합니다.
    
3. **타 플러그인 제어 (External Control):** 레벨이 오르면 LuckPerms 그룹을 바꾸거나, Towny 권한을 주는 등 외부 시스템을 자동으로 제어합니다.
    

---

## 2. 데이터 구조 (Data Structure)

### 2.1. 성장 설정 (`jobs/leveling_config.yml`)

성장 곡선과 전역 설정을 정의합니다.

|**속성 (Field)**|**설명**|**예시 값**|
|---|---|---|
|**max_level**|최대 레벨 제한|`100`|
|**exp_formula**|레벨업 요구 경험치 공식 (JS 파싱)|`"100 * (level ^ 1.5) + 50"`|
|**global_multipliers**|서버 전체 배율 (이벤트용)|`exp: 1.0`, `money: 1.0`|
|**level_override**|특정 구간의 요구량 수동 설정 (허들 구간)|`49 -> 50: 99999` (승급 심사)|
|**titles**|레벨 구간별 칭호 (Chat Prefix)|`1~9: "&7[견습]"`, `10~29: "&e[숙련]"`|

### 2.2. 유저 데이터 모델 (`UserJobData`)

DB나 파일에 저장되는 플레이어의 실제 상태(State)입니다.

- `Map<JobType, JobProgress>`
    
    - **`JobProgress` 객체:**
        
        - `int level`: 현재 레벨
            
        - `double currentExp`: 현재 경험치
            
        - `double totalAccumulatedExp`: (통계용) 누적 총 획득 경험치
            

---

## 3. 핵심 로직 흐름 (The Calculation Pipeline)

이 엔진은 **"입력(Action) -> 가공(Calculation) -> 출력(Reward)"**의 3단계 파이프라인으로 작동합니다.

### 3.1. 입력 단계 (Input)

각 직업 리스너(Listener)는 구체적인 보상 계산을 하지 않습니다. 단지 `JobManager`에게 **기본값(Base Value)**만 던집니다.

- 코드 호출 예시:
    
    JobManager.giveReward(player, JobType.MINER, baseExp: 10, baseMoney: 5);
    

### 3.2. 가공 단계 (Calculation & Multipliers)

가장 중요한 부분입니다. 여러 단계의 배율(Multiplier)을 순차적으로 적용합니다.

1. **기본값 로드:** 10 Exp, 5 Money
    
2. **전역 배율 (Global):** 주말 이벤트 2배 -> 20 Exp, 10 Money
    
3. **랭크 배율 (Rank):** LuckPerms 그룹 확인 (`vip` 그룹은 1.2배) -> 24 Exp, 12 Money
    
4. **아이템 배율 (Item/Passive):**
    
    - PDC 체크: "지혜의 반지(exp_boost: 0.1)" 착용 중인가?
        
    - 패시브 스킬: "광부의 통찰력" 활성화 상태인가?
        
    - -> 최종: **26.4 Exp, 12 Money**
        
5. **부스트 아이템 (Consumable):** "경험치 2배 쿠폰" 사용 중? (Session 메모리 체크)
    

### 3.3. 레벨업 처리 로직 (Level Up Logic)

경험치를 더한 후, 레벨업이 발생했는지 확인합니다. **재귀(Recursion)** 혹은 **Loop** 처리가 필수입니다 (한 번에 2업 이상 할 수 있으므로).

1. `currentExp += gainedExp`
    
2. **While** (`currentExp >= requiredExp(nextLevel)`):
    
    - `currentExp -= requiredExp`
        
    - `level++`
        
    - `fireLevelUpEvent(player, job, level)` 호출
        
    - 만약 `level >= max_level`이면 루프 종료 및 경험치 최대치 고정.
        

---

## 4. 보상 집행 및 외부 연동 (Integration & Execution)

레벨업이 확정되거나 보상이 계산되었을 때 실행되는 **Action Layer**입니다.

### 4.1. 경제 시스템 연동 (Vault)

- 계산된 Money를 `Vault` API의 `economy.depositPlayer()`로 입금.
    
- **소수점 처리:** `config.yml` 설정에 따라 소수점 버림/반올림 처리 (Vault는 소수점을 지원하지만, 깔끔한 UI를 위해 정수화를 권장).
    

### 4.2. 권한 및 랭크 연동 (LuckPerms & Console)

특정 레벨 도달 시 **자동으로 권한을 부여**하여 유저의 스펙을 확장합니다.

- **YAML 설정 예시:**
    
    YAML
    
    ```
    rewards:
      level_10:
        commands:
          - "lp user %player% parent add miner_novice"
          - "lp user %player% permission set dreamwork.skill.miner.radar true"
    ```
    
- **활용:**
    
    - Lv.10 달성 시 -> `miner_novice` 그룹 승급 (채팅 칭호 변경).
        
    - Lv.50 달성 시 -> `dreamwork.region.nether` 권한 부여 (네더 월드 입장 허용).
        

### 4.3. 스탯 및 패시브 부여 (Attribute Modifiers)

단순한 숫자 놀음이 아니라, 캐릭터가 실제로 강해지도록 바닐라 속성을 수정합니다.

- **방식:** 레벨업 시 해당 플레이어의 `AttributeInstance`를 영구적으로 수정.
    
- **예시:**
    
    - 광부 Lv.30 -> 공격 속도(Attack Speed) +10%
        
    - 사냥꾼 Lv.50 -> 이동 속도(Movement Speed) +0.05
        
    - 농부 Lv.100 -> 최대 체력(Max Health) +4 (하트 2칸)
        

---

## 5. 시각적 피드백 (Visual Feedback)

유저가 성장하고 있음을 즉각적으로 느끼게 해주는 **UX(User Experience)** 설계입니다.

### 5.1. 실시간 액션바 (Action Bar)

채팅창 도배를 막기 위해, 경험치 획득 로그는 액션바에 띄웁니다.

- **Format:** `&e+24 Exp &6(+12 G) &7| &f광부 Lv.5 &a[▮▮▮▮▯▯▯▯▯▯] (40%)`
    
- **구현 팁:** 매번 문자열을 생성하면 무거우므로, 프로그레스 바 문자열은 미리 캐싱해두거나 10% 단위로 끊어서 생성합니다.
    

### 5.2. 레벨업 축하 (Title & Sound)

- **Title:** 화면 중앙에 크게 `&b&lLEVEL UP!`
    
- **Subtitle:** `&f광부 Lv.5 &7-> &bLv.6 &7(새로운 스킬 해금!)`
    
- **Sound:** `ENTITY_PLAYER_LEVELUP` (피치 1.0 -> 레벨이 높을수록 피치를 높여 웅장하게).
    
- **Firework:** 플레이어 위치에 해당 직업의 상징색(광부=회색, 농부=노란색) 폭죽 발사.
    

---

## 6. 개발 시 고려사항 (Implementation Tips)

1. **수식 파서 (Math Parser):**
    
    - Java 내장 `ScriptEngineManager`(JavaScript)는 Java 15 이후 제거되었습니다.
        
    - 대안: `exp4j` 같은 가벼운 수학 라이브러리를 사용하거나, 간단한 파서(`UnknownExpressionParser`)를 직접 구현해야 합니다. (보안상 라이브러리 추천)
        
2. **비동기 데이터 저장 (Async Saving):**
    
    - 경험치 획득은 초당 수십 번(광역 스킬 등) 일어날 수 있습니다.
        
    - DB 저장은 `LevelUp` 시점에만 하거나, 5분 주기의 **Auto-Save Task**로 미뤄야 합니다. 절대 매 획득 시마다 IO를 발생시키지 마십시오.
        
3. **어뷰징 방지 (Anti-Abuse):**
    
    - **Cooldown:** 동일한 행위에 대해 `0.5초` 내부 쿨타임을 둡니다. (매크로 방지)
        
    - **Daily Cap:** (옵션) 하루에 얻을 수 있는 경험치/돈의 상한선을 설정 파일에서 제한할 수 있게 합니다.
        

이 통합 레벨링 시스템은 DreamWork 서버의 **경제와 성장의 척추** 역할을 하며, 기획자가 언제든지 엑셀 시트(YAML)만 수정하여 서버 밸런스를 조절할 수 있는 강력한 환경을 제공할 것입니다.
<br>
사용자님께서 요청하신 **Development Roadmap (개발 로드맵)**의 상세 계획안입니다.

지금까지 설계한 **Item, Job, GUI, Mission, Leveling** 모듈들이 서로 의존성(Dependency)을 가지고 있기 때문에, **개발 순서가 매우 중요**합니다. 뒤죽박죽 개발하면 "아이템이 없어서 테스트를 못 함" 또는 "데이터 저장이 안 돼서 레벨이 초기화됨" 같은 상황이 발생합니다.

가장 효율적이고 안전한 **6단계(Phase) 개발 공정**을 제안합니다.

---

# 🚀 DreamWork Development Roadmap

**"From Skeleton to Ecosystem"**

## 🗓️ Phase 1: The Foundation (뼈대와 데이터)

**목표:** 플러그인이 서버에서 오류 없이 켜지고, 설정 파일을 읽고, 유저 데이터를 저장할 수 있는 상태.

### 1.1. Project Setup (환경 구축)

- **Maven/Gradle:** `spigot-api`, `Vault`, `PlaceholderAPI`, `Towny` 의존성 추가.
    
- **Package Structuring:**
    
    - `core` (Main, Config, UserData)
        
    - `modules` (Items, Jobs, Missions, GUI)
        
    - `utils` (Math, String, ItemBuilder)
        
    - `hooks` (Vault, Towny)
        

### 1.2. Configuration Manager (설정 로더)

- **기능:** `plugins/DreamWork` 폴더 내의 폴더 구조(`items/`, `jobs/`, `gui/`)를 자동으로 생성.
    
- **로직:**
    
    - `config.yml` 로드 (Debug 모드, DB 설정).
        
    - YAML 파일 파싱 에러 처리 (문법 틀려도 서버 안 꺼지게 예외 처리).
        

### 1.3. User Data Manager (데이터 입출력)

- **기능:** 플레이어 접속(`Join`) 시 데이터를 메모리에 올리고, 나갈 때(`Quit`) 파일/DB에 저장.
    
- **구현:**
    
    - `UserData` 클래스 설계 (Map<JobType, JobData> 보유).
        
    - **Async I/O:** 파일 입출력은 반드시 **비동기 스레드**에서 처리 (서버 렉 방지).
        
    - `Auto-Save Task`: 5분마다 변경된 데이터 자동 저장.
        

---

## 🛠️ Phase 2: The Object Engine (재료 준비)

**목표:** 게임 내에서 사용할 커스텀 아이템을 정의하고 소환할 수 있는 상태. **(Job 개발의 선행 조건)**

### 2.1. Item Manager (DW-IE) 구현

- **템플릿 로더:** `items/*.yml`을 읽어 메모리에 캐싱.
    
- **Item Builder:** ID를 입력하면 `ItemStack`을 뱉어내는 공장 구현.
    
    - PDC(PersistentDataContainer) 주입 로직 구현.
        
    - `/dw give <id>` 명령어 개발 (테스트용).
        
- **Item Identifier:** `ItemStack`을 넣으면 ID를 반환하는 판독기 구현.
    

### 2.2. Debug Tools (검증 도구)

- 아이템의 NBT/PDC 태그를 채팅창에 보여주는 `/dw check` 명령어 구현.
    
- 이 단계에서 아이템의 이름, 로어, 모델 데이터가 정상적으로 나오는지 확인.
    

---

## ⚔️ Phase 3: The Job Gameplay (핵심 로직)

**목표:** 플레이어가 행동(채광, 농사 등)을 했을 때 보상이 계산되고 지급되는 상태.

### 3.1. Reward Calculator (보상 처리기)

- **Job Template:** `jobs/*.yml`을 읽어 보상 테이블(행동 -> 보상) 캐싱.
    
- **Calculator:** 기본 보상 + 배율(랭크, 아이템) 계산 로직.
    
- **Action Bar Feedback:** 보상 획득 시 화면 하단에 `+10 Exp` 뜨게 구현.
    

### 3.2. Job Listeners (직업별 리스너 순차 개발)

난이도가 낮은 순서대로 개발하여 안정성을 확보합니다.

1. **Miner (광부):** `BlockBreakEvent` 연결. `placed_by_player` 태그 체크 로직 필수.
    
2. **Hunter (사냥꾼):** `EntityDeathEvent` 연결. 스포너 몹(`SpawnReason`) 필터링 구현.
    
3. **Farmer (농부):** `Ageable` 체크(다 자랐는가?) 및 확률적 등급 작물 드롭 구현.
    
4. **Fisher (어부):** `PlayerFishEvent`. 미끼 아이템 감지 및 커스텀 물고기 드롭.
    
5. **Adventurer (탐험가):** **(가장 어려움)** `PlayerMoveEvent` 최적화(청크 단위 검사) 및 구조물 감지 비동기 처리.
    

### 3.3. Universal Leveling (레벨업)

- 경험치 획득 시 레벨업 공식 체크.
    
- 레벨업 시 `LevelUpEvent` 발생 -> Title 메시지 출력 및 사운드 재생.
    

---

## 🖥️ Phase 4: The Interface (시각화)

**목표:** 명령어가 아닌 GUI로 정보를 확인하고 상호작용하는 상태.

### 4.1. GUI Framework (DW-DE)

- `DreamGui` 추상 클래스 및 `GuiManager` 구현.
    
- YAML 기반 인벤토리 배치 로직 (좌표 계산).
    
- 클릭 이벤트 핸들러 (아이템 빼가기 방지, Action 문자열 파싱).
    

### 4.2. UI Implementation (화면 제작)

- **Main Dashboard:** 내 정보, 직업 레벨 표시.
    
- **Job Detail:** 각 직업별 상세 능력치 표시.
    
- **Interactive UI:** 농부의 '요리 솥', 광부의 '감정소' 등 아이템을 넣고 빼는 기능 구현.
    

---

## 📜 Phase 5: The Motivation (미션 및 목적)

**목표:** 플레이어에게 퀘스트를 부여하고 달성 시 보상을 주는 상태.

### 5.1. Mission Engine (DW-QC)

- **Template Loader:** `missions/*.yml` 로드.
    
- **Progress Tracker:** 유저의 미션 진행도(카운트) 관리.
    
- **Condition Parser:** "사막 바이옴에서", "밤에만" 같은 조건 처리 로직.
    

### 5.2. Integration with Listeners

- Phase 3에서 만든 Job Listener들에 **미션 카운팅 코드 한 줄 추가**.
    
    - _예:_ `MissionManager.processEvent(player, Type.BREAK, blockType);`
        
- NPC 납품 시스템(Citizens 연동) 구현.
    

---

## 🔗 Phase 6: Integration & Polish (연동 및 마감)

**목표:** 외부 플러그인과 연결하고, 상용화 수준으로 다듬는 단계.

### 6.1. External Hooks

- **Vault:** `Reward Calculator`에서 실제 돈 지급 연동.
    
- **PlaceholderAPI:** `%dreamwork_miner_level%` 같은 변수 등록. (스코어보드용)
    
- **Towny:** 타운 내부/외부 판별 로직 적용.
    

### 6.2. Balancing & Optimization

- **Lag Check:** `Timings` 리포트를 보며 탐험가/농부 리스너 부하 확인.
    
- **Config Calibration:** 실제 서버를 열고 광물을 캐보며 경험치/돈 수치 조절.
    
- **Message Localization:** `messages.yml`의 모든 텍스트가 예쁘게 출력되는지 확인.
    

---

## 📅 권장 개발 스케줄 (Estimated Timeline)

|**단계**|**예상 소요 시간**|**핵심 산출물**|**비고**|
|---|---|---|---|
|**Phase 1 (기반)**|3~5일|`/dw reload`, 데이터 저장 파일 생성|가장 지루하지만 가장 중요함|
|**Phase 2 (아이템)**|3~4일|`/dw give`, 아이템 YAML 스펙 확정|기획자와 협업 필요 (YAML 작성)|
|**Phase 3 (직업)**|7~10일|직업별 경험치/돈 획득, 레벨업|**가장 많은 시간 소요**|
|**Phase 4 (GUI)**|4~5일|대시보드 오픈, 클릭 액션 작동|디자인 감각 필요|
|**Phase 5 (미션)**|5~7일|미션 수주/완료, 연계 퀘스트 작동|로직 복잡도 높음|
|**Phase 6 (마감)**|3~5일|PAPI 변수, 랭크 연동, 버그 수정|실제 플레이 테스트 필수|

### 💡 개발자의 팁 (Pro Tips)

1. **Phase 1, 2는 완벽하게:** 기반이 흔들리면 나중에 미션 만들 때 다 뜯어고쳐야 합니다. 데이터 구조와 아이템 식별 로직은 시간을 들여서라도 탄탄하게 짜세요.
    
2. **Mocking:** Phase 3(직업)를 만들 때, GUI가 없어서 불편하다면 임시 명령어(`/dw debug exp add <amount>`)를 먼저 만들어두고 테스트하세요.
    
3. **기획 병행:** 개발자가 Phase 1 코딩을 하는 동안, 기획자는 `items/*.yml`과 `jobs/*.yml` 데이터를 엑셀질 하듯 미리 작성해둬야 공백기가 생기지 않습니다.