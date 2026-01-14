DreamWork 플러그인에서 EconomyShop 플러그인의 시스템을 API처럼 활용하기 위한 **구조적 설계(Architecture Design)**를 제안해 드릴게요.

이미 `DreamWork` 내에 `Hook` 관련 패키지(`com.dreamwork.hook`)가 존재하고, `EconomyShop`도 명확한 매니저 구조(`Manager` 패턴)를 가지고 있으므로, **"Hook 패턴"**을 사용하여 연동하는 것이 가장 깔끔합니다.

다음은 구현을 위한 단계별 구상입니다.

---

### 1. 전체적인 구조 (Architecture)

DreamWork가 EconomyShop의 기능을 직접 수정하는 것이 아니라, **EconomyShop이 제공하는 공개 메서드(Public Method)나 매니저 클래스를 호출**하는 방식(Wrapper/Hook)으로 설계해야 합니다.

- **DreamWork (Core)**: 상점 열기, 판매 요청 등을 보냄.
    
- **EconomyShopHook (Bridge)**: DreamWork와 EconomyShop 사이의 통역사 역할. EconomyShop이 서버에 설치되어 있는지 확인하고, 안전하게 메서드를 호출함.
    
- **EconomyShop (External Plugin)**: 실제 GUI 출력, 가격 계산, 거래 처리를 담당.
    

---

### 2. 단계별 구현 구상

#### 1단계: 의존성 설정 (Dependency Setup)

가장 먼저 두 플러그인이 서로를 인식할 수 있게 해야 합니다.

- **plugin.yml (`DreamWork`)**:
    
    - `softdepend: [EconomyShop]` 또는 `depend: [EconomyShop]`을 추가합니다.
        
    - `softdepend`를 권장합니다. (EconomyShop이 없어도 DreamWork 서버가 켜질 수는 있게 하기 위함)
        
- **Build Path**:
    
    - 프로젝트 라이브러리에 `EconomyShop.jar`를 추가하여 코드를 참조할 수 있게 합니다.
        

#### 2단계: Hook 클래스 생성 (`EconomyShopHook`)

`com.dreamwork.hook` 패키지에 `EconomyShopHook.java`를 생성하여 EconomyShop의 API에 접근하는 창구를 만듭니다.

- **주요 역할**:
    
    - `EconomyShop` 플러그인 인스턴스 가져오기.
        
    - EconomyShop의 `ShopManager`나 `EconomyManager` 인스턴스 확보.
        
    - DreamWork에서 필요한 기능을 메서드로 정의 (예: `openShop(Player, ShopType)`).
        

#### 3단계: 기능별 연동 전략

EconomyShop의 파일 구조를 분석했을 때, 다음과 같은 클래스들과 연결이 필요합니다.

A. 상점 GUI 열기 (Opening Shop GUI)

DreamWork의 NPC(대장장이, 상인 등)를 클릭했을 때 EconomyShop의 GUI를 띄우는 기능입니다.

- **EconomyShop 타겟**: `me.antigravity.economyshop.manager.ShopManager`
    
    - 이 매니저에는 특정 카테고리(섹션)의 상점을 여는 메서드(예: `openShopSection(Player, String section)`)가 존재할 것입니다.
        
- **DreamWork 구현**:
    
    - `HookManager`를 통해 `EconomyShopHook.openShop(player, "farming")`과 같이 호출하면, 훅 내부에서 EconomyShop의 `ShopManager`를 호출해 GUI를 엽니다.
        

B. 아이템 가격 가져오기 (Price Checking)

DreamWork의 미션이나 보상 시스템에서 특정 아이템의 가치를 알아야 할 때 사용합니다.

- **EconomyShop 타겟**: `me.antigravity.economyshop.manager.ShopManager` 또는 `EconomyManager`
    
    - 아이템(`ItemStack`)을 넣으면 `ShopItem` 객체를 반환하거나 `double price`를 반환하는 메서드를 찾습니다.
        
- **DreamWork 구현**:
    
    - `EconomyShopHook.getSellPrice(ItemStack item)` 메서드를 만듭니다.
        
    - DreamWork의 직업(Job) 보너스를 계산하기 전, 기본 시세를 여기서 가져옵니다.
        

C. 판매 로직 위임 (Delegating Selling)

DreamWork의 "모두 팔기" 명령어나 특정 GUI에서 판매를 수행할 때, 실제 돈 지급과 아이템 회수는 EconomyShop 로직을 따르게 합니다.

- **EconomyShop 타겟**: `me.antigravity.economyshop.manager.EconomyManager` (거래 처리)
    
- **DreamWork 구현**:
    
    - DreamWork는 판매할 아이템 목록만 추려내고, 실제 처리는 `EconomyShopHook.sellItems(...)`로 넘깁니다.
        
    - 이렇게 하면 EconomyShop에 설정된 **가격 변동(Dynamic Pricing)이나 제한(LimitManager)** 등이 자동으로 적용됩니다.
        

#### 4단계: 직업 버프 및 이벤트 처리 (Advanced)

DreamWork의 핵심인 "직업 레벨에 따른 판매 가격 보너스"를 적용하려면 이벤트 리스너가 필요합니다.

- **EconomyShop 이벤트 리스닝**:
    
    - EconomyShop이 아이템을 판매할 때 발생하는 이벤트(예: `ShopTransactionEvent` 또는 `PreSellEvent` 등, 해당 플러그인이 이벤트를 지원한다면)를 `com.dreamwork.listener`에서 리스닝합니다.
        
- **로직**:
    
    1. 판매 이벤트 감지.
        
    2. 플레이어의 DreamWork 직업(Job) 및 레벨 확인.
        
    3. 직업 스킬(예: `Merchant's Insight`)에 따라 판매 가격(Price)에 배율(Multiplier) 적용.
        
    4. 이벤트의 가격을 수정(`event.setPrice(...)`).
        

---

### 3. 기존 코드 정리 (Refactoring)

이 연동을 진행하면서 DreamWork의 기존 코드 중 일부는 수정이 필요합니다.

- **`com.dreamwork.shop` 패키지 축소**:
    
    - 자체적으로 구현했던 상점 GUI(`ShopGui.java`), 아이템 가격 설정 파일, 구매/판매 로직은 제거하거나 EconomyShopHook을 사용하는 형태로 변경합니다.
        
- **`items` 폴더 의존도 감소**:
    
    - `resources/items/*.yml`에 정의된 가격 정보 대신, EconomyShop의 설정(`shop/*.yml`)을 따르게 되므로 중복 데이터를 관리할 필요가 없어집니다.
        

### 요약

1. **Hook 클래스**를 만들어 EconomyShop의 `ShopManager`를 래핑(Wrapping)하세요.
    
2. 상점 열기는 **API 호출**로 대체하고, 자체 GUI 코드는 제거하세요.
    
3. 직업 보너스는 EconomyShop의 **이벤트 리스너**를 통해 가격을 수정(Modify)하는 방식으로 구현하세요.
    

이 구조로 가면 상점 시스템 관리(가격, 품목)는 EconomyShop에서 편하게 하고, DreamWork는 RPG 요소(직업, 스킬)에만 집중할 수 있게 됩니다.



제공해주신 `EconomyShop` 플러그인의 소스 코드를 분석하여, 다른 플러그인에서 이 플러그인의 기능을 연동(Hook)하여 사용할 수 있도록 **API 명세서** 형태로 정리해 드립니다.

이 플러그인은 `EconomyShop` 메인 클래스를 통해 **싱글톤(Singleton)** 패턴으로 각 매니저(Manager)에 접근할 수 있는 구조로 되어 있습니다.

---

# 🛠️ EconomyShop API 개발 가이드

다른 플러그인에서 `EconomyShop`의 기능을 사용하기 위해 필요한 핵심 클래스와 메서드 정보입니다.

## 1. 프로젝트 설정 (Dependency)

가장 먼저 `plugin.yml`에 의존성을 추가하여 `EconomyShop`이 먼저 로드되도록 해야 합니다.

**plugin.yml**

YAML

```
depend: [EconomyShop]
# 또는 선택적 의존성일 경우
softdepend: [EconomyShop]
```

## 2. 플러그인 인스턴스 접근 (Entry Point)

모든 기능은 `EconomyShop.getInstance()`를 통해 접근합니다.

Java

```
import me.antigravity.economyshop.EconomyShop;

public class MyHook {
    private EconomyShop economyShop;

    public void hook() {
        if (Bukkit.getPluginManager().getPlugin("EconomyShop") != null) {
            this.economyShop = EconomyShop.getInstance();
        }
    }
}
```

---

## 3. 주요 매니저 및 기능 분석

`EconomyShop` 클래스에서 제공하는 Getter를 통해 다음 매니저들에 접근할 수 있습니다.

### A. 상점 데이터 관리 (`ShopManager`)

상점의 카테고리(Section)와 아이템 데이터를 조회하거나 수정할 때 사용합니다.

- **접근:** `economyShop.getShopManager()`
    

|**리턴 타입**|**메서드**|**설명**|
|---|---|---|
|`Map<String, ShopSection>`|`getSections()`|로드된 모든 상점 섹션(카테고리) 목록을 가져옵니다.|
|`void`|`loadShops()`|파일에서 상점 데이터를 다시 로드합니다.|
|`void`|`saveShopItem(ShopSection section, ShopItem item)`|특정 아이템의 변경 사항(가격, 슬롯 등)을 파일 및 DB에 저장합니다.|
|`void`|`deleteShopItem(ShopSection section, ShopItem item)`|특정 아이템을 상점에서 제거하고 파일에서도 삭제합니다.|

**활용 예시:** 특정 상점의 아이템 가격을 외부 플러그인에서 조정하고 저장하기

Java

```
ShopManager shopManager = EconomyShop.getInstance().getShopManager();
ShopSection farmingSection = shopManager.getSections().get("farming");

if (farmingSection != null) {
    for (ShopItem item : farmingSection.getItems()) {
        if (item.getId().equals("WHEAT")) {
            item.setBuyPrice(50.0); // 가격 수정
            shopManager.saveShopItem(farmingSection, item); // 변경 사항 저장
        }
    }
}
```

### B. GUI 제어 (`GUIManager`)

플레이어에게 상점 창을 강제로 열어주거나 네비게이션을 제어할 때 사용합니다.

- **접근:** `economyShop.getGuiManager()`
    

|**리턴 타입**|**메서드**|**설명**|
|---|---|---|
|`void`|`openMainMenu(Player player)`|상점 메인 메뉴(카테고리 선택창)를 엽니다.|
|`void`|`openShop(Player player, ShopSection section)`|특정 카테고리(섹션)의 상점 페이지를 엽니다.|
|`void`|`openItemEditor(Player player, ShopSection section, ShopItem item)`|관리자용 아이템 에디터 GUI를 엽니다.|

**활용 예시:** 명령어로 특정 상점 카테고리 열기

Java

```
GUIManager guiManager = EconomyShop.getInstance().getGuiManager();
ShopSection section = EconomyShop.getInstance().getShopManager().getSections().get("blocks");

if (section != null) {
    guiManager.openShop(player, section);
}
```

### C. 경제 시스템 통합 (`EconomyManager`)

이 플러그인은 Vault, PlayerPoints, EXP, 아이템(에메랄드 등) 등 다양한 화폐를 통합 관리합니다. 외부 플러그인에서도 이 통합된 경제 시스템을 활용할 수 있습니다.

- **접근:** `economyShop.getEconomyManager()`
    

|**리턴 타입**|**메서드**|**설명**|
|---|---|---|
|`EconomyProvider`|`getProvider(String name)`|특정 경제 시스템(예: "Vault", "EXP", "Item:DIAMOND")을 가져옵니다.|
|`EconomyProvider`|`getDefaultProvider()`|`config.yml`에 설정된 기본 경제 시스템을 가져옵니다.|
|`EconomyProvider`|`getProvider(ShopSection section, ShopItem item)`|특정 아이템이 사용하는 경제 시스템을 자동으로 판단하여 가져옵니다.|

**활용 예시:** 플레이어에게서 아이템 구매 비용 차감하기 (화폐 종류 신경 안 쓰고)

Java

```
EconomyManager ecoManager = EconomyShop.getInstance().getEconomyManager();
// 해당 아이템에 설정된 화폐 시스템 가져오기 (예: 다이아몬드 상점이면 다이아몬드 차감)
EconomyProvider provider = ecoManager.getProvider(section, shopItem);

if (provider.has(player, 1000)) {
    provider.withdraw(player, 1000);
    player.sendMessage("구매 완료!");
}
```

### D. 기타 유용한 매니저

- **`LimitManager`**: 플레이어별 구매 제한 정보를 확인하거나 조작할 때 사용될 것으로 보입니다. (상세 코드는 제공되지 않았으나 `EconomyShop.java`에 Getter 존재)
    
- **`ConfigManager`**: 플러그인의 설정을 가져올 때 사용합니다. (`getMainConfig()`, `getSectionsConfig()` 등)
    

---

## 4. 데이터 모델 (Data Objects)

API를 사용할 때 자주 마주치게 될 객체들입니다.

### `ShopSection` (상점 카테고리)

- **필드:** `id` (식별자), `displayName` (표시 이름), `icon` (아이콘 아이템), `items` (포함된 `ShopItem` 리스트), `economy` (사용 화폐 종류)
    
- **역할:** 하나의 상점 페이지(예: 광물 상점, 농작물 상점)를 나타냅니다.
    

### `ShopItem` (판매 아이템)

- **필드:** `id`, `buyPrice`, `sellPrice`, `slot`, `dynamicPricing` (동적 가격 여부), `currentStock` (현재 재고), `maxStock` (최대 재고)
    
- **Builder 패턴 사용:** `ShopItem.builder().id("TEST").buyPrice(100)...build()` 형태로 생성 가능합니다.
    
- **특이 사항:** `itemStackLoader`를 통해 아이템 정보를 지연 로딩(Lazy Loading)합니다.
    

---

## 5. 요약: 외부 플러그인에서의 활용 시나리오

1. **커스텀 명령어/NPC 연결:**
    
    - `GUIManager.openShop()`을 사용하여 특정 조건에서 상점을 엽니다.
        
2. **동적 시세 조작:**
    
    - 게임 내 이벤트(예: 흉작) 발생 시 `ShopManager`를 통해 농작물 가격(`buyPrice`, `sellPrice`)을 인상하고 `saveShopItem()`으로 저장합니다.
        
3. **통합 경제 API 활용:**
    
    - 자체 플러그인에서 보상을 지급할 때 `EconomyManager`를 통해 `EconomyShop`이 지원하는 다양한 화폐(EXP, 아이템 등)로 지급할 수 있습니다.
        

이 분석 정보를 바탕으로 개발하시려는 플러그인과 연동 코드를 작성하시면 됩니다. 추가로 궁금한 부분이 있다면 말씀해 주세요!