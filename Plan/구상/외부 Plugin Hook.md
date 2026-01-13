사용자님께서 제공해주신 `Plan2.0` 및 세부 기획 문서(`명령.txt` 등)를 바탕으로, **DreamWork 플러그인**이 외부 플러그인들과 유기적으로 작동하기 위한 **필수 외부 플러그인 목록**과 **연동 아키텍처(구조)**를 설계해 드립니다.

코드를 제외하고, **"논리적인 연결 흐름"**과 **"구조적 설계"** 위주로 설명해 드리겠습니다.

---

### 1. 외부 라이브러리(Plugin) 선정 및 역할 정리

`명령.txt`와 기획서에 따르면, DreamWork가 의존해야 할 핵심 외부 플러그인은 다음 5가지입니다.

|**플러그인 명**|**역할 (Role)**|**DreamWork와의 관계**|
|---|---|---|
|**Vault**|**경제(Economy) 표준 API**|DreamWork에서 번 돈을 지급하거나, 업그레이드 비용을 차감하는 **"은행 창구"** 역할입니다. 자체 상점 플러그인과도 돈을 공유하게 해주는 다리입니다.|
|**Towny**|**마을 및 영토 관리**|"특정 직업은 타운 내에서만 활동 가능" 등의 제약을 걸기 위한 **"지역 판별기"**입니다. (예: 농부는 농경지 Plot에서만 작물 성장)|
|**LuckPerms**|**권한 및 랭크 관리**|직업 레벨업 시 칭호(Prefix)를 변경하거나, 특정 스킬 사용 권한을 부여하는 **"인사팀"** 역할입니다.|
|**Citizens**|**NPC 생성 및 관리**|퀘스트 수주, 물고기 납품, 전리품 교환 등을 담당하는 NPC와의 **"상호작용 트리거"**입니다.|
|**PlaceholderAPI** (PAPI)|**데이터 시각화**|스코어보드나 채팅창에 `%dreamwork_miner_level%` 처럼 플레이어의 정보를 띄워주는 **"전광판"** 역할입니다.|

---

### 2. DreamWork와의 연동 구조 설계 (Integration Architecture)

DreamWork 플러그인 내부에는 외부 플러그인을 관리하는 **`HookManager` (또는 `DependencyManager`)** 모듈을 두어 관리하는 것이 유지보수에 좋습니다.

#### A. Vault (경제 시스템 연동)

- **연동 방식:** `ServiceManager` (서비스 매니저) 패턴
    
- **구동 원리:**
    
    1. 서버가 켜지면(`onEnable`), DreamWork는 서버의 `ServicesManager`에게 "지금 경제(Economy)를 담당하는 플러그인이 누구냐?"라고 물어봅니다.
        
    2. Vault가 연결된 경제 플러그인(보통 EssentialsX 등)을 알려줍니다.
        
    3. DreamWork는 이 연결 객체를 `economy` 변수에 저장해둡니다.
        
- **활용 시나리오:**
    
    - **수익 창출:** 광부가 광물을 캐면 `economy.depositPlayer(player, amount)`를 호출하여 돈을 입금합니다.
        
    - **비용 지불:** 직업 승급 심사 시 `economy.has(player, cost)`로 잔액을 확인하고 `withdrawPlayer`로 차감합니다.
        

#### B. Towny (타운 및 영토 연동)

- **연동 방식:** API 직접 호출 및 정적(Static) 메소드 활용
    
- **구동 원리:**
    
    - Towny는 전역적으로 접근 가능한 `TownyUniverse`나 `TownBlock` 객체를 제공합니다.
        
- **활용 시나리오 (이벤트 리스너 내):**
    
    - **농부의 경우:** 플레이어가 씨앗을 심으려고 할 때(`BlockPlaceEvent`), 해당 좌표(`Location`)를 Towny API에 던집니다.
        
    - **판별 로직:**
        
        1. "이 좌표에 타운이 존재하는가?" (`hasTown`)
            
        2. "이 땅의 타입이 '농경지(Farm Plot)'인가?" (`getType`)
            
        3. 조건이 맞지 않으면 `event.setCancelled(true)`로 행동을 막고 메시지를 띄웁니다.
            
    - **혜택:** 타운 소속일 경우 직업 활동 시 추가 경험치 보너스 로직을 넣을 수 있습니다.
        

#### C. LuckPerms (권한 및 랭크 연동)

- **연동 방식:** Provider API (`LuckPermsProvider`)
    
- **구동 원리:**
    
    - LuckPerms API를 통해 특정 유저(`User`)의 데이터에 접근하여 노드(Node)를 수정합니다.
        
- **활용 시나리오:**
    
    - **직업 레벨업:** 플레이어가 '광부 Lv.10' 달성 시, DreamWork는 LuckPerms에게 요청하여 `dreamwork.miner.lv10` 퍼미션을 부여합니다.
        
    - **칭호 변경:** 메타 데이터(Meta Data)를 수정하여 채팅 칠 때 이름 앞에 `[숙련 광부]` 같은 접두사가 붙도록 설정합니다.
        
    - **스킬 해금:** 특정 스킬 명령어 실행 시, DreamWork 내부 로직 대신 LuckPerms 권한(`dreamwork.skill.doublejump`) 보유 여부를 체크합니다.
        

#### D. Citizens (NPC 상호작용)

- **연동 방식:** 이벤트 리스너 (`NPCRightClickEvent`)
    
- **구동 원리:**
    
    - Citizens 플러그인은 NPC를 우클릭했을 때 별도의 이벤트를 발생시킵니다.
        
- **활용 시나리오:**
    
    1. DreamWork 설정 파일(`config.yml`)에 '광부 퀘스트 NPC 번호: 5'라고 저장해둡니다.
        
    2. 플레이어가 NPC를 우클릭하면 DreamWork가 이벤트를 가로챕니다.
        
    3. 클릭된 NPC의 ID가 '5'인지 확인합니다.
        
    4. 맞다면 **DreamWork 자체 GUI(퀘스트/상점 창)**를 플레이어에게 띄워줍니다.
        
    
    - _핵심:_ NPC는 껍데기일 뿐이고, 실제 기능(GUI, 대화)은 DreamWork가 처리합니다.
        

#### E. PlaceholderAPI (정보 출력)

- **연동 방식:** `PlaceholderExpansion` 클래스 상속
    
- **구동 원리:**
    
    - DreamWork 내부에 PAPI 규격을 따르는 확장 클래스를 하나 만듭니다.
        
- **활용 시나리오:**
    
    - 외부(스코어보드, 탭 리스트)에서 `%dreamwork_miner_level%`이라는 요청이 들어옵니다.
        
    - 이 요청을 DreamWork가 받아서, 해당 플레이어의 PDC(PersistentDataContainer)나 메모리에 저장된 레벨 값을 찾아서 숫자(String)로 반환해줍니다.
        
    - 이를 통해 유저는 자신의 직업 레벨이나 현재 미션 상태를 채팅창이나 보드에서 볼 수 있습니다.
        

---

### 3. 전체 시스템 구조도 (System Architecture)

이 모든 것을 통합한 DreamWork 플러그인의 내부 구조는 다음과 같이 구성하는 것을 추천합니다.

Plaintext

```
[DreamWork Plugin Core]
│
├── onEnable()
│    ├── 1. Config 로드
│    ├── 2. HookManager 초기화 (외부 플러그인 연결 시도)
│    └── 3. EventListener & Command 등록
│
├── [Managers]
│    ├── JobManager (직업 레벨/경험치 계산)
│    ├── MissionManager (미션 진행도 체크)
│    └── HookManager (★ 핵심: 외부 플러그인과의 통신 담당)
│         ├── VaultHook (경제)
│         ├── TownyHook (지역)
│         ├── LuckPermsHook (권한)
│         └── CitizensHook (NPC)
│
└── [Expansion]
     └── PAPIExpansion (외부로 데이터 송출)
```

### 4. 개발 시 팁 (Best Practices)

1. **Soft Dependency (유연한 의존성):**
    
    - `plugin.yml`에 `depend: [Vault]` 대신 `softdepend: [Towny, PlaceholderAPI]` 등으로 설정하세요.
        
    - 만약 Towny가 없는 서버(야생 서버)에서 DreamWork를 돌릴 경우, `TownyHook`이 활성화되지 않도록 예외 처리를 하면 플러그인이 오류 없이 작동합니다(이 경우 타운 제한 기능만 꺼짐).
        
2. **추상화 (Abstraction):**
    
    - 나중에 Towny 대신 Lands나 GriefPrevention을 쓸 수도 있습니다. 따라서 `RegionProvider`라는 인터페이스를 만들고, `TownyHook`이 이를 구현하게 하면 나중에 플러그인을 교체하기 매우 쉽습니다.
        
3. **자체 상점(EconomyShop)과의 연결:**
    
    - 나중에 만드실 상점 플러그인도 **Vault**를 지원하도록 만들거나, DreamWork API를 통해 직접 통신하면 됩니다. 현재 단계에서는 **Vault**를 통해 돈이 오가는 길만 뚫어놓으면 완벽하게 대비가 됩니다.
        

이 구조로 개발을 시작하시면, 각 플러그인이 독립적이면서도 유기적으로 연결된 **Plan 2.0**의 시스템을 안정적으로 구현하실 수 있습니다.



사용자님의 요청대로 **Vault(경제 시스템)**와의 연동 구조를 아주 상세하게, **논리적 흐름**과 **아키텍처** 관점에서 깊이 있게 구상해 드립니다.

사용자님께서 **"DreamWork에서 돈(Balance)을 직접 관리하고 싶다"**고 하셨으므로, 단순히 Vault를 사용하는 단계를 넘어, **DreamWork가 Vault의 경제 시스템을 어떻게 장악(Provider)하거나 활용하는지** 두 가지 관점에서 설계해 드립니다.

---

### 1. Vault 연동의 핵심 개념 (The Concept)

Vault는 돈을 직접 들고 있는 '은행'이 아니라, **"은행 창구(Interface)"**입니다.

서버에는 다음과 같은 3계층 구조가 형성됩니다.

1. **클라이언트 (Client):** 돈을 쓰거나 벌고 싶은 플러그인 (**DreamWork**, 자체 상점 등)
    
2. **중개자 (Interface):** 표준 규격 (**Vault**)
    
3. **공급자 (Provider):** 실제 돈 데이터를 저장하고 관리하는 플러그인 (**EssentialsX** 또는 **DreamWork 자체**)
    

---

### 2. 아키텍처 설계: DreamWork와 Vault의 통신 구조

이 구조는 DreamWork가 **"돈 계산을 요청하는 입장(Consumer)"**일 때와 **"돈을 직접 관리하는 입장(Provider)"**일 때로 나뉩니다. 사용자님의 기획 의도(자체 관리)를 반영하여 **Provider 구조**까지 포함했습니다.

#### A. 논리적 계층 구조도

코드 스니펫

```
graph TD
    subgraph "DreamWork Plugin"
        Job[직업 시스템] --> Hook[Vault Hook Manager]
        Shop[자체 상점] --> Hook
    end

    Hook --"1. 입/출금 요청"--> VaultAPI[Vault (API Layer)]
    
    VaultAPI --"2. 요청 전달"--> Provider[Economy Provider]
    
    subgraph "Data Storage"
        Provider --"3. 데이터 수정"--> BalanceData[(유저 돈 데이터)]
    end
    
    style Hook fill:#f9f,stroke:#333,stroke-width:2px
    style VaultAPI fill:#bbf,stroke:#333,stroke-width:2px
    style Provider fill:#dfd,stroke:#333,stroke-width:2px
```

---

### 3. 상세 기능 명세 (Logic Flow)

DreamWork 내부의 `VaultHook` 클래스가 수행해야 할 논리적 절차입니다.

#### ① 초기화 단계 (The Handshake)

서버가 켜질 때 (`onEnable`), DreamWork는 Vault와 악수를 해야 합니다.

1. **탐색 (Lookup):** 서버의 서비스 매니저(`ServicesManager`)에게 _"지금 등록된 경제(Economy) 플러그인이 누구냐?"_ 라고 방송합니다.
    
2. **검증 (Validation):**
    
    - 응답이 없다면? → 경제 기능 비활성화 또는 에러 로그 출력 (서버 닫힘 방지).
        
    - 응답이 있다면? → 해당 객체(`Economy Provider`)를 `HookManager` 변수에 저장.
        
3. **연결 확정:** 이후의 모든 돈 관련 명령은 이 변수를 통해 수행됩니다.
    

#### ② 입금 프로세스 (Deposit Flow) - 예: 광부 광물 판매

광부가 금을 팔았을 때의 흐름입니다.

1. **요청:** `MinerClass`가 `VaultHook.deposit(player, 1000)`을 호출.
    
2. **검사:** `VaultHook`은 `Vault`가 정상 연결되어 있는지 확인.
    
3. **실행:** `Vault`를 통해 실제 잔액을 `+1000` 수정.
    
4. **피드백:**
    
    - 성공 시: `EconomyResponse.Success` 반환 → 채팅창에 "1000D 획득" 메시지 출력.
        
    - 실패 시: (예: 통장 한도 초과 등) 에러 메시지 출력.
        

#### ③ 출금 프로세스 (Withdraw Flow) - 예: 직업 승급 심사

플레이어가 50,000D를 내고 레벨업을 하려고 할 때입니다.

1. **잔액 확인 (Pre-check):**
    
    - 무작정 돈을 빼지 않습니다. `economy.has(player, 50000)` 메소드로 잔액이 충분한지 **먼저 조회**합니다.
        
    - 돈이 부족하면? → "돈이 부족합니다" 메시지 띄우고 로직 즉시 종료 (`return`).
        
2. **출금 실행:**
    
    - 잔액이 충분하면 `economy.withdrawPlayer(player, 50000)` 실행.
        
3. **트랜잭션 확인:**
    
    - 만약 알 수 없는 오류로 출금에 실패했다면? → 승급 로직을 취소해야 합니다 (돈은 안 나갔는데 레벨업 되면 안 되니까).
        
4. **최종 승인:** 출금 성공 응답을 받으면 그때 `JobManager`가 레벨을 +1 올립니다.
    

---

### 4. 심화 설계: DreamWork가 직접 '돈'을 관리하려면? (Custom Provider)

사용자님께서 **"돈을 관리하는 시스템을 DreamWork에서 구현해야 함"**이라고 하셨습니다.

이 경우, DreamWork는 Vault에게 돈을 달라고 하는 게 아니라, **"내가 바로 은행이다"**라고 선언해야 합니다.

이 구조를 구현하기 위한 **논리적 단계**는 다음과 같습니다.

1. **Vault 인터페이스 구현 (Implementation):**
    
    - DreamWork 내부에 `AbstractEconomy` 같은 Vault의 표준 규격 문서를 가져와서 빈칸 채우기를 합니다.
        
    - 예: `getBalance(player)`라는 빈칸에 → "내 `player_data.yml`이나 `PDC`에서 돈 데이터 꺼내오기" 로직을 채워 넣습니다.
        
2. **서비스 등록 (Registration):**
    
    - DreamWork가 켜질 때(`onEnable`), 서버에게 이렇게 말합니다.
        
    - _"이제부터 이 서버의 경제(Economy) 담당자는 나(DreamWork)입니다. 다른 플러그인들이 돈 찾으면 저한테 보내세요."_
        
    - 코드로 `ServicePriority.Highest`를 설정하여 우선권을 가져갑니다.
        
3. **결과:**
    
    - 이렇게 하면 나중에 만드실 **[상점 플러그인]**이나 타운 플러그인(**Towny**)이 DreamWork가 만든 돈 시스템을 자연스럽게 사용하게 됩니다.
        
    - Towny가 세금을 걷을 때, DreamWork의 데이터베이스에서 돈이 빠져나갑니다.
        

---

### 5. 추천하는 개발 방향 (Conclusion)

**초기 개발 단계**에서는 **B안(직접 구현)**보다는 **A안(연동 사용)**을 강력하게 추천합니다.

- **이유 1 (안정성):** 돈과 관련된 로직(동시성 문제, 소수점 오류 등)은 매우 민감합니다. 이미 검증된 **EssentialsX** 같은 플러그인을 'Provider'로 두고, DreamWork는 'Hook'을 통해 명령만 내리는 구조가 훨씬 안전하고 개발 속도가 빠릅니다.
    
- **이유 2 (호환성):** 상점 플러그인, Towny, LuckPerms 등은 이미 EssentialsX 기반 경제와 완벽하게 호환됩니다. DreamWork가 직접 돈을 관리하려면 이 호환성을 일일이 맞춰줘야 하는 번거로움이 있습니다.
    

**결론적 제안:**

1. **1단계:** **Vault Hook** 구조만 만드세요. (EssentialsX 등을 깔아서 돈 저장소로 사용)
    
2. **2단계:** DreamWork의 기능이 안정화되면, 그때 Hook 내부 로직을 수정하여 DreamWork가 직접 돈을 관리하도록 바꿔도 늦지 않습니다. (Hook 구조 덕분에 코드를 많이 뜯어고칠 필요가 없습니다.)



**Towny (타운 및 영토 연동) 상세 구상안**

사용자님의 **Plan 2.0** 철학인 "야생에서의 노동(Work)과 타운에서의 삶(Dream)"을 실현하기 위해, Towny는 단순한 땅 보호 플러그인이 아닌 **"직업 활동의 무대이자 제약 장치"**로 기능해야 합니다.

DreamWork 플러그인이 Towny를 어떻게 감지하고, 활용하고, 제어할지 **논리적 구조**와 **기능적 흐름**을 구체화해 드립니다.

---

### 1. 연동의 핵심 철학: "Zoning (용도 구역)" 시스템

DreamWork는 Towny의 **Plot Type(토지 용도)** 정보를 읽어와서, 해당 땅이 어떤 직업에게 유리한지(또는 불리한지)를 판단합니다.

|**Towny Plot Type**|**DreamWork 판정**|**직업별 영향 (예시)**|
|---|---|---|
|**Wilderness (야생)**|**자원 채취 구역**|광부/탐험가/사냥꾼: 효율 100%<br><br>  <br><br>농부: 효율 50% (서리 방지 명목)|
|**Resident Plot (주거)**|**사유지**|모든 직업 스킬 사용 불가 (휴식 공간)|
|**Farm Plot (농경지)**|**농업 특화 구역**|**농부:** 성장 속도 2배, 수확량 보너스<br><br>  <br><br>타 직업: 농사 불가능|
|**Arena (투기장)**|**전투 구역**|**사냥꾼:** PvP/PvE 스킬 쿨타임 감소|
|**Commercial (상가)**|**경제 구역**|**공통:** NPC와 거래 가능, 개인 상점 개설 가능|

---

### 2. 직업별 구체적 연동 시나리오

#### A. 농부 (Farmer) - "대농장 시스템"

Towny 연동이 가장 강력하게 들어가는 직업입니다. 야생 난개발을 막고 타운 농장을 유도합니다.

- **로직 흐름:**
    
    1. 플레이어가 괭이질을 하거나 씨앗을 심으려 함 (`BlockPlaceEvent`).
        
    2. DreamWork가 좌표를 확인: `TownyHook.isFarmPlot(location)`.
        
    3. **True일 경우:** 정상 설치 + "비옥한 토지입니다!" 메시지 출력.
        
    4. **False일 경우:**
        
        - 야생이라면: "야생에서는 작물이 잘 자라지 않습니다." (성장 속도 50% 너프 적용).
            
        - 일반 도로/주거지라면: "이곳은 농경지가 아닙니다." (설치 취소 `event.setCancelled(true)`).
            

#### B. 광부/탐험가 (Miner/Adventurer) - "도시 보호 및 가공"

이들은 주로 타운 밖에서 활동하지만, 타운 안에서는 **"안전 장치"**가 작동해야 합니다.

- **안전 장치:** 광부의 `광맥 탐지`나 `드릴` 같은 스킬이 타운 내 건축물을 부수지 않도록 방지합니다.
    
- **가공소 혜택:** 광부가 타운 내 **'대장간(특정 좌표 혹은 Commercial Plot)'**에서 광물을 구우면, Towny의 소속 여부를 체크하여 수수료를 할인해줍니다.
    

#### C. 타운 혜택 (Citizenship Benefit)

자신이 소속된 타운 영토 내에서 활동할 때 **'애향심(Patriotism)'** 버프를 부여합니다.

- **조건:** `TownyHook.isMyTown(player, location)` == `true`
    
- **효과:** 직업 경험치 획득량 +10%, 배고픔 감소 속도 저하.
    

---

### 3. 기술적 구현 구조 (Technical Architecture)

Towny API를 직접 `JobClass`에 넣으면 Towny가 없을 때 에러가 납니다. 반드시 **Wrapper Class(포장 클래스)**를 만들어야 합니다.

#### [Structure: TownyHook.java]

이 클래스는 DreamWork와 Towny 사이의 통역사입니다.

Java

```
// 의사 코드 (Pseudo Logic)
public class TownyHook {
    
    private boolean isEnabled = false;

    // 초기화: Towny 플러그인이 있는지 확인
    public TownyHook() {
        if (Bukkit.getPluginManager().getPlugin("Towny") != null) {
            this.isEnabled = true;
        }
    }

    // 1. 특정 위치가 '농경지(Farm)'인지 확인
    public boolean isFarmPlot(Location loc) {
        if (!isEnabled) return true; // Towny 없으면 그냥 허용(야생 서버)
        
        try {
            TownBlock tb = TownyAPI.getInstance().getTownBlock(loc);
            return tb != null && tb.getType() == TownBlockType.FARM;
        } catch (Exception e) { return false; }
    }

    // 2. 플레이어가 이 땅의 소유주(혹은 소속 타운원)인지 확인
    public boolean canBuild(Player player, Location loc) {
        // Towny의 Utils.getCanBuild() 등을 호출하여 권한 체크 대행
    }

    // 3. 특정 위치의 타운 이름 가져오기 (GUI 표시용)
    public String getTownName(Location loc) {
        // ...
    }
}
```

#### [Integration: Listener 적용 예시]

Java

```
// FarmerListener.java 내부
@EventHandler
public void onPlant(BlockPlaceEvent event) {
    // 1. DreamWork 내부 로직: 농작물인지 확인
    if (!JobManager.isCrop(event.getBlock())) return;

    // 2. Towny Hook 호출 (★ 핵심)
    // "농경지가 아니면 심지 못하게 해줘"
    if (!DreamWork.getTownyHook().isFarmPlot(event.getBlock().getLocation())) {
        event.getPlayer().sendMessage("§c작물은 [농경지] 구역에만 심을 수 있습니다!");
        event.setCancelled(true);
        return;
    }
    
    // 3. 성공 로직 진행...
}
```

---

### 4. 경제 순환 모델 (The Economic Loop)

DreamWork와 Towny, Vault가 맞물려 돌아가는 구조입니다.

1. **세금 납부 (Tax):**
    
    - Towny는 매일 자정 Vault를 통해 유저의 돈을 걷어갑니다(유지비).
        
    - 유저는 세금을 내기 위해 **DreamWork 직업 활동(광질, 농사)**을 해야 합니다.
        
2. **타운 금고 기부 (Deposit):**
    
    - DreamWork의 `/dw donate [금액]` 명령어를 만들 수 있습니다.
        
    - 이 명령어를 치면 플레이어의 지갑(Vault)에서 돈을 빼서, 소속 타운의 계좌(Towny Account)로 바로 입금해줍니다. (Towny API 활용)
        
3. **직업 길드 창설:**
    
    - Towny의 **'Nation(국가)'** 기능을 활용하여, 특정 국가는 "광부 연합", "농부 조합" 컨셉을 잡도록 유도할 수 있습니다.
        

### 5. 요약 및 제안

1. **DreamWork**는 **TownyHook**이라는 클래스를 통해서만 Towny와 대화합니다. (의존성 분리)
    
2. **농부**는 **Farm Plot** 강제화를 통해 타운의 필요성을 극대화합니다.
    
3. **GUI**나 **스코어보드**에 현재 서 있는 땅이 "광질 가능 구역"인지 "농사 구역"인지 표시해주면 유저 편의성이 크게 오릅니다.
    

이 구조로 개발하시면, Towny 플러그인을 단순한 땅따먹기가 아닌 **"직업 활동의 법적 테두리"**로 완벽하게 활용하실 수 있습니다.


**LuckPerms (권한 및 랭크 연동) 상세 구상안**

DreamWork 시스템에서 LuckPerms는 단순한 관리자 권한 부여 도구가 아니라, **"플레이어의 성장(Job Level)과 신분(Citizen Rank)을 증명하는 디지털 신분증"** 역할을 수행해야 합니다.

사용자님의 **Plan 2.0**에 맞춰, DreamWork가 LuckPerms를 어떻게 제어하고 활용할지 **[권한 구조], [랭크 계층], [데이터 흐름]** 3단계로 구체화해 드립니다.

---

### 1. 권한 노드 설계 (Permission Node Structure)

DreamWork 플러그인이 LuckPerms에 던져줄 **"명령어(Node)"**의 체계입니다. 주먹구구식이 아닌, 체계적인 트리를 짜야 관리하기 편합니다.

|**카테고리**|**노드 패턴 (Node Pattern)**|**용도 및 예시**|
|---|---|---|
|**직업 스킬**|`dreamwork.skill.<job>.<skill_name>`|특정 스킬 사용 권한<br><br>  <br><br>예: `dreamwork.skill.miner.oreradar` (광맥 탐지 사용 가능)|
|**패시브 능력**|`dreamwork.passive.<ability>.<level>`|능력치 패시브<br><br>  <br><br>예: `dreamwork.passive.speed.2` (이동속도 2단계 적용)|
|**상호작용**|`dreamwork.interact.<entity_type>`|특정 NPC나 기계 사용 권한<br><br>  <br><br>예: `dreamwork.interact.machine.blast_furnace`|
|**관리자**|`dreamwork.admin.<command>`|OP 명령어 권한<br><br>  <br><br>예: `dreamwork.admin.setlevel`|

---

### 2. 하이브리드 랭크 시스템 (Hybrid Rank Architecture)

DreamWork는 **"시민 등급(공통)"**과 **"직업 레벨(개별)"** 두 가지 척도가 존재합니다. 이를 LuckPerms로 구현하려면 **Group(그룹)**과 **Meta(메타)**를 분리해서 운용해야 합니다.

#### A. 시민 등급 = "LuckPerms Group" 사용

공통 미션(Plan 2.0 공통.pdf)을 통해 올라가는 등급은 **LP 그룹**으로 관리합니다.

- **구조:** `default` (방랑자) → `resident` (거주민) → `citizen` (시민) → `noble` (귀족)
    
- **LP 적용:** `parent set` 기능을 이용해 상속 구조를 만듭니다.
    
    - `citizen` 그룹은 `resident`의 모든 권한을 상속받습니다.
        
    - **DreamWork 역할:** 공통 미션 완료 시 `user.setPrimaryGroup("citizen")` 명령을 LP에 보냅니다.
        

#### B. 직업 레벨 = "LuckPerms Meta & Suffix" 사용

직업 레벨(1~100)은 너무 많아서 그룹으로 만들면 관리가 불가능합니다. 이는 **메타 데이터(Meta Data)**로 처리합니다.

- **칭호(Prefix):** 채팅 칠 때 이름 앞에 붙는 태그.
    
    - 예: `[광부 Lv.50]`, `[전설의 농부]`
        
- **가중치(Weight):** 여러 직업 중 가장 높은 직업을 대표 칭호로 보여줄 때 사용.
    
- **DreamWork 역할:**
    
    - 레벨업 시: LP에게 `meta.setPrefix("[광부 Lv." + level + "]")` 데이터를 전송합니다.
        

---

### 3. 논리적 데이터 흐름 (Logic Flow)

DreamWork 내부의 `LuckPermsHook` 클래스가 이벤트를 처리하는 순서입니다.

#### 시나리오: 광부 레벨업 (Lv.9 → Lv.10)

1. **Event 발생:** 플레이어가 경험치를 채워 레벨업 조건 달성.
    
2. **DreamWork 내부 처리:** DB(또는 파일)에 레벨 10 저장.
    
3. **Hook 호출:** `luckPermsHook.updateUserRank(player, "miner", 10);`
    
4. **LuckPerms 제어 (비동기 처리):**
    
    - **Step 1 (권한 해금):** Lv.10에 해금되는 스킬이 있다면?
        
        - `user.data().add(Node.builder("dreamwork.skill.miner.deep_mining").build());`
            
    - **Step 2 (칭호 변경):** 칭호 업데이트.
        
        - `user.data().add(PrefixNode.builder("[광부 Lv.10] ", priority).build());`
            
    - **Step 3 (저장):** `luckPerms.getUserManager().saveUser(user);`
        
5. **결과:** 플레이어가 채팅을 치면 `[광부 Lv.10] 닉네임: 안녕하세요`가 출력됨.
    

---

### 4. 고급 기능: 컨텍스트(Context) 활용

DreamWork의 야심 찬 기능인 **"지역별/월드별 차등 적용"**을 LP의 Context 기능으로 구현합니다.

- **월드 제한 스킬:**
    
    - "이중 점프 스킬은 야생 월드(`world_resource`)에서만 써야 한다."
        
    - 구현: 노드 추가 시 `ContextSet`을 같이 넣습니다.
        
    - `Node.builder("dreamwork.skill.doublejump").withContext("world", "world_resource").build()`
        
    - **효과:** 플레이어가 마을(`world_town`)로 오면 자동으로 권한이 사라져 스킬이 안 나갑니다. 코드로 `if(world == town)` 검사를 덕지덕지 할 필요가 없어집니다.
        

---

### 5. Chat Formatting (채팅 연동)

LuckPerms는 채팅 형식을 관리하는 기능(LPC)이 있지만, DreamWork는 **[직업] [시민등급] [닉네임]** 처럼 복합적인 정보를 보여줘야 할 수 있습니다.

- **구조:** `EssentialsX Chat` 또는 `LPC` 플러그인을 사용하되, DreamWork가 **PlaceholderAPI**를 통해 정보를 제공하는 방식이 가장 깔끔합니다.
    
- **설정 예시 (EssentialsX Chat):**
    
    YAML
    
    ```
    format: '{dreamwork_job_prefix} {dreamwork_citizen_rank} {DISPLAYNAME}: {MESSAGE}'
    ```
    
    - `{dreamwork_job_prefix}`: LP Meta에서 가져온 `[광부 Lv.XX]`
        
    - `{dreamwork_citizen_rank}`: LP Group에서 가져온 `[시민]`
        

---

### 6. 요약 및 개발 조언

1. **그룹(Group)은 신분(Rank)** 관리에만 쓰세요. (거주민, 시민 등)
    
2. **직업(Job)은 권한(Node)과 메타(Meta)**로 관리하세요.
    
3. **비동기(Async) 필수:** LuckPerms의 데이터 저장/로드 작업은 무겁습니다. 반드시 `CompletableFuture`나 `runTaskAsynchronously` 안에서 처리해야 서버 렉(Lag)이 안 걸립니다.
    

이 구조로 설계하시면, DreamWork는 복잡한 권한 로직을 직접 짜는 대신 LuckPerms라는 강력한 엔진을 "리모컨"으로 조종하는 형태가 되어 개발 효율과 안정성이 극대화됩니다.




**Citizens (NPC 상호작용) 구체화 계획**

사용자님의 **Plan 2.0**에서 NPC는 단순한 장식품이 아니라, **"경제의 순환점(Trade Node)"**이자 **"퀘스트의 발주처(Quest Giver)"**입니다.

Citizens 플러그인은 NPC의 **'껍데기(외형/움직임)'**를 담당하고, DreamWork 플러그인은 NPC의 **'두뇌(기능/로직)'**를 담당하는 구조로 설계해야 합니다. 이를 위한 구체적인 연동 계획을 제안합니다.

---

### 1. 핵심 철학: "NPC는 키오스크다"

DreamWork 시스템에서 NPC를 복잡한 인공지능으로 보지 말고, **"걸어 다니는 GUI 버튼"**으로 정의하면 개발이 매우 쉬워집니다.

- **Citizens의 역할:** 월드에 서 있고, 플레이어를 쳐다보고, 가끔 걸어 다님. (스킨, 이름 담당)
    
- **DreamWork의 역할:** 플레이어가 NPC를 우클릭했을 때, **어떤 창(GUI)을 띄울지** 결정함.
    

---

### 2. 식별 시스템 (Identification System)

"이 NPC가 대장장이인지, 어부인지 어떻게 아는가?"에 대한 해결책입니다. NPC의 이름으로 코딩하면 이름 변경 시 오류가 나므로, **NPC ID 매핑 방식**을 사용합니다.

#### A. Config 설정 구조 (`npcs.yml`)

DreamWork 플러그인 폴더 내에 NPC의 역할과 ID를 매칭하는 파일을 둡니다.

YAML

```
# npcs.yml 예시
roles:
  blacksmith:
    ids: [3, 15]  # NPC ID 3번과 15번은 대장장이 기능을 함
    name: "타운 대장장이"
    gui: "FORGE_MAIN"
  
  fishmonger:
    ids: [7]
    name: "생선 장수"
    gui: "FISH_MARKET"
    
  quest_giver_miner:
    ids: [10]
    name: "광산 조합장"
    function: "OPEN_MISSION_DIALOGUE"
```

#### B. 로직 흐름

1. 운영자(사용자님)가 인게임에서 `/npc create 대장장이`를 입력 (ID: 3 생성됨).
    
2. `npcs.yml`에 `ids: [3]`을 등록.
    
3. 플레이어가 NPC를 우클릭하면, DreamWork가 ID 3번임을 확인하고 대장간 GUI를 오픈.
    

---

### 3. 직업별 NPC 상세 구현 시나리오

Plan 2.0의 각 직업 활동이 타운 NPC와 어떻게 연결되는지 정리했습니다.

#### ① 광부 - [대장장이 NPC]

- **위치:** 타운 대장간 건물 앞.
    
- **기능 1 (감정):** '미지의 광석'을 들고 우클릭 시 감정 GUI 오픈.
    
- **기능 2 (제련):** 철/금/구리를 들고 가면 '합금'으로 바꿔주는 교환창 오픈.
    
- **기능 3 (수리):** 마인크래프트 바닐라 모루보다 저렴하게 내구도를 수리해줌 (Vault 돈 소모).
    

#### ② 어부 - [수족관 관리자 & 생선 장수]

- **수족관 관리자:**
    
    - **조건:** `살아있는 물고기(Live)` 태그가 있는 아이템(물양동이/살림망)을 들고 우클릭.
        
    - **반응:** "오! 정말 싱싱한 참치로군요!" (대화 출력) -> 비싼 값에 매입 후 사라짐.
        
- **생선 장수:**
    
    - **기능:** 일반 물고기를 가져가면 수수료를 받고 **'회(Sashimi)'** 아이템으로 바꿔줌.
        

#### ③ 농부 - [주점 주인 & 요리사]

- **주점 주인 (Bartender):**
    
    - **기능:** 오크통에서 숙성된 술을 판매하는 곳.
        
    - **상호작용:** 숙성도(NBT Data)가 높은 술을 들고 클릭하면 추가 대사("이건 10년산이구만!")와 함께 보너스 금액 지급.
        
- **요리사:**
    
    - **기능:** 3성 작물을 버프 요리로 교환.
        

#### ④ 탐험가 - [우체부 & 지도 제작자]

- **우체부 (Courier):**
    
    - **배달 미션:** 우클릭 시 "이 소포를 좌표 X:2000에 있는 오두막의 [NPC ID: 55]에게 전해주게."라는 퀘스트 시작.
        
    - **도착 판정:** 실제 좌표 X:2000으로 이동하여 ID 55번 NPC를 우클릭해야 미션 완료 처리.
        
    - _핵심:_ Citizens NPC를 **네비게이션의 목적지**로 활용합니다.
        

#### ⑤ 사냥꾼 - [현상금 사냥꾼 & 박제사]

- **박제사:** 엘리트 몹의 머리를 가져오면 가구(설치 가능 아이템)로 가공해줌.
    
- **현상금 담당관:** "오늘의 수배 전단" GUI를 보여줌. (특정 몹 처치 퀘스트 수주)
    

---

### 4. 기술적 연동 아키텍처 (Logic Flow)

DreamWork 플러그인 내부의 `CitizensHook` 클래스 구조입니다.

코드 스니펫

```
graph TD
    Player -- "NPC 우클릭 (Right Click)" --> Event[NPCRightClickEvent]
    Event --> Hook[CitizensHook]
    
    Hook -- "1. NPC ID 확인" --> ID_Check{ID가 npcs.yml에 있는가?}
    
    ID_Check -- "YES (예: ID 5)" --> Role_Check{역할 판별}
    ID_Check -- "NO" --> Pass[무시 (일반 NPC)]
    
    Role_Check -- "Blacksmith" --> GUI1[대장간 GUI 오픈]
    Role_Check -- "QuestGiver" --> Logic1[미션 진행도 체크]
    
    Logic1 -- "미션 완료 상태" --> Reward[보상 지급 & 칭찬 대사]
    Logic1 -- "미션 진행 중" --> Status[진행 상황 알려줌]
    Logic1 -- "미션 없음" --> Assign[새 미션 부여]
```

### 5. 대화 시스템 (Dialogue System)

Citizens 자체 대화 기능(Denizen 등)은 너무 복잡합니다. DreamWork 자체적으로 심플하게 구현하는 것을 추천합니다.

- **말풍선 방식:**
    
    - NPC 우클릭 시 채팅창이 아니라, **플레이어 화면 중앙(Title/Subtitle)이나 액션바**에 대사를 띄웁니다.
        
    - _예:_ `Title: [대장장이]` `Subtitle: "오늘은 어떤 광물을 캐왔나, 친구?"`
        
- **랜덤 대사:**
    
    - `npcs.yml`에 대사 리스트를 넣어두고 랜덤으로 출력하면 생동감이 돕니다.
        
    - `messages: ["어서오게!", "망치가 어디 갔지?", "좋은 날씨야."]`
        

---

### 6. 요약 및 제안

1. **Citizens는 '모양'만 씁니다.** 복잡한 스크립트(Trait, Denizen)를 배우지 마세요.
    
2. **`NPCRightClickEvent` 하나면 충분합니다.** 이 이벤트 안에서 ID를 검사하고 DreamWork의 GUI를 여는 방식이 가장 깔끔하고 버그가 적습니다.
    
3. **NPC ID 관리는 Config 파일로 하세요.** 인게임에서 NPC를 지웠다 다시 만들어도, Config 파일의 숫자만 바꿔주면 코드를 수정할 필요가 없습니다.
    

이 계획대로라면, 마을은 단순한 건축물이 아니라 **"NPC들이 플레이어를 기다리는 살아있는 도시"**가 될 것입니다.



**PlaceholderAPI (정보 출력) 구체화 계획**

PlaceholderAPI(이하 PAPI)는 DreamWork 시스템 내부에서 열심히 계산되고 있는 숫자들(직업 레벨, 미션 상태, 시민 등급 등)을 **플레이어의 눈에 보이는 곳(채팅창, 스코어보드, 탭 리스트)**으로 꺼내주는 **"데이터 방송국"** 역할을 합니다.

DreamWork 플러그인 내부에 자체 PAPI 확장 모듈을 내장하여, 외부 플러그인들이 DreamWork의 데이터를 마음껏 가져다 쓸 수 있도록 하는 **설계 도면**입니다.

---

### 1. 연동의 핵심 원리: "방송국(Expansion) 설립"

DreamWork는 PAPI에게 데이터를 제공하는 **공급자(Provider)**가 되어야 합니다. 이를 위해 별도의 플러그인을 더 만드는 것이 아니라, **DreamWork 플러그인 내부에 `Expansion` 클래스를 포함**시키는 방식(Internal Expansion)이 가장 효율적입니다.

- **식별자(Identifier):** `dreamwork` (모든 요청은 `%dreamwork_...%`로 시작)
    
- **구동 방식:**
    
    1. 서버 구동 시 DreamWork가 PAPI에게 "나도 방송할 데이터가 있어"라고 신고(`register`).
        
    2. PAPI는 `%dreamwork_...%`라는 요청이 들어오면 무조건 DreamWork에게 마이크를 넘김.
        
    3. DreamWork는 요청된 키워드(예: `miner_level`)를 해석해서 현재 값(예: `50`)을 반환.
        

---

### 2. 제공할 플레이스홀더 목록 (Placeholder List)

Plan 2.0 기획에 맞춰, 외부로 송출해야 할 필수 데이터 목록을 정리했습니다. 이 키워드들을 스코어보드 플러그인 설정 파일에 적으면 그대로 숫자가 뜹니다.

#### A. 직업 정보 (Jobs)

가장 많이 사용될 데이터입니다.

|**키워드 (Placeholder)**|**반환 예시**|**설명**|
|---|---|---|
|`%dreamwork_job_miner_level%`|`10`|광부 레벨|
|`%dreamwork_job_miner_exp%`|`50.5`|광부 경험치 (%)|
|`%dreamwork_job_farmer_level%`|`5`|농부 레벨|
|`%dreamwork_job_fisher_state%`|`낚시 중`|현재 상태 (낚시, 휴식 등)|
|`%dreamwork_job_hunter_boss_kills%`|`3`|보스 처치 횟수|
|`%dreamwork_job_primary%`|`광부`|현재 가장 레벨이 높은 직업 (대표 직업)|

#### B. 시민 및 계급 (Citizenship)

채팅창 칭호나 탭 리스트에 사용하기 좋습니다.

|**키워드 (Placeholder)**|**반환 예시**|**설명**|
|---|---|---|
|`%dreamwork_rank_name%`|`거주민`|현재 시민 등급 텍스트|
|`%dreamwork_rank_color%`|`§a`|등급별 상징 색상 코드|
|`%dreamwork_town_name%`|`서울`|소속 타운 이름 (Towny 연동 시)|

#### C. 미션 및 경제 (Progress)

플레이어 개인의 목표 진행 상황을 보여줍니다.

|**키워드 (Placeholder)**|**반환 예시**|**설명**|
|---|---|---|
|`%dreamwork_mission_current%`|`석탄 100개 캐기`|현재 진행 중인 미션 이름|
|`%dreamwork_mission_progress%`|`45/100`|미션 진행도|
|`%dreamwork_currency_point%`|`1500`|시민 포인트 (돈 이외의 제2재화)|

---

### 3. 내부 아키텍처 (Internal Logic Flow)

DreamWork 내부의 `PAPIExpansion` 클래스가 데이터를 처리하는 논리적 흐름입니다.

코드 스니펫

```
graph LR
    User(플레이어 화면) -- "%dreamwork_miner_level%" 요청 --> PAPI(PlaceholderAPI)
    PAPI -- "식별자 'dreamwork' 확인" --> Expansion[DreamWorkExpansion]
    
    Expansion -- "키워드 'miner_level' 분석" --> Manager[JobManager]
    Manager -- "메모리에서 데이터 조회" --> Data[(User Data Cache)]
    
    Data -- "값: 50" --> Manager
    Manager -- "String 변환" --> Expansion
    Expansion -- "출력" --> PAPI
    PAPI -- "50" --> User
```

#### ※ 성능 최적화 (Critical)

PAPI 요청은 초당 수십 번 발생할 수 있습니다 (스코어보드 갱신 주기 때문).

- **절대 금지:** `onPlaceholderRequest` 메소드 안에서 DB(MySQL)나 파일 입출력(File I/O)을 하면 안 됩니다. 서버가 멈춥니다.
    
- **필수 구현:** 반드시 서버 접속 시 로드된 **메모리(HashMap 등)에 있는 값**만 즉시 반환(Return)하도록 짜야 합니다.
    

---

### 4. 활용 방안 (Integration Examples)

이 기능이 구현되면 서버에 다음과 같은 연출이 가능해집니다.

#### ① 개인화 스코어보드 (Sidebar)

- 사용 플러그인: AnimatedScoreboard 등
    
- **연출:**
    
    Plaintext
    
    ```
    [ 내 정보 ]
    닉네임: 홍길동
    신분: [거주민] (%dreamwork_rank_name%)
    
    [ 직업 현황 ]
    ⛏ 광부: Lv.10 (%dreamwork_job_miner_level%)
    🌾 농부: Lv.5  (%dreamwork_job_farmer_level%)
    
    [ 현재 목표 ]
    %dreamwork_mission_current%
    (%dreamwork_mission_progress%)
    ```
    

#### ② 채팅 포맷 (Chat Format)

- 사용 플러그인: LuckPerms Chat / EssentialsChat
    
- **설정:** `{dreamwork_rank_name} {DISPLAYNAME}: {MESSAGE}`
    
- **결과:** `[거주민] 홍길동: 안녕하세요!`
    

#### ③ NPC 홀로그램 (Hologram)

- 사용 플러그인: DecentHolograms
    
- **연출:** NPC 머리 위에 플레이어 맞춤형 정보 표시.
    
- **텍스트:** `어서오세요, %dreamwork_job_primary%님!` -> `어서오세요, 광부님!`
    

---

### 5. 요약 및 제안

1. **의존성 설정:** `plugin.yml`의 `softdepend`에 `PlaceholderAPI`를 꼭 넣으세요. (PAPI가 없어도 DreamWork 자체는 돌아가야 하므로)
    
2. **클래스 분리:** `DreamWorkExpansion` 클래스를 따로 만들어서 관리하면 코드가 아주 깔끔해집니다.
    
3. **데이터 캐싱:** "PAPI는 오직 메모리에 있는 데이터만 읽는다"는 원칙을 지키면 서버 렉 없이 화려한 UI를 제공할 수 있습니다.
    

이 설계를 통해 DreamWork의 데이터는 서버의 모든 곳(채팅, 보드, 탭)에서 살아 숨 쉬게 될 것입니다.