package DS_Assignment.model.units;

// 팀을 나누기 위한 enum
public enum Team {
    A("🟦"),
    B("🟥");

    // 팀별 색상을 나타내는 아이콘 저장
    private final String icon;

    // enum 생성자
    Team(String icon) {
        this.icon = icon;
    }

    // Getter
    public String getIcon() {
        return icon;
    }
}