package DS_Assignment.model.units;

import DS_Assignment.model.graph.NodeName;

import java.util.Random;

public abstract class Unit {
    // 멤버 변수
    private int hp;
    private final int maxHp;
    private final int damage;
    private NodeName currentNodeId;
    private NodeName previousNodeId;
    private final Team team;

    // 고정 시드 상수 필드 및 난수 생성기
    private final int accSeed;  // 고정 시드 상수 필드
    protected Random random;    // 자식 클래스에서 독립 베르누이 시행에 접근할 수 있도록 protected 유지

    // 생성자
    public Unit(int maxHp, int damage, NodeName spawnNodeId, int accSeed, Team team) {
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.damage = damage;
        this.currentNodeId = spawnNodeId;
        this.previousNodeId = null; // 막 부활 혹은 초기 상태, 이전 방문 노드 없음.
        this.team = team;

        // 상수 필드 초기화 및 해당 시드로 개별 난수 생성기 가동
        this.accSeed = accSeed;
        this.random = new Random(accSeed);
    }

    // [추상 메서드] 자식 클래스 고유 이름 반환 (getClass().getSimpleName()으로 구현 예정)
    public abstract String getRoleName();

    // [추상 메서드] 자식 클래스별 고유 공격(및 치유) 메커니즘 구현
    public abstract void act(Unit target);

    // [공통 메서드] 팀과 이름을 한 번에 반환
    public String getDisplayName() { return getTeam().getIcon() + " " + getRoleName(); }

    // [공통 메서드] 이동 로직
    public void move(NodeName targetNodeId) {
        if (!isAlive()) return;
        this.previousNodeId = this.currentNodeId;
        this.currentNodeId = targetNodeId;
    }

    // [공통 메서드] 생존 여부 판정
    public boolean isAlive() {
        return hp > 0;
    }

    // [공통 메서드] 부활 로직 (상태 초기화)
    public void respawn(NodeName spawnNodeId) {
        this.hp = this.maxHp;
        this.currentNodeId = spawnNodeId;
        this.previousNodeId = null;
        // 부활 시 난수 시드 흐름을 초기 상태로 리셋하고 싶다면 아래 주석을 해제하세요.
        // this.random = new Random(accSeed); 
    }

    // [공통 메서드] 피격 및 치유
    public void takeDamage(int incomingDamage) {
        if (!isAlive()) return;
        this.hp -= incomingDamage;
        if (this.hp <= 0) {
            this.hp = 0;
        }
    }

    public void heal(int healAmount) {
        if (!isAlive()) return;
        this.hp = Math.min(this.hp + healAmount, this.maxHp);
    }

    @Override
    public String toString() {
        return String.format("[%s] 위치(노드): %s | HP: %d/%d | 상태: %s",
                getDisplayName(), currentNodeId, hp, maxHp, isAlive() ? "생존" : "사망(부활 대기)");
    }

    // 외부 및 자식 클래스 접근을 위한 Getter / Setter
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getDamage() { return damage; }
    public NodeName getCurrentNodeId() { return currentNodeId; }
    public Team getTeam() { return this.team; }
    public NodeName getPreviousNodeId() { return previousNodeId; }
    public void setPreviousNodeId(NodeName previousNodeId) { this.previousNodeId = previousNodeId; }

    // accSeed 조회를 위한 Getter (상수이므로 Setter는 불필요)
    public int getAccSeed() { return accSeed; }
}