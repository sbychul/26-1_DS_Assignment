package DS_Assignment.model.units;

import DS_Assignment.model.graph.NodeName;

public class Healer extends Unit {

    // 생성자: Healer의 고정 스탯(HP 200, 적 대상 공격력 65)을 부모 생성자에 주입 및 위치, 난수, 팀 설정
    // 치유량 40은 아군 대상 적용 시 내부 상수로 처리, 부모에게는 기본 공격력인 65를 전달.
    public Healer(NodeName spawnNodeId, int accSeed, Team team) {
        super(200, 65, spawnNodeId, accSeed, team);
    }

    // 클래스명을 기반으로 역할군 반환
    @Override
    public String getRoleName() {
        return this.getClass().getSimpleName(); // "Healer" 반환
    }

    // 힐러 고유의 3회 사격 메커니즘 (아군이면 발당 40 치유 / 적군이면 발당 65 피해)
    @Override
    public void act(Unit target) {
        // 자기 자신은 치유(행동) 불가.
        if (target == this) return;

        // 내 생사 여부 및 타겟의 유효성 검증
        if (!isAlive() || target == null || !target.isAlive()) return;

        // 힐러의 메커니즘에 근거하여 타겟이 나와 같은 팀인지 여부에 따라 행동 및 로그 분기
        boolean isAllied = (this.getTeam() == target.getTeam());
        String actionText = isAllied ? "치유 사격을 개시합니다. ➕" : "대응 사격을 개시합니다. ☄️";

        System.out.println(String.format("🔮 [%s]의 턴! [%s] 노드에 있는 %s [%s]에게 %s",
                this.getDisplayName(), this.getCurrentNodeId(),
                isAllied ? "아군" : "적군", target.getDisplayName(), actionText));

        int hitCount = 0;
        int totalEffect = 0;

        // Healer는 총 3회의 독립적인 사격을 수행 (발당 독립 베르누이 시행)
        for (int i = 1; i <= 3; i++) {
            // 명중률 판정 (60% 확률)
            if (this.random.nextDouble() <= 0.60) {
                hitCount++;
                // 아군이면 발당 40 치유, 적군이면 본인의 기본 공격력(65)만큼 피해 누적
                totalEffect += isAllied ? 40 : this.getDamage();
            }
        }

        // 주사위 결과에 따른 최종 효과 적용
        if (hitCount > 0) {
            if (isAllied) {
                System.out.println(String.format("⚕\uFE0F [결과] 3회 사격 중 %d회 명중! (총 %d의 아군 체력 회복)", hitCount, totalEffect));
                target.heal(totalEffect);
            } else {
                System.out.println(String.format("🎯 [결과] 3회 사격 중 %d회 명중! (총 %d의 기본 피해)", hitCount, totalEffect));

                /* * [주의] 적군에게 피해를 입힐 때는 피격 유닛이 속한 노드에
                 * 상대 팀 탱커가 존재하여 패시브(20% 감면)가 켜지는지
                 * GameManager 단계에서 해시 테이블을 조회하여 최종 데미지를 가공한 뒤
                 * target.takeDamage(finalDamage); 로 호출하는 것을 권장합니다.
                 */
                target.takeDamage(totalEffect);
            }
        } else {
            System.out.println("💨 [결과] 모든 사격이 빗나갔습니다..");
        }
    }
}