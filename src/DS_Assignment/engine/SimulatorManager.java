package DS_Assignment.engine;

import DS_Assignment.model.graph.*;
import DS_Assignment.model.units.*;
import java.util.*;

public class SimulatorManager {
    private final Graph graph;
    private final Map<NodeName, List<Unit>> occupancyMap; // 노드 점유 상태를 관리할 해시 테이블(맵)
    private final List<Unit> allUnits;                    // 전체 유닛 리스트

    // 숙제 문서 상 필수 요구 자료구조 3번: 리스폰 대기열 (팀별 공유 큐)
    private final Queue<RespawnEntry> respawnQueueA;
    private final Queue<RespawnEntry> respawnQueueB;

    private int currentRound;
    private Team pointOwner;       // 거점(P)의 현재 소유 팀
    private int scoreA;            // A팀 점령도 (%)
    private int scoreB;            // B팀 점령도 (%)

    // 리스폰 큐에 라운드 정보 담을 내부 클래스
    private static class RespawnEntry {
        final Unit unit;
        final int respawnRound;

        RespawnEntry(Unit unit, int deathRound) {
            this.unit = unit;
            this.respawnRound = deathRound + 3; // K라운드 사망 시 K+3라운드 부활
        }
    }

    // 생성자
    public SimulatorManager() {
        this.graph = new Graph();
        this.occupancyMap = new EnumMap<>(NodeName.class);
        for (NodeName node : NodeName.values()) {
            occupancyMap.put(node, new ArrayList<>());
        }

        this.allUnits = new ArrayList<>();
        this.respawnQueueA = new LinkedList<>();
        this.respawnQueueB = new LinkedList<>();
        this.currentRound = 1;
        this.pointOwner = null;
        this.scoreA = 0;
        this.scoreB = 0;

        initMatch();
    }

    // 유닛 배치 및 초기화
    private void initMatch() {
        // 난수 시드 고정 필드, 같은 값으로 하면 같은 값이 나오는지 확인하기 위한 검증 용도
        int seed = 0;

        // 블루팀(A팀) 유닛 생성 및 배치 (SPAWN_A 시작)
        allUnits.add(new Tank(NodeName.SPAWN_A, seed + 1, Team.A));
        allUnits.add(new DPS(NodeName.SPAWN_A, seed + 2, Team.A));
        allUnits.add(new Healer(NodeName.SPAWN_A, seed + 3, Team.A));

        // 레드팀(B팀) 유닛 생성 및 배치 (SPAWN_B 시작)
        allUnits.add(new Tank(NodeName.SPAWN_B, seed + 4, Team.B));
        allUnits.add(new DPS(NodeName.SPAWN_B, seed + 5, Team.B));
        allUnits.add(new Healer(NodeName.SPAWN_B, seed + 6, Team.B));

        // 초기 해시 테이블 동기화
        for (Unit u : allUnits) {
            occupancyMap.get(u.getCurrentNodeId()).add(u);
        }
    }

    // 메인 시뮬레이션 루프 실행부, 무한 루프로 끝날 때까지 반복.
    public void simStart() {
        while (scoreA < 100 && scoreB < 100) {
            System.out.println(String.format("\n================ 🏁 ROUND %d ================", currentRound));

            // 라운드 시작 시 부활 체크 (K+3라운드 규칙)
            processRespawn(respawnQueueA, NodeName.SPAWN_A);
            processRespawn(respawnQueueB, NodeName.SPAWN_B);

            // 힐러 패시브 발동 (라운드 시작 시 주변 아군 40 회복)
            healPassiveActive();

            // 순서대로 턴 진행
            // A Tank -> B Tank -> A DPS -> B DPS -> A Healer -> B Healer
            List<Unit> turnOrder = getTurnOrder();
            for (Unit activeUnit : turnOrder) {
                if (!activeUnit.isAlive()) continue; // 사망자 스킵

                // 유닛의 목적지(targetNode) 선정
                NodeName targetNode = findTargetNode(activeUnit);

                // 최단 경로 계산 후 한 칸 이동
                boolean ignoreWeight = activeUnit instanceof Tank; // 현재 유닛이 탱커라면 위험도 무시
                Dijkstra.Result pathResult = Dijkstra.calculate(graph, activeUnit.getCurrentNodeId(), occupancyMap, activeUnit.getTeam(), ignoreWeight);
                List<NodeName> path = pathResult.getPathTo(targetNode);

                boolean hasMoved = false;
                if (path.size() > 1) {
                    NodeName nextNode = path.get(1); // 경로의 바로 다음 칸

                    boolean returnsToPrevious = nextNode == activeUnit.getPreviousNodeId();
                    boolean hasAlternativeMove = false;
                    for (NodeName neighbor : graph.getNeighbors(activeUnit.getCurrentNodeId())) {
                        if (neighbor != activeUnit.getPreviousNodeId()) {
                            hasAlternativeMove = true;
                            break;
                        }
                    }

                    // 아군 힐러 피격 시 긴급 구출 노드로 진입하려는지 판정하는 로직 추가
                    Unit myHealer = findUnit(activeUnit.getTeam(), "Healer");
                    boolean isEmergency = false;
                    if (myHealer != null && myHealer.isAlive()) {
                        isEmergency = (!getEnemiesAtNode(myHealer.getCurrentNodeId(), activeUnit.getTeam()).isEmpty())
                                && (targetNode == myHealer.getCurrentNodeId());
                    }

                    // 직전 노드 재진입은 막되, 스폰처럼 다른 이동 가능 노드가 없거나 긴급 구출 상황이면 허용
                    if (!returnsToPrevious || !hasAlternativeMove || isEmergency) {
                        // 해시 테이블 동기화
                        occupancyMap.get(activeUnit.getCurrentNodeId()).remove(activeUnit);
                        activeUnit.move(nextNode);
                        occupancyMap.get(activeUnit.getCurrentNodeId()).add(activeUnit);
                        hasMoved = true;
                        System.out.println(String.format("🏃 %s -> [%s] 노드로 이동", activeUnit.getDisplayName(), nextNode));
                    }
                }

                // 진입한 유닛은 다음 라운드부터 공격할 수 있음
                // 이번 턴에 이동하지 않고 제자리를 지켰거나 이미 교전 중이었던 유닛만 행동(Act).
                if (!hasMoved) {
                    // 제자리 대기 시 다음 턴 자유 기동을 위해 직전 라운드 이동 제약 초기화.
                    activeUnit.setPreviousNodeId(null);

                    Unit actionTarget = selectTarget(activeUnit);
                    if (actionTarget != null) {
                        activeUnit.act(actionTarget, occupancyMap);
                        // 행동 직후 사망자 발생 시 리스폰 큐 인입 처리
                        checkQueueDeath(actionTarget);
                    }
                }
            }

            // 라운드 종료 후 거점 소유권 & 점령도 연산 진행
            updateObjScore();

            // 이번 라운드 경과 출력
            printRoundStat();
            currentRound++;

            // 무한 루프 방지 용도
            if (currentRound > 200) {
                System.out.println("⚠️ 200라운드가 초과되어 경기가 종료됩니다.");
                break;
            }

            // 로그가 너무 빨리 지나가서 0.5초 대기 추가, 활성화하려면 주석 해제.
            // try {
            //     Thread.sleep(500);
            // } catch (InterruptedException e) {
            //     System.out.println("대기 과정 중 오류가 발생하였습니다.");
            // }
        }
        System.out.println(String.format("\n🏆 승리: %s 팀!", scoreA >= 100 ? "BLUE(A)" : "RED(B)"));
    }

    // 턴 순서 리스트를 반환.
    private List<Unit> getTurnOrder() {
        List<Unit> order = new ArrayList<>();
        String[] roles = {"Tank", "DPS", "Healer"};
        for (String role : roles) {
            for (Team t : Team.values()) {
                for (Unit u : allUnits) {
                    if (u.getTeam() == t && u.getRoleName().equals(role)) {
                        order.add(u);
                    }
                }
            }
        }
        return order;
    }

    // 유닛의 최적 목적지 판단 메소드
    private NodeName findTargetNode(Unit unit) {
        Team myTeam = unit.getTeam();
        Team enemyTeam = (myTeam == Team.A) ? Team.B : Team.A;

        // 긴급 이동, 아군 힐러 노드 침입 발생 시 최우선으로 해당 노드로 이동할 것.
        Unit myHealer = findUnit(myTeam, "Healer");
        if (myHealer != null && myHealer.isAlive()) {
            List<Unit> enemiesAtHealerNode = getEnemiesAtNode(myHealer.getCurrentNodeId(), myTeam);
            if (!enemiesAtHealerNode.isEmpty() && !(unit instanceof Healer)) {
                return myHealer.getCurrentNodeId(); // Tank와 DPS의 목적지를 힐러 노드로 설정.
            }
        }

        // 역할군별 일반 목적지 규칙 산출
        switch (unit.getRoleName()) {
            case "Tank":
                // 거점 확보 전이면 무조건 거점(P), 확보 완료 시 상대 Tank 추격
                if (pointOwner == myTeam) {
                    Unit enemyTank = findUnit(enemyTeam, "Tank");
                    if (enemyTank != null && enemyTank.isAlive()) {
                        NodeName enemySpawn = (enemyTeam == Team.A) ? NodeName.SPAWN_A : NodeName.SPAWN_B; // 적 본진 스폰 노드 식별
                        if (enemyTank.getCurrentNodeId() == enemySpawn) return NodeName.P; // 적 탱커가 적 본진에 있다면 스폰킬 방지를 위해 거점으로 복귀
                        return enemyTank.getCurrentNodeId();
                    }
                    // 탱커 사망 시 살아있는 잔여 상대 추격 (우선순위: Healer > DPS)
                    Unit enemyHealer = findUnit(enemyTeam, "Healer");
                    if (enemyHealer != null && enemyHealer.isAlive()) {
                        NodeName enemySpawn = (enemyTeam == Team.A) ? NodeName.SPAWN_A : NodeName.SPAWN_B; // 적 본진 스폰 노드 식별
                        if (enemyHealer.getCurrentNodeId() == enemySpawn) return NodeName.P; // 적 힐러가 적 본진에 있다면 스폰킬 방지를 위해 거점으로 복귀
                        return enemyHealer.getCurrentNodeId();
                    }
                    Unit enemyDps = findUnit(enemyTeam, "DPS");
                    if (enemyDps != null && enemyDps.isAlive()) {
                        NodeName enemySpawn = (enemyTeam == Team.A) ? NodeName.SPAWN_A : NodeName.SPAWN_B; // 적 본진 스폰 노드 식별
                        if (enemyDps.getCurrentNodeId() == enemySpawn) return NodeName.P; // 적 딜러가 적 본진에 있다면 스폰킬 방지를 위해 거점으로 복귀
                        return enemyDps.getCurrentNodeId();
                    }
                }
                return NodeName.P;

            case "DPS":
                // 최우선 목적지는 상대방 Healer, 사망 시 거점(P)
                Unit enemyHeal = findUnit(enemyTeam, "Healer");
                if (enemyHeal != null && enemyHeal.isAlive()) {
                    NodeName enemySpawn = (enemyTeam == Team.A) ? NodeName.SPAWN_A : NodeName.SPAWN_B; // 적 본진 스폰 노드 식별
                    if (pointOwner != myTeam && enemyHeal.getCurrentNodeId() == enemySpawn) { // 거점을 점령하지 못했거나 적이 본진에 있다면 스폰킬 방지
                        return NodeName.P; // 거점으로 목적지 강제 전환
                    }
                    return enemyHeal.getCurrentNodeId();
                }
                return NodeName.P;

            case "Healer":
                // 아군 탱커 생사 유무에 따른 수식 f(v) 기반 스마트 후퇴/합류
                Unit myTank = findUnit(myTeam, "Tank");
                return findBestHealerNode(unit, myTank, (myTeam == Team.A) ? NodeName.SPAWN_A : NodeName.SPAWN_B, enemyTeam);
        }
        return NodeName.P;
    }

    // 힐러 f(v) 목적지 평가 공식
    private NodeName findBestHealerNode(Unit healer, Unit tank, NodeName spawnNode, Team enemyTeam) {
        // [조건] Tank 사망 시 혹은 위치 사수 조건 발동 시의 예외 처리 선제 적용
        if (tank == null || !tank.isAlive()) {
            // Tank 사망 시 전체 맵 기준 f(v) 최솟값으로 리그룹 대기
            return findHealFv(healer, spawnNode, enemyTeam, Arrays.asList(NodeName.values()));
        }

        // Tank가 이미 근처에 있거나 내가 현재 교전 중이라면 포지션 유지
        List<NodeName> tankNeighbors = graph.getNeighbors(tank.getCurrentNodeId());
        boolean isTankNear = (healer.getCurrentNodeId() == tank.getCurrentNodeId() || tankNeighbors.contains(healer.getCurrentNodeId()));
        boolean isEngaged = !getEnemiesAtNode(healer.getCurrentNodeId(), healer.getTeam()).isEmpty();

        if (isTankNear || isEngaged) {
            return healer.getCurrentNodeId(); // 이동 중지 및 현재 위치 유지
        }

        // 평상시: 아군 탱커의 인접 노드들 후보군 중 f(v) 최소 노드 채택
        List<NodeName> candidates = new ArrayList<>(tankNeighbors);
        candidates.add(tank.getCurrentNodeId()); // 탱커의 현재 노드도 포함

        return findHealFv(healer, spawnNode, enemyTeam, candidates);
    }

    // 힐러 f(v) 계산 및 동점 처리 메소드
    private NodeName findHealFv(Unit healer, NodeName spawnNode, Team enemyTeam, List<NodeName> candidates) {
        NodeName bestNode = null;
        int minFv = Integer.MAX_VALUE;

        // 가중치 무시 다익스트라를 돌려 순수 홉(Hop) 거리 산출
        Dijkstra.Result hopResult = Dijkstra.calculate(graph, spawnNode, occupancyMap, healer.getTeam(), true);

        for (NodeName v : candidates) {
            int distanceToSpawn = hopResult.getDistances().get(v);

            // 해당 노드에 살아있는 적 DPS가 존재하는지 확인
            int enemyDpsDanger = 0;
            for (Unit u : occupancyMap.get(v)) {
                if (u.getTeam() == enemyTeam && u.getRoleName().equals("DPS") && u.isAlive()) {
                    enemyDpsDanger = 1;
                    break;
                }
            }

            int fv = distanceToSpawn + (2 * enemyDpsDanger); // f(v)

            if (fv < minFv) {
                minFv = fv;
                bestNode = v;
            } else if (fv == minFv) {
                // 동점 처리 1, 이전 위치(현재 위치) 유지 우선
                if (v == healer.getCurrentNodeId()) {
                    bestNode = v;
                }
                // 동점 처리 2, 노드 ID 사전 순(오름차순)으로 결정
                else if (bestNode != healer.getCurrentNodeId() && v.name().compareTo(bestNode.name()) < 0) {
                    bestNode = v;
                }
            }
        }
        return bestNode;
    }

    // 타겟팅 우선순위(Healer > DPS > Tank)를 기반으로 행동 대상 선정
    private Unit selectTarget(Unit unit) {
        NodeName currentNode = unit.getCurrentNodeId();
        List<Unit> unitsAtNode = occupancyMap.get(currentNode);
        Team myTeam = unit.getTeam();

        // 힐러라면 아군 치유 사거리(인접 1칸) 포함
        List<Unit> candidates = new ArrayList<>(unitsAtNode);
        if (unit instanceof Healer) {
            for (NodeName neighbor : graph.getNeighbors(currentNode)) {
                candidates.addAll(occupancyMap.get(neighbor));
            }
        }

        String[] actPrior = {"Healer", "DPS", "Tank"}; // 공격 상대 우선순위

        // 힐러라면 치유가 우선
        if (unit instanceof Healer) {
            // 아군 대상 치유 우선 탐색 (체력이 깎인 아군 선점)
            for (String role : actPrior) {
                for (Unit u : candidates) {
                    if (u.getTeam() == myTeam && u.getRoleName().equals(role) && u.isAlive() && u.getHp() < u.getMaxHp()) {
                        return u; // 치유 타겟 반환
                    }
                }
            }
        }

        // 적군 공격 타겟 탐색 (반드시 같은 노드의 적군만 타격 가능)
        for (String role : actPrior) {
            for (Unit u : unitsAtNode) {
                if (u.getTeam() != myTeam && u.getRoleName().equals(role) && u.isAlive()) {
                    return u; // 공격 타겟 반환
                }
            }
        }
        return null;
    }

    // 힐러 패시브
    private void healPassiveActive() {
        for (Unit u : allUnits) {
            // 힐러가 살아있을 때만 적용
            if (u instanceof Healer && u.isAlive()) {
                List<NodeName> targets = new ArrayList<>(graph.getNeighbors(u.getCurrentNodeId()));
                targets.add(u.getCurrentNodeId()); // 본인 포함

                for (NodeName node : targets) {
                    for (Unit ally : occupancyMap.get(node)) {
                        if (ally.getTeam() == u.getTeam() && ally.isAlive()) {
                            ally.heal(40); // 40 패시브 회복
                        }
                    }
                }
            }
        }
    }

    // K+3 라운드 리스폰 선입선출 제어 메소드
    private void processRespawn(Queue<RespawnEntry> queue, NodeName spawnNode) {
        while (!queue.isEmpty() && queue.peek().respawnRound <= currentRound) {
            RespawnEntry entry = queue.poll();
            Unit u = entry.unit;
            u.respawn(spawnNode); // 상태 초기화 및 재생성
            occupancyMap.get(spawnNode).add(u);
            System.out.println(String.format("✨ [부활] %s 유닛이 본진 [%s] 노드에 재생성되었습니다.", u.getDisplayName(), spawnNode));
        }
    }

    private void checkQueueDeath(Unit target) {
        if (!target.isAlive()) {
            occupancyMap.get(target.getCurrentNodeId()).remove(target); // 그래프 상에서 해당 유닛 제거
            if (target.getTeam() == Team.A) {
                respawnQueueA.add(new RespawnEntry(target, currentRound));
            } else {
                respawnQueueB.add(new RespawnEntry(target, currentRound));
            }
            System.out.println(String.format("💀 [전사] %s 가 치명상을 입고 전사하였습니다.", target.getDisplayName()));
        }
    }

    // 라운드 종료 시점 거점 점령 상태 및 점수 가산
    private void updateObjScore() {
        List<Unit> unitsAtPoint = occupancyMap.get(NodeName.P);
        int countA = 0;
        int countB = 0;

        for (Unit u : unitsAtPoint) {
            if (u.isAlive()) {
                if (u.getTeam() == Team.A) countA++;
                else countB++;
            }
        }

        // 거점 내부 교전 발생 여부를 판정할 필드.
        boolean contested = countA > 0 && countB > 0;

        // 단 한 팀의 유닛만 거점에 단독 존재할 때 소유권 변경.
        if (countA > 0 && countB == 0) pointOwner = Team.A;
        else if (countB > 0 && countA == 0) pointOwner = Team.B;

        // 거점 내 교전이 없을 경우 진척도 상승.
        if (!contested) {
            if (pointOwner == Team.A) {
                scoreA += 5;
                System.out.println("🚩 [거점] BLUE(A) 팀이 거점을 점령 중입니다. (+5%)");
            }
            else if (pointOwner == Team.B) {
                scoreB += 5;
                System.out.println("🚩 [거점] RED(B) 팀이 거점을 점령 중입니다. (+5%)");
            }
        } else {
            System.out.println("💥 [거점] 거점(P) 내 격돌 중입니다. (점령 진척도 상승 정지)");
        }
    }

    private Unit findUnit(Team team, String role) {
        for (Unit u : allUnits) {
            if (u.getTeam() == team && u.getRoleName().equals(role)) return u;
        }
        return null;
    }

    private List<Unit> getEnemiesAtNode(NodeName node, Team myTeam) {
        List<Unit> enemies = new ArrayList<>();
        for (Unit u : occupancyMap.get(node)) {
            if (u.getTeam() != myTeam && u.isAlive()) enemies.add(u);
        }
        return enemies;
    }

    private void printRoundStat() {
        System.out.println(String.format("\n점령 현황 -> \uD83D\uDD35 BLUE(A): %d%% | \uD83D\uDD34 RED(B): %d%% (현재 소유 팀: %s)",
                scoreA, scoreB, pointOwner == null ? "중립" : pointOwner.name()));
        for (Unit u : allUnits) {
            System.out.println("  " + u);
        }
    }
}