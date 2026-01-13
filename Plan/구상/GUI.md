제공해주신 `Plan2.0`, `공통`, `직업별 세부 테이블` 및 `미션 문서`들을 바탕으로, **DreamWork** 플러그인에 최적화된 GUI 시스템 설계안을 제안해 드립니다.

DreamWork는 **"직관적인 정보 전달"**과 **"경제적 상호작용"**이 핵심이므로, 복잡한 명령어 대신 마우스 클릭만으로 모든 상태를 확인하고 기능을 수행할 수 있는 **Chest GUI (인벤토리 기반 인터페이스)** 방식이 필수적입니다.

---

# 🛠️ DreamWork GUI 시스템 마스터 플랜

## 1. GUI 계층 구조 (Hierarchy)

모든 GUI는 `/dream` 또는 `/dw` 명령어 하나로 열리는 **메인 대시보드**에서 시작하여 파생되는 구조를 추천합니다.

코드 스니펫

```
graph TD
    A[메인 대시보드 (/dw)] --> B[내 정보 & 랭크]
    A --> C[직업 선택/관리]
    A --> D[미션 현황판]
    A --> E[통합 상점/교환소]
    
    C --> C1[광부 정보]
    C --> C2[농부 정보]
    C --> C3[어부 정보]
    C --> C4[사냥꾼 정보]
    C --> C5[탐험가 정보]
    
    D --> D1[일일 미션]
    D --> D2[주간/시즌 미션]
    
    E --> E1[대장간 (광부)]
    E --> E2[주방 (농부)]
    E --> E3[어시장 (어부)]
    E --> E4[박제소 (사냥꾼)]
    E --> E5[지도 상점 (탐험가)]
```

---

## 2. 세부 GUI 디자인 (Layout & Design)

마인크래프트 GUI는 보통 9x3(27칸) 또는 9x6(54칸)을 사용합니다. DreamWork는 정보량이 많으므로 **9x5(45칸) 또는 9x6(54칸)**을 기본 규격으로 잡는 것이 좋습니다.

### A. 메인 대시보드 (Main Dashboard)

서버에 접속하면 가장 많이 보게 될 화면입니다.

- **Size:** 9x3 (27 Slots) - 간결함 유지
    
- **구성:**
    
    - **중앙 (Slot 13):** **[내 프로필]** (플레이어 머리)
        
        - _Lore:_ 시민 등급, 현재 소지 금액, 타운 소속, 활동 점수 표시.
            
    - **좌측 (직업 아이콘):** 5대 직업 상태 요약
        
        - Slot 10: ⛏️ **광부** (다이아 곡괭이) - 레벨/현재 미션
            
        - Slot 11: 🌾 **농부** (금 괭이)
            
        - Slot 12: 🎣 **어부** (낚싯대)
            
        - Slot 14: 🏹 **사냥꾼** (활)
            
        - Slot 15: 🗺️ **탐험가** (지도)
            
    - **하단 (기능성):**
        
        - Slot 22: 📜 **미션 보드** (책과 깃펜) - 현재 진행 중인 미션 바로가기
            
        - Slot 26: ⚙️ **설정** (레드스톤 중계기) - 알림 설정 등
            

### B. 직업별 세부 정보창 (Job Detail GUI)

예: **광부(Miner)** 메뉴 클릭 시

- **Size:** 9x5 (45 Slots)
    
- **디자인 컨셉:**
    
    - **배경:** 회색 유리판으로 빈 공간 채움 (깔끔함).
        
    - **상단 중앙:** 직업 대표 아이콘 (현재 레벨, 경험치 바 시각화).
        
    - **중단 (스킬/패시브):**
        
        - 해금된 스킬은 **[빛나는 아이콘]**, 미해금은 **[기반암/장벽]** 혹은 회색 염료.
            
        - 마우스 오버 시: "해금 조건: 레벨 10 & 석탄 1000개 채굴" 표시.
            
    - **하단 (전용 기능):**
        
        - 버튼: **[미지의 광석 감정하기]** (모루 아이콘) -> 클릭 시 대장간 GUI로 이동.
            

### C. 기능성 GUI (Interactive Function Blocks)

DreamWork의 핵심인 '가공'과 '교환'을 담당하는 GUI입니다. 단순히 정보를 보여주는 것이 아니라, 유저가 아이템을 올리고 결과를 받아야 합니다.

#### 1. 대장간 (The Forge - 광부)

- **목적:** 미지의 광석 감정 및 합금 제작.
    
- **레이아웃:**
    
    - **Slot 10 (입력):** 유저가 '미지의 광석'을 놓는 곳.
        
    - **Slot 11~15 (진행):** 화살표 모양(→)으로 유리판 배치. 클릭 시 "감정 시작" (비용 차감).
        
    - **Slot 16 (결과):** 감정된 결과물(다이아몬드, 화석 등)이 나타나는 곳.
        
    - _특징:_ 결과 확인 전까지 아이템을 못 꺼내게 `InventoryClickEvent`를 취소(Cancel)해야 함.
        

#### 2. 요리/주방 (Kitchen - 농부)

- **목적:** 3성 작물로 버프 요리 제작.
    
- **레이아웃:**
    
    - **Slot 10, 11, 19, 20 (재료 투입구):** 2x2 조합창 느낌.
        
    - **Slot 24 (결과물 미리보기):** 재료가 맞으면 결과물 아이콘 표시.
        
    - **Slot 22 (요리하기 버튼):** 화로 아이콘. 클릭 시 요리 완료.
        

#### 3. 어시장 (Fishmonger - 어부)

- **목적:** 물고기 손질 (회 뜨기) 및 납품.
    
- **디자인:**
    
    - 유저 인벤토리의 물고기를 **클릭**하면 즉시 판매/손질되는 "Shop GUI" 방식 추천.
        
    - **좌측:** **[회 뜨기 모드]** 버튼 (칼 아이콘) - 클릭 시 모드 전환.
        
        - 활성화 상태에서 물고기 클릭 -> 회(Sashimi) + 부산물로 변환되어 인벤토리로 지급.
            
    - **우측:** **[납품 모드]** 버튼 (에메랄드 아이콘) - 클릭 시 모드 전환.
        
        - 활성화 상태에서 '살아있는 물고기' 클릭 -> 즉시 판매되고 돈 지급 (타운 아쿠아리움 납품).
            

---

## 3. 기술적 구현 가이드 (Implementation Strategy)

직접 플러그인을 개발하시므로, 유지보수가 쉬운 구조로 코드를 짜는 것이 중요합니다.

### A. GUI 프레임워크 (Abstract Class 구조)

모든 GUI가 공통적으로 가져야 할 기능을 추상 클래스로 만듭니다.

Java

```
public abstract class DreamGui implements InventoryHolder {
    protected Inventory inventory;
    protected Player player;
    protected DreamWorkPlugin plugin;

    public DreamGui(Player player, String title, int size) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, size, title);
        initializeItems(); // 아이템 배치 로직
    }

    // 자식 클래스에서 구현할 추상 메서드
    public abstract void initializeItems();
    
    // 클릭 이벤트 처리 (추상 메서드 or 오버라이딩)
    public abstract void onGuiClick(InventoryClickEvent event);

    public void open() {
        player.openInventory(inventory);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
```

- **장점:** `InventoryHolder`를 상속받아 구현하면, `InventoryClickEvent`에서 `event.getInventory().getHolder()`가 `DreamGui`의 인스턴스인지 확인하는 것만으로 내 플러그인의 GUI인지 판별할 수 있습니다.
    

### B. PDC 데이터 연동 (Data Binding)

GUI를 열 때(`initializeItems`), 플레이어의 **PersistentDataContainer(PDC)**를 읽어서 아이콘을 동적으로 생성해야 합니다.

- **예시 (광부 GUI 아이콘 생성):**
    
    1. `player.getPersistentDataContainer()`에서 광부 레벨(`dreamwork_miner_level`) 조회.
        
    2. 레벨에 따라 아이콘 재질 변경 (Lv.1 돌 곡괭이 -> Lv.50 다이아 곡괭이).
        
    3. 아이템 **Lore**에 진행 중인 미션 진행도(`current_mission_progress` / `max_progress`)를 계산하여 표시.
        
    
    Java
    
    ```
    // 진행도 바(Bar) 유틸리티 예시
    String progressBar = getProgressBar(current, max, 20, "|", ChatColor.GREEN, ChatColor.GRAY);
    // 결과: "§a||||||||||§7||||||||||"
    ```
    

### C. 이벤트 리스너 처리 (The Listener)

중앙 리스너 하나에서 모든 GUI 클릭을 분배합니다.

Java

```
@EventHandler
public void onInventoryClick(InventoryClickEvent event) {
    if (event.getInventory().getHolder() instanceof DreamGui) {
        event.setCancelled(true); // 기본적으로 아이템 이동 막기 (필요시 내부에서 풀기)
        
        DreamGui gui = (DreamGui) event.getInventory().getHolder();
        gui.onGuiClick(event); // 해당 GUI 클래스에 처리 위임
    }
}
```

### D. GUI 갱신 (Refresh)

미션 보상을 받거나 아이템을 가공했을 때, GUI가 즉시 갱신되어야 합니다.

- 가장 쉬운 방법: 로직 처리 후 `initializeItems()`를 다시 호출하여 인벤토리 내용을 덮어씌웁니다. 굳이 인벤토리를 닫았다가 다시 열 필요는 없습니다.
    

---

## 4. 유저 경험(UX)을 위한 꿀팁

1. **뒤로 가기 버튼 (Back Button):**
    
    - 모든 하위 메뉴의 좌측 하단(Slot 45 등)이나 우측 하단에는 반드시 **[뒤로 가기]** (화살표 머리 혹은 문) 버튼을 두어 메인 메뉴로 돌아가게 해주세요.
        
2. **소리 효과 (Sound Feedback):**
    
    - GUI 열기: `UI_BUTTON_CLICK`
        
    - 성공/수령: `ENTITY_PLAYER_LEVELUP` (피치 높게)
        
    - 실패/거절: `BLOCK_NOTE_BLOCK_BASS`
        
    - _시각 정보뿐만 아니라 청각 정보가 있어야 "직관적"이라고 느낍니다._
        
3. **색상 코딩 (Color Coding):**
    
    - DreamWork 문서에 따르면 등급(Tier)이 중요하므로, GUI 내 아이템 이름 색상을 통일하세요.
        
    - 일반(흰색) -> 숙련(초록) -> 전문(파랑) -> 장인(보라) -> 전설(주황).
        
4. **아이템 반환:**
    
    - 대장간이나 요리 GUI에 아이템을 올려둔 상태로 인벤토리를 닫으면 (`InventoryCloseEvent`), 올려둔 아이템이 증발하지 않고 플레이어 인벤토리로 들어오거나 바닥에 드랍되도록 안전장치를 꼭 구현해야 합니다.
        

이 설계를 바탕으로 **`DreamGui` 추상 클래스**를 먼저 만드시고, **`MainDashboard`**부터 하나씩 상속받아 구현해 나가시는 것을 추천합니다.




제안해주신 메인 대시보드(Main Dashboard)는 유저가 **DreamWork** 서버에 접속했을 때 가장 먼저, 그리고 가장 자주 마주하는 **'서버의 얼굴'**입니다.

따라서 단순히 버튼을 나열하는 것을 넘어, **"내가 지금 무엇을 해야 하는가?"**를 한눈에 보여주고, 성취감을 자극하는 **게이미피케이션(Gamification)** 요소가 UI에 녹아있어야 합니다.

요청하신 **[A. 메인 대시보드]**의 상세 디자인 및 구현 계획서를 제안합니다.

---

# 🖥️ DreamWork 메인 대시보드 상세 기획서 (GUI Design)

## 1. GUI 개요 및 레이아웃

이 GUI는 `/dw`, `/dream`, `/menu` 명령어 또는 **'지급된 메뉴 아이템(예: 네더의 별) 우클릭'**으로 호출됩니다.

- **GUI 제목:** `DreamWork : My Status`
    
- **크기:** 9 x 3 (27 Slots) - _한눈에 들어오는 컴팩트한 사이즈_
    

### 📐 그리드 설계 (Visual Grid)

Plaintext

```
[ 00 ][ 01 ][ 02 ][ 03 ][ 04 ][ 05 ][ 06 ][ 07 ][ 08 ]  <- 장식 (Glass Pane)
[ 09 ][ 10 ][ 11 ][ 12 ][ 13 ][ 14 ][ 15 ][ 16 ][ 17 ]  <- 직업 & 프로필 핵심 라인
[ 18 ][ 19 ][ 20 ][ 21 ][ 22 ][ 23 ][ 24 ][ 25 ][ 26 ]  <- 기능 & 미션 라인
```

- **배경(00~09, 17~18, 26):** `GRAY_STAINED_GLASS_PANE` (이름 없음)으로 채워 깔끔하게 마감.
    
- **중앙 핵심(13):** 내 프로필 (Player Head)
    
- **직업 라인(10, 11, 12 | 14, 15, 16):** 좌측 3개(생산/채집), 우측 3개(전투/탐험) 대칭 배치.
    

---

## 2. 슬롯별 상세 명세 (Item Specification)

각 아이템은 플레이어의 PDC(PersistentDataContainer) 데이터와 연동되어 **동적으로 변화**해야 합니다.

### 👤 중앙: 내 정보 (Slot 13)

플레이어의 현재 신분을 나타내는 지표입니다. `공통.pdf`의 시민 등급 시스템을 반영합니다.

- **아이템:** `PLAYER_HEAD` (플레이어 본인 스킨)
    
- **표시 이름:** `§e§l[ %player_name% 님의 여권 ]`
    
- **Lore (설명):**
    
    Plaintext
    
    ```
    §7DreamWork ID: #%player_uuid_short%
    §7
    §f🎖️ 시민 등급: §b%dreamwork_citizen_rank%  <- (방랑자/거주민/시민 등)
    §f💰 보유 자산: §e%vault_eco_balance_formatted% D
    §f🏰 소속 타운: §a%towny_town_name%
    §7
    §7⏲️ 접속 시간: %statistic_time_played%
    §7
    §e클릭하여 시민 등급 혜택 보기
    ```
    

### ⛏️ 좌측: 생산 직업군 (Slot 10, 11, 12)

직업 레벨에 따라 아이콘의 재질(Material)이 바뀝니다. (예: Lv.1 돌 곡괭이 -> Lv.50 다이아 곡괭이)

#### **Slot 10: 광부 (Miner)**

- **아이템:** `IRON_PICKAXE` (레벨에 따라 재질 변경)
    
- **표시 이름:** `§6[ ⛏️ 광부 : 대지의 개척자 ]`
    
- **Lore:**
    
    Plaintext
    
    ```
    §7레벨: §fLv.%dw_miner_level%
    §7숙련도: §a[||||||||||] §7(45%)
    §7
    §f📜 현재 임무: §e철광석 채굴 (II)
    §7▶ 목표: 철광석 500개 캐기 (320/500)
    §7
    §7⚒️ 대표 스킬: 광맥 탐지, 합금 제련
    §7
    §e클릭하여 광부 전용 메뉴 열기
    ```
    
- **기능:** 클릭 시 **[광부 세부 정보창]**으로 이동.
    

#### **Slot 11: 농부 (Farmer)**

- **아이템:** `GOLDEN_HOE` (레벨에 따라 재질 변경)
    
- **표시 이름:** `§a[ 🌾 농부 : 대지의 관리자 ]`
    
- **Lore:**
    
    Plaintext
    
    ```
    §7레벨: §fLv.%dw_farmer_level%
    §7... (위와 동일한 포맷)
    §f📜 현재 임무: §e최고급 호박 파이
    §7▶ 목표: 3성 호박 수확 (5/10)
    §7
    §e클릭하여 농부 전용 메뉴 열기
    ```
    

#### **Slot 12: 어부 (Fisher)**

- **아이템:** `FISHING_ROD`
    
- **표시 이름:** `§b[ 🎣 어부 : 심해의 탐구자 ]`
    
- **Lore:**
    
    Plaintext
    
    ```
    §7...
    §f📜 현재 임무: §e전설의 돗돔
    §7▶ 목표: 100cm 이상 물고기 낚기
    §7
    §e클릭하여 어부 전용 메뉴 열기
    ```
    

### ⚔️ 우측: 탐험/전투 직업군 (Slot 14, 15)

#### **Slot 14: 사냥꾼 (Hunter)**

- **아이템:** `BOW` or `CROSSBOW`
    
- **표시 이름:** `§c[ 🏹 사냥꾼 : 야생의 수호자 ]`
    
- **Lore:**
    
    Plaintext
    
    ```
    §7...
    §f📜 현재 현상수배: §4엘리트 스켈레톤
    §7▶ 목표: 1마리 처치 (0/1)
    §7
    §e클릭하여 사냥꾼 전용 메뉴 열기
    ```
    

#### **Slot 15: 탐험가 (Adventurer)**

- **아이템:** `FILLED_MAP` (or `COMPASS`)
    
- **표시 이름:** `§d[ 🗺️ 탐험가 : 지평선의 기록자 ]`
    
- **Lore:**
    
    Plaintext
    
    ```
    §7...
    §f📜 현재 탐사지: §e메사 바이옴
    §7▶ 목표: 해당 좌표 도달 및 보고서 작성
    §7
    §e클릭하여 탐험가 전용 메뉴 열기
    ```
    

### 🧩 하단: 기능성 메뉴 (Slot 21, 22, 23)

#### **Slot 21: 🏆 명예의 전당 (Rank & Stats)**

- **아이템:** `NETHER_STAR`
    
- **표시 이름:** `§e[ 랭킹 및 통계 ]`
    
- **설명:** 서버 내 전체 직업 랭킹과 나의 종합 순위를 확인합니다.
    

#### **Slot 22: 📜 통합 미션 보드 (Mission Board)**

- **아이템:** `WRITABLE_BOOK`
    
- **표시 이름:** `§a[ 📋 일일/주간 미션 확인 ]`
    
- **Lore:**
    
    Plaintext
    
    ```
    §7직업별 미션 외에
    §7서버 공통 미션(시민 의무)을 확인합니다.
    §7
    §f📅 일일 미션: §a완료 가능 (1/3)
    §f📅 주간 미션: §c진행 중 (2/5)
    ```
    
- **Glow Effect:** 완료 가능한 보상이 있을 경우 **반짝임(Enchant Effect)** 부여.
    

#### **Slot 23: 👜 통합 교환소 (Exchange)**

- **아이템:** `CHEST` (or `ENDER_CHEST`)
    
- **표시 이름:** `§6[ ⚖️ 통합 교환소 ]`
    
- **설명:** 각 직업의 결과물(광석, 농작물, 물고기 등)을 교환하거나 가공소(대장간/주방 등)로 바로 이동하는 퀵 메뉴입니다.
    

---

## 3. 개발 구현 가이드 (Java Code Strategy)

상속 구조를 활용하여 유지보수성을 높입니다.

### A. 동적 아이콘 생성 (Dynamic Item Builder)

직업 레벨에 따라 아이콘의 재질(Material)을 변경하는 로직 예시입니다.

Java

```
public ItemStack getJobIcon(Player player, JobType job) {
    int level = DreamWork.getPdcManager().getLevel(player, job);
    Material mat;
    
    // 광부의 경우 레벨별 아이콘 진화
    if (job == JobType.MINER) {
        if (level < 10) mat = Material.STONE_PICKAXE;
        else if (level < 30) mat = Material.IRON_PICKAXE;
        else mat = Material.DIAMOND_PICKAXE;
    } else if (job == JobType.FARMER) {
        // ... 농부 로직
        mat = Material.GOLDEN_HOE; 
    }
    // ... 기타 직업

    ItemStack item = new ItemStack(mat);
    ItemMeta meta = item.getItemMeta();
    
    // Custom Model Data 적용 (추후 리소스팩 확장을 위해)
    meta.setCustomModelData(1000 + level); 
    
    // Lore 생성 (Progress Bar 포함)
    List<String> lore = new ArrayList<>();
    lore.add("§7레벨: §fLv." + level);
    lore.add("§7숙련도: " + getProgressBar(player, job)); 
    // ...
    
    item.setItemMeta(meta);
    return item;
}

// 프로그레스 바 유틸리티
private String getProgressBar(Player p, JobType job) {
    double current = DreamWork.getPdcManager().getExp(p, job);
    double max = DreamWork.getLevelSystem().getMaxExp(level);
    int totalBars = 10;
    int filledBars = (int) ((current / max) * totalBars);
    
    return "§a" + "|".repeat(filledBars) + "§7" + "|".repeat(totalBars - filledBars);
}
```

### B. 미션 알림 상태 (Notification State)

`Slot 22` 미션 보드 아이콘에 **"보상 수령 가능"** 상태를 시각적으로 표시하는 것이 중요합니다.

Java

```
public ItemStack getMissionBoardIcon(Player player) {
    ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
    boolean hasReward = DreamWork.getMissionManager().hasClaimableReward(player);

    if (hasReward) {
        // 보상이 있으면 인챈트 효과 추가 (반짝임)
        item.addUnsafeEnchantment(Enchantment.LUCK, 1);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.setDisplayName("§a§l[ 📋 미션 보드 (보상 있음!) ]");
        item.setItemMeta(meta);
    }
    return item;
}
```

---

## 4. UX (사용자 경험) 개선 포인트

1. **진입 장벽 낮추기:**
    
    - 초보자는 메뉴가 복잡하면 당황합니다. 메인 대시보드는 **"정보 확인"**에 집중하고, 복잡한 기능(조합, 강화)은 클릭해서 들어가는 **하위 메뉴**로 격리시켰습니다.
        
2. **직관적인 색상 사용:**
    
    - 광부=주황(Gold), 농부=연두(Green), 어부=하늘(Aqua), 사냥꾼=빨강(Red), 탐험가=보라(Light Purple) 등으로 직업별 테마 색상을 고정하여 텍스트를 읽지 않고도 구분되게 합니다.
        
3. **사운드 피드백:**
    
    - 직업 아이콘 클릭 시: `UI_BUTTON_CLICK`
        
    - 미션 보드(보상 있음) 클릭 시: `BLOCK_AMETHYST_BLOCK_CHIME` (청량한 소리)
        
    - 닫기: `UI_WIN_TEAMS_BLOCKED` (낮은 베이스 소리)
        

이 설계안은 DreamWork의 핵심 철학인 **"직관성"**을 최우선으로 고려하였으며, 향후 기능이 확장되더라도 `Slot 23` 등의 빈 공간을 활용할 수 있어 확장성이 뛰어납니다.

다음 단계로 **[B. 직업별 세부 정보창]**의 구체적인 디자인이 필요하시다면 말씀해 주세요. 각 직업의 특성(광부-제련, 어부-납품 등)에 맞춘 GUI 설계를 이어가겠습니다.



**DreamWork** 플러그인의 핵심인 **직업별 세부 정보창 (Job Detail GUI)** 구체화 계획을 제안해 드립니다.

이 화면은 플레이어가 자신의 **"성장(Progression)"**을 확인하고, 각 직업의 **"고유 기능(Unique Function)"**으로 진입하는 **관제 센터** 역할을 합니다.

---

# 🛠️ B. 직업별 세부 정보창 (Job Detail GUI) 마스터 플랜

## 1. 공통 레이아웃 설계 (Common Template)

모든 직업 GUI는 유저의 학습 비용을 줄이기 위해 **동일한 구조**를 가집니다.

- **크기:** 9x5 (45 Slots)
    
- **배경:** `GRAY_STAINED_GLASS_PANE` (빈 공간 마감)
    

### 📐 그리드 구조 (Visual Grid)

Plaintext

```
[ 00 ][ 01 ][ 02 ][ 03 ][ 04 ][ 05 ][ 06 ][ 07 ][ 08 ]  <- 1열: 타이틀 & 스탯 요약
[ 09 ][ 10 ][ 11 ][ 12 ][ 13 ][ 14 ][ 15 ][ 16 ][ 17 ]  <- 2열: 패시브/스킬 트리 (Tier 1)
[ 18 ][ 19 ][ 20 ][ 21 ][ 22 ][ 23 ][ 24 ][ 25 ][ 26 ]  <- 3열: 패시브/스킬 트리 (Tier 2)
[ 27 ][ 28 ][ 29 ][ 30 ][ 31 ][ 32 ][ 33 ][ 34 ][ 35 ]  <- 4열: 통계 & 업적 현황
[ 36 ][ 37 ][ 38 ][ 39 ][ 40 ][ 41 ][ 42 ][ 43 ][ 44 ]  <- 5열: 고유 기능 실행 & 네비게이션
```

- **Slot 04 (중앙 상단):** 직업 대표 아이콘 (메인 대시보드와 동일, 상세 스탯 표시)
    
- **Slot 22 (정중앙):** 핵심 능력 또는 현재 단계 설명
    
- **Slot 40 (하단 중앙):** **[직업 고유 기능 열기]** (예: 대장간, 주방, 어시장 등)
    
- **Slot 36:** [뒤로 가기] (메인 대시보드로 이동)
    
- **Slot 44:** [도움말/가이드북]
    

---

## 2. 직업별 상세 디자인 (Specific Design)

각 직업의 **`변경.pdf`** 및 **`세부 테이블.pdf`**에 명시된 스킬과 기능을 반영했습니다.

### ⛏️ 1. 광부 (Miner) - "대지의 개척자"

_광부는 '채광'과 '제련'이 핵심입니다._

- **Slot 04 (대표):** `DIAMOND_PICKAXE` (Lore: 현재 채광 속도 보너스, 행운 수치)
    
- **스킬 슬롯 (해금 여부에 따라 아이콘 활성/비활성):**
    
    - **Slot 11 [패시브: 광부의 눈]:** `GLOW_INK_SAC` (주변 광물 발광 효과 / 해금: Lv.10)
        
    - **Slot 13 [패시브: 정밀 타격]:** `GOLDEN_PICKAXE` (더블 드롭 확률 증가 / 해금: Lv.30)
        
    - **Slot 15 [패시브: 안전 제일]:** `IRON_HELMET` (용암/낙하 피해 감소 / 해금: Lv.50)
        
- **통계 슬롯 (Slot 30, 32):**
    
    - `COAL`: 총 캔 석탄 수
        
    - `DIAMOND`: 총 캔 다이아몬드 수
        
- **🚀 핵심 기능 (Slot 40): [대장간 이동 (Go to Forge)]**
    
    - **아이콘:** `ANVIL`
        
    - **기능:** 클릭 시 **'대장간 GUI'** (미지의 광석 감정 & 합금 제작)로 화면 전환.
        
    - **Lore:**
        
        - "미지의 광석을 감정합니다."
            
        - "특수 합금(Alloy)을 제련합니다."
            

### 🌾 2. 농부 (Farmer) - "대지의 관리자"

_농부는 '품질(Star)'과 '가공(Cooking)'이 핵심입니다._

- **Slot 04 (대표):** `GOLDEN_HOE` (Lore: 3성 작물 수확 확률 표시)
    
- **스킬 슬롯:**
    
    - **Slot 11 [패시브: 녹색 손길]:** `BONE_MEAL` (수확 시 자동 재파종 / 해금: Lv.5)
        
    - **Slot 13 [패시브: 풍년의 기원]:** `SUNFLOWER` (성장 속도 범위 버프 / 해금: Lv.25)
        
    - **Slot 15 [패시브: 품종 개량]:** `GOLDEN_CARROT` (3성 작물 등장 확률 2배 / 해금: Lv.40)
        
- **🚀 핵심 기능 (Slot 40): [주방/저장고 이동 (Go to Kitchen)]**
    
    - **아이콘:** `SMOKER` (훈연기)
        
    - **기능:** 클릭 시 **'요리 GUI'**로 전환.
        
    - **Lore:**
        
        - "3성 작물로 버프 요리를 만듭니다."
            
        - "농산물을 전용 저장고에 보관합니다."
            

### 🎣 3. 어부 (Fisher) - "심해의 탐구자"

_어부는 '신선도(Live)'와 '손질(Fillet)'이 핵심입니다._

- **Slot 04 (대표):** `FISHING_ROD` (Lore: 대어 낚을 확률, 현재 미끼 정보)
    
- **스킬 슬롯:**
    
    - **Slot 11 [패시브: 미끼 마스터]:** `TROPICAL_FISH` (크릴 새우 미끼 효율 증가 / 해금: Lv.10)
        
    - **Slot 13 [패시브: 강태공]:** `NAUTILUS_SHELL` (입질 대기 시간 감소 / 해금: Lv.20)
        
    - **Slot 15 [패시브: 살림망]:** `WATER_BUCKET` (잡은 물고기를 'Live' 상태로 보존 / 해금: Lv.30)
        
- **🚀 핵심 기능 (Slot 40): [어시장 이동 (Fish Market)]**
    
    - **아이콘:** `PUFFERFISH_BUCKET`
        
    - **기능:** 클릭 시 **'어시장 GUI'**로 전환.
        
    - **Lore:**
        
        - "물고기를 회(Sashimi)로 뜹니다."
            
        - "살아있는 물고기를 아쿠아리움에 납품합니다."
            

### 🏹 4. 사냥꾼 (Hunter) - "야생의 수호자"

_사냥꾼은 '도감(Bestiary)'과 '현상금(Bounty)'이 핵심입니다._

- **Slot 04 (대표):** `CROSSBOW` (Lore: 치명타 확률, 엘리트 몹 추가 데미지)
    
- **스킬 슬롯:**
    
    - **Slot 11 [패시브: 약점 간파]:** `SPYGLASS` (체력 30% 미만 적에게 추가 데미지 / 해금: Lv.15)
        
    - **Slot 13 [패시브: 추적자]:** `COMPASS` (주변 엘리트 몬스터 방향 표시 / 해금: Lv.30)
        
    - **Slot 15 [패시브: 도축 숙련]:** `IRON_SWORD` (가죽/고기 드롭량 2배 / 해금: Lv.45)
        
- **🚀 핵심 기능 (Slot 40): [현상수배 게시판 (Bounty Board)]**
    
    - **아이콘:** `SKELETON_SKULL`
        
    - **기능:** 클릭 시 **'현상금 GUI'**로 전환.
        
    - **Lore:**
        
        - "오늘의 사냥 의뢰를 확인합니다."
            
        - "박제(Trophy)를 제작합니다."
            

### 🗺️ 5. 탐험가 (Adventurer) - "지평선의 기록자"

_탐험가는 '좌표 기록'과 '지도 제작'이 핵심입니다._

- **Slot 04 (대표):** `FILLED_MAP` (Lore: 밝힌 청크 수, 이동 거리)
    
- **스킬 슬롯:**
    
    - **Slot 11 [패시브: 가벼운 발걸음]:** `LEATHER_BOOTS` (비전투 시 이동 속도 증가 / 해금: Lv.10)
        
    - **Slot 13 [패시브: 지형 극복]:** `VINE` (덩굴/사다리 오르는 속도 증가 / 해금: Lv.25)
        
    - **Slot 15 [패시브: 육감]:** `EYE_OF_ENDER` (주변 유적/구조물 감지 알림 / 해금: Lv.50)
        
- **🚀 핵심 기능 (Slot 40): [탐사 보고 (Exploration Report)]**
    
    - **아이콘:** `WRITABLE_BOOK` (책과 깃펜)
        
    - **기능:** 클릭 시 **'보고서 작성 GUI'**로 전환.
        
    - **Lore:**
        
        - "현재 위치의 좌표를 기록하여 좌표 스크롤을 만듭니다."
            
        - "탐험 일지를 납품하여 보상을 받습니다."
            

---

## 3. 기술적 구현 전략 (Implementation)

세부 정보창은 **스킬 해금 상태**를 시각적으로 보여주는 것이 가장 중요합니다.

### A. 스킬 아이콘 렌더링 로직 (Skill Renderer)

`DreamGui` 클래스 내부에서 `renderSkill` 메서드를 활용합니다.

Java

```
// 스킬 아이콘을 배치하는 헬퍼 메서드
private void renderSkill(int slot, String skillName, int requiredLevel, Material icon, String description) {
    int currentLevel = DreamWork.getPdcManager().getLevel(player, this.jobType);
    boolean isUnlocked = currentLevel >= requiredLevel;

    ItemStack item;
    if (isUnlocked) {
        // 해금됨: 밝은 아이콘 + 인챈트 효과
        item = new ItemStack(icon);
        item.addUnsafeEnchantment(Enchantment.LUCK, 1); // 반짝임
        // ... Meta 설정: "§a[습득 완료]"
    } else {
        // 미해금: 회색 염료 or 기반암 + 빨간 텍스트
        item = new ItemStack(Material.GRAY_DYE); 
        // ... Meta 설정: "§c[잠김] §7해금 조건: Lv." + requiredLevel
    }
    
    inventory.setItem(slot, item);
}
```

### B. GUI 전환 (Navigation System)

`Slot 40` 클릭 시 현재 창을 닫지 말고, 즉시 새로운 인벤토리를 열어 **빠른 전환(Quick Switch)** 느낌을 줍니다.

Java

```
@Override
public void onGuiClick(InventoryClickEvent event) {
    if (event.getSlot() == 40) {
        // 예: 광부 GUI에서 대장간 버튼 클릭 시
        if (this.jobType == JobType.MINER) {
            new ForgeGui(player).open(); // 대장간 GUI 열기
        }
        // ... 타 직업 분기
        return;
    }
    
    if (event.getSlot() == 36) {
        new MainDashboardGui(player).open(); // 뒤로 가기
        return;
    }
}
```

---

## 4. 유저 경험(UX) 디테일

1. **시각적 차별화:**
    
    - **해금된 스킬:** 아이콘이 반짝이거나(`ItemFlag.HIDE_ENCHANTS`), Lore가 **초록색**으로 표시되어야 합니다.
        
    - **잠긴 스킬:** 아이콘을 `BARRIER`나 `BEDROCK` 대신, **`GRAY_DYE`(회색 염료)**나 **`COAL`(석탄)** 처럼 어두운 계열의 아이템을 사용하여 "아직 활성화되지 않음"을 은유적으로 표현하는 것이 훨씬 세련됩니다.
        
2. **직관적인 설명 (Tooltip):**
    
    - 스킬 아이콘에 마우스를 올리면, 정확한 **수치(%)**를 보여주세요.
        
    - 예: "채광 속도 10% 증가" (O) vs "채광 속도 증가" (X)
        
3. **성취감 부여:**
    
    - Slot 30~34 구간에 **"누적 통계"**를 넣어주세요. (예: "지금까지 잡은 물고기: 5,231마리")
        
    - 이 숫자가 올라가는 것을 보기 위해서라도 유저는 GUI를 자주 열어보게 됩니다.
        

이 설계를 바탕으로 **`JobDetailGui`** 클래스를 하나 만드시고, 생성자에서 `JobType`을 받아 내용만 갈아끼우는 방식으로 구현하면 코드 중복을 최소화할 수 있습니다.

다음 단계로는 이 메뉴들에서 연결되는 **실제 기능성 GUI (대장간, 주방, 어시장)**의 구체적인 슬롯 배치와 로직(InventoryClickEvent 처리)이 필요하실 것입니다. 이에 대한 기획도 준비되어 있으니 말씀해 주세요.



**DreamWork**의 경제가 실제로 돌아가는 핵심 엔진, **기능성 GUI (Interactive Function Blocks)**의 상세 기획안입니다.

이 GUI들은 단순히 정보를 보여주는 것을 넘어, 유저가 **아이템을 투입(Input)**하고, **결과를 수령(Output)**하는 상호작용이 일어나므로 **데이터 안전성(Item Safety)**과 **직관성(Intuition)**이 무엇보다 중요합니다.

---

# 🏭 C. 기능성 GUI (Interactive Function Blocks) 마스터 플랜

## 1. GUI 시스템 공통 설계 (System Architecture)

모든 기능성 GUI는 **`MachineGui`**라는 개념을 공유합니다.

- **구조:** `입력 슬롯` → `가공 버튼(Process)` → `결과 슬롯`
    
- **보안 수칙 (매우 중요):**
    
    1. **아이템 반환:** GUI를 닫을 때(`InventoryCloseEvent`), 입력/결과 슬롯에 남은 아이템은 **반드시** 플레이어 인벤토리로 돌아오거나 바닥에 드랍되어야 합니다. (아이템 증발 방지)
        
    2. **입력 제한:** 입력 슬롯에는 해당 기능에 맞는 아이템(예: 광석, 식재료)만 올라가도록 `InventoryClickEvent`에서 필터링해야 합니다.
        

---

## 2. 직업별 기능성 GUI 상세 디자인

### ⚒️ 1. 대장간 (The Forge) - 광부 전용

**목적:** '미지의 광석' 감정 및 '특수 합금' 제련.

- **Size:** 9x3 (27 Slots) - _집중도를 높이기 위해 컴팩트하게 구성_
    
- **Layout:**
    
    Plaintext
    
    ```
    [ 00 ][ 01 ][ 02 ][ 03 ][ 04 ][ 05 ][ 06 ][ 07 ][ 08 ]
    [ 09 ][ IN ][ 11 ][ 12 ][BTN][ 14 ][ 15 ][OUT][ 17 ]
    [ 18 ][ 19 ][ 20 ][ 21 ][ 22 ][ 23 ][ 24 ][ 25 ][ 26 ]
    ```
    
- **슬롯 상세:**
    
    - **Slot 10 (IN):** **[재료 투입구]**
        
        - 필터: `미지의 광석` 또는 `합금 재료`만 허용.
            
    - **Slot 13 (BTN):** **[두드리기 (망치)]**
        
        - 아이콘: `ANVIL`
            
        - 상태 변화: 재료가 올바르면 "§a[제련 시작]"으로 이름 변경 및 반짝임 효과.
            
        - 기능: 클릭 시 `Sound.BLOCK_ANVIL_USE` 소리와 함께 확률 계산 후 결과물 생성.
            
    - **Slot 16 (OUT):** **[완성품 받침대]**
        
        - 기능: 결과물(다이아몬드, 화석, 합금 등)이 나타나는 곳. 유저는 여기서 아이템을 꺼낼 수만 있음.
            
    - **배경 (나머지):**
        
        - 기본: `GRAY_STAINED_GLASS_PANE`
            
        - 가공 성공 시: Slot 11, 12, 14, 15가 잠시 `LIME_STAINED_GLASS_PANE`으로 깜빡임.
            

### 🍳 2. 주방 (The Kitchen) - 농부 전용

**목적:** 3성 작물을 조합하여 '버프 요리' 제작.

- **Size:** 9x5 (45 Slots) - _레시피북을 보여주기 위해 넓게 구성_
    
- **Layout:**
    
    - **좌측 (요리 도구):**
        
        - Slot 10, 11, 19, 20: **[2x2 재료 투입]** (마인크래프트 제작대와 유사하지만 2x2)
            
        - Slot 21: **[화살표 (→)]**
            
        - Slot 22: **[조리 버튼]** (화로/훈연기 아이콘)
            
        - Slot 24: **[완성된 요리]** (미리보기 및 수령)
            
    - **우측 (레시피 북):**
        
        - Slot 16, 25, 34 등 우측 3열은 **[추천 레시피]**를 보여줌.
            
        - 예: "광부의 도시락 - 감자(3성) + 당근(3성)" 아이콘 클릭 시 재료가 인벤토리에 있다면 자동으로 투입구로 이동(Auto-Fill).
            

### 🏪 3. 어시장 (Fish Market) - 어부 전용

**목적:** 물고기 손질(회) 및 납품.

- **방식:** 아이템을 넣고 돌리는 방식이 아니라, **"모드(Mode) 선택형 상호작용"**이 더 효율적입니다.
    
- **Size:** 9x3 (27 Slots)
    
- **Layout:**
    
    - **Slot 11 (칼 아이콘):** **[🔪 회 뜨기 모드]**
        
        - 설명: "클릭하여 활성화하세요. 인벤토리의 물고기를 클릭하면 즉시 회로 변합니다."
            
        - 상태: 활성화 시 `ENCHANTED` 효과 부여.
            
    - **Slot 15 (에메랄드 아이콘):** **[💰 납품 모드]**
        
        - 설명: "살아있는 물고기(Live)를 클릭하여 즉시 납품합니다."
            
    - **동작 로직:**
        
        1. 유저가 `회 뜨기 모드`를 켭니다.
            
        2. 자기 인벤토리(Bottom Inventory)에 있는 '대구'를 클릭합니다.
            
        3. `InventoryClickEvent`가 이를 감지하여, 대구를 제거하고 '회'와 '생선 뼈'를 즉시 지급합니다. (`Sound.ENTITY_SHEEP_SHEAR` 효과음)
            
        
        - _장점: 수십 마리의 물고기를 빠르게 처리하는 '손맛'을 제공합니다._
            

### 📜 4. 탐험가 책상 (Cartography) - 탐험가 전용

**목적:** 좌표 기록 및 스크롤 제작.

- **Size:** 9x3 (27 Slots)
    
- **Layout:**
    
    - **Slot 10 (IN):** **[빈 종이/지도 투입]**
        
    - **Slot 13 (Center):** **[현재 위치 기록하기]** (깃펜 아이콘)
        
        - Lore: "현재 좌표: X:100, Y:64, Z:-200 (메사 바이옴)"
            
        - 기능: 투입된 종이를 소모하여 **[좌표 스크롤]** 아이템 생성.
            
    - **Slot 16 (OUT):** **[결과물]**
        

---

## 3. 핵심 구현 로직 (Code Logic & Strategy)

기능성 GUI 구현 시 가장 까다로운 **'아이템 감정(Identification)'**과 **'보안(Security)'** 로직 예시입니다.

### A. 대장간 감정 로직 (Identify Logic)

Java

```
public class ForgeGui extends DreamGui {
    // ... 생성자 생략

    @Override
    public void onGuiClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        
        // 1. 가공 버튼(Slot 13) 클릭 감지
        if (slot == 13) {
            ItemStack input = inventory.getItem(10); // 입력 슬롯
            
            // 유효성 검사: 미지의 광석인가?
            if (DreamWorkItems.isUnknownOre(input)) {
                processIdentify(input); // 감정 로직 실행
            } else {
                player.sendMessage("§c감정할 수 없는 아이템입니다.");
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            }
        }
        
        // 2. 결과 슬롯(Slot 16) 보호: 유저는 아이템을 가져갈 수만 있고, 놓을 순 없음
        if (slot == 16 && !event.getAction().name().contains("PICK")) {
            event.setCancelled(true);
        }
    }

    private void processIdentify(ItemStack input) {
        // 1. 비용 차감 (Economy) or 아이템 1개 차감
        input.setAmount(input.getAmount() - 1);
        
        // 2. 확률 계산 (RNG)
        double chance = Math.random();
        ItemStack result;
        
        if (chance < 0.05) result = DreamWorkItems.getDreamStone(); // 5% 대박
        else if (chance < 0.20) result = new ItemStack(Material.DIAMOND); // 15% 당첨
        else if (chance < 0.50) result = new ItemStack(Material.IRON_INGOT); // 30% 보통
        else result = new ItemStack(Material.COBBLESTONE); // 50% 꽝
        
        // 3. 결과 지급
        inventory.setItem(16, result);
        
        // 4. 효과 연출
        player.playSound(player, Sound.BLOCK_ANVIL_USE, 1f, 1f);
        playUpgradeAnimation(); // 유리판 색깔 바꾸기 등
    }
}
```

### B. GUI 닫을 때 아이템 반환 (Crucial Security)

이 코드가 없으면 서버 재부팅 시 아이템이 사라지거나 복사되는 버그가 발생합니다.

Java

```
@EventHandler
public void onInventoryClose(InventoryCloseEvent event) {
    if (event.getInventory().getHolder() instanceof DreamGui) {
        Inventory inv = event.getInventory();
        Player p = (Player) event.getPlayer();

        // 반환해야 할 슬롯 번호들 (입력 슬롯, 결과 슬롯 등)
        int[] returnSlots = {10, 11, 12, 16, 19, 20}; 

        for (int slot : returnSlots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                // 인벤토리에 공간이 있으면 넣고, 없으면 바닥에 떨어뜨림
                HashMap<Integer, ItemStack> leftOver = p.getInventory().addItem(item);
                for (ItemStack drop : leftOver.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), drop);
                }
            }
        }
    }
}
```

---

## 4. UI/UX 디테일 팁

1. **Ghost Item 방지:**
    
    - 서버 렉으로 인해 클라이언트에는 아이템이 보이지만 실제로는 없는 현상을 막기 위해, 중요한 거래 후에는 `player.updateInventory()`를 호출해 주는 것이 안전합니다. (deprecated 되었지만 여전히 유효한 경우가 많음)
        
2. **프로그레스 바 (Progress Bar):**
    
    - 대장간이나 요리에서 버튼을 누르면 즉시 결과가 나오는 것보다, 0.5초~1초 정도 유리판 색이 **흰색 -> 초록색**으로 차오르는 간단한 애니메이션(Scheduler 사용)을 보여주면 유저는 훨씬 큰 만족감을 느낍니다.
        
3. **사운드 스케이프:**
    
    - 대장간: `BLOCK_ANVIL_LAND` (묵직함)
        
    - 주방: `BLOCK_FIRE_EXTINGUISH` (치이익 소리)
        
    - 어시장: `ENTITY_FISHING_BOBBER_SPLASH` (첨벙 소리)
        

이로써 DreamWork의 핵심 GUI 3종 세트(메인, 상세, 기능성)의 설계가 완료되었습니다. 이 설계도면을 바탕으로 코딩을 시작하시면, 구조가 탄탄하여 개발 속도가 매우 빠를 것입니다.

이제 개발 순서는 **`DreamGui(추상)` -> `MainDashboard` -> `JobDetail` -> `MachineGui`** 순으로 진행하시는 것을 추천합니다. 추가로 필요한 데이터 테이블(Config) 예시가 필요하시면 말씀해 주세요!