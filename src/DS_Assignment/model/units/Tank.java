package DS_Assignment.model.units;

import DS_Assignment.model.graph.NodeName;

public class Tank extends Unit {

    // 생성자: Tank의 고정 스탯(HP 350, ATK 85)을 부모 생성자에 주입 및 위치 난수 팀 설정
    public Tank(NodeName spawnNodeId, int accSeed, Team team) {
        super(350, 85, spawnNodeId, accSeed, team);
    }

    // 클래스명을 기반으로 역할군 반환
    @Override
    public String getRoleName() {
        return this.getClass().getSimpleName(); // "Tank" 반환
    }

    // 탱커 고유의 2회 타격 공격 메커니즘
    @Override
    public void act(Unit target) {
        // 내 생사 여부 및 타겟의 유효성 검증
        if (!isAlive() || target == null || !target.isAlive()) return;

        System.out.println(String.format("🛡️ [%s]의 턴, [%s] 노드에 있는 적 [%s]을(를) 타격합니다.",
                this.getDisplayName(), this.getCurrentNodeId(), target.getDisplayName()));

        int hitCount = 0;
        int totalDamage = 0;

        // 탱커는 총 2회의 공격을 수행
        for (int i = 1; i <= 2; i++) {
            // 발당 독립 베르누이 시행, 80% 명중률 판정.
            if (this.random.nextDouble() <= 0.80) {
                hitCount++;
                totalDamage += this.getDamage(); // 성공 시 발당 85의 피해
            }
        }

        // 타격 결과 콘솔 출력 및 데미지 적용
        if (hitCount > 0) {
            System.out.println(String.format("🎯 [결과] 2회의 공격 중 %d회 명중! (총 %d의 기본 피해)", hitCount, totalDamage));

            /* * [주의] 실제 데미지를 입힐 때는 피격 유닛이 속한 노드에
             * 상대 팀 탱커가 존재하여 패시브(20% 감면)가 켜지는지
             * GameManager 단계에서 해시 테이블을 조회하여 최종 데미지를 가공한 뒤
             * target.takeDamage(finalDamage); 로 호출하는 것을 강력히 권장합니다.
             */
            target.takeDamage(totalDamage);
        } else {
            System.out.println("💨 [결과] 모든 공격이 빗나갔습니다..");
        }
    }
}