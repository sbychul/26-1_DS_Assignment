package DS_Assignment.model.units;

import DS_Assignment.model.graph.NodeName;

public class DPS extends Unit {

    // 생성자: DPS의 고정 스탯(HP 200, ATK 30)을 부모 생성자에 주입 및 위치, 난수, 팀 설정
    public DPS(NodeName spawnNodeId, int accSeed, Team team) {
        super(200, 30, spawnNodeId, accSeed, team);
    }

    // 클래스명을 기반으로 역할군 반환
    @Override
    public String getRoleName() {
        return this.getClass().getSimpleName(); // "DPS" 반환
    }

    // DPS 고유의 10회 사격 및 30% 확률 치명타(1.5배) 패시브 메커니즘
    @Override
    public void act(Unit target) {
        // 내 생사 여부 및 타겟의 유효성 검증
        if (!isAlive() || target == null || !target.isAlive()) return;

        System.out.println(String.format("⚔️ [%s]의 턴, [%s] 노드에 있는 적 [%s]을(를) 10회 사격합니다.",
                this.getDisplayName(), this.getCurrentNodeId(), target.getDisplayName()));

        int hitCount = 0;
        int critCount = 0;
        int totalDamage = 0;

        // DPS는 총 10회의 독립적인 사격을 수행 (발당 독립 베르누이 시행)
        for (int i = 1; i <= 10; i++) {
            // 1. 명중률 판정 (60% 확률)
            if (this.random.nextDouble() <= 0.60) {
                hitCount++;

                // 2. 패시브 판정: 명중한 탄환당 독립적으로 30% 확률 치명타(1.5배 피해) 발생
                if (this.random.nextDouble() <= 0.30) {
                    critCount++;
                    totalDamage += (int) (this.getDamage() * 1.5); // 30 * 1.5 = 45 피해
                } else {
                    totalDamage += this.getDamage(); // 일반 명중 시 30 피해
                }
            }
        }

        // 사격 결과 콘솔 출력 및 데미지 적용
        if (hitCount > 0) {
            System.out.println(String.format("🎯 [결과] 10회 사격 중 %d회 명중! (치명타 %d회 / 총 %d의 기본 피해)",
                    hitCount, critCount, totalDamage));

            /* * [주의] 실제 데미지를 입힐 때는 피격 유닛이 속한 노드에
             * 상대 팀 탱커가 존재하여 패시브(20% 감면)가 켜지는지
             * GameManager 단계에서 해시 테이블을 조회하여 최종 데미지를 가공한 뒤
             * target.takeDamage(finalDamage); 로 호출하는 것을 강력히 권장합니다.
             */
            target.takeDamage(totalDamage);
        } else {
            System.out.println("💨 [결과] 10발이 모두 빗나갔습니다...");
        }
    }
}