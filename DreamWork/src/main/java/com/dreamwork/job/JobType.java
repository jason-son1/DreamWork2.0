package com.dreamwork.job;

/**
 * 직업 타입 열거형
 * 
 * DreamWork 서버의 5대 직업을 정의합니다.
 * 
 * @author DreamWork Team
 */
public enum JobType {

    /**
     * 광부 - 대지의 개척자 & 대장장이
     * 광물 채굴 및 합금 제련 담당
     */
    MINER("광부", "miner", "⛏", "§6", "대지의 개척자"),

    /**
     * 농부 - 대지의 관리자 & 미식가
     * 고품질 식량 생산 및 버프 요리 담당
     */
    FARMER("농부", "farmer", "🌾", "§a", "대지의 관리자"),

    /**
     * 어부 - 심해의 탐구자 & 항해사
     * 희귀 어종 수집 및 아쿠아리움 납품 담당
     */
    FISHER("어부", "fisher", "🎣", "§b", "심해의 탐구자"),

    /**
     * 사냥꾼 - 야생의 수호자 & 용병
     * 몬스터 소재 공급 및 보스 레이드 담당
     */
    HUNTER("사냥꾼", "hunter", "🏹", "§c", "야생의 수호자"),

    /**
     * 탐험가 - 지평선의 기록자
     * 맵 확장, 특수 지형 발견, 좌표/바이옴 정보 판매 담당
     */
    ADVENTURER("탐험가", "adventurer", "🗺", "§e", "지평선의 기록자");

    private final String displayName; // 한국어 이름
    private final String configKey; // 설정 파일 키 (소문자)
    private final String icon; // 이모지 아이콘
    private final String colorCode; // 대표 색상 코드
    private final String subtitle; // 부제목

    JobType(String displayName, String configKey, String icon, String colorCode, String subtitle) {
        this.displayName = displayName;
        this.configKey = configKey;
        this.icon = icon;
        this.colorCode = colorCode;
        this.subtitle = subtitle;
    }

    /**
     * 한국어 표시 이름
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 설정 파일에서 사용하는 키 (소문자)
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * 이모지 아이콘
     */
    public String getIcon() {
        return icon;
    }

    /**
     * 대표 색상 코드
     */
    public String getColorCode() {
        return colorCode;
    }

    /**
     * 부제목 (직업 설명)
     */
    public String getSubtitle() {
        return subtitle;
    }

    /**
     * 아이콘이 포함된 표시 이름
     */
    public String getFullDisplayName() {
        return icon + " " + displayName;
    }

    /**
     * 문자열로부터 JobType 찾기
     * 
     * @param input 직업 이름 또는 키
     * @return 해당 JobType, 없으면 null
     */
    public static JobType fromString(String input) {
        if (input == null)
            return null;

        String lower = input.toLowerCase().trim();

        for (JobType type : values()) {
            if (type.name().equalsIgnoreCase(lower)
                    || type.configKey.equals(lower)
                    || type.displayName.equals(input)) {
                return type;
            }
        }

        return null;
    }

    /**
     * 설정 파일 키로부터 JobType 찾기
     */
    public static JobType fromConfigKey(String key) {
        if (key == null)
            return null;

        for (JobType type : values()) {
            if (type.configKey.equalsIgnoreCase(key)) {
                return type;
            }
        }

        return null;
    }
}
