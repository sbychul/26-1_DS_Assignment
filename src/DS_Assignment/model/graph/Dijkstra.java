package DS_Assignment.model.graph;

import DS_Assignment.model.units.Team;
import DS_Assignment.model.units.Unit;
import java.util.*;

public class Dijkstra {

    // 다익스트라 계산 결과를 한 번에 반환하기 위한 내부 클래스
    public static class Result {
        private final Map<NodeName, Integer> distances;
        private final Map<NodeName, NodeName> predecessors;

        public Result(Map<NodeName, Integer> distances, Map<NodeName, NodeName> predecessors) {
            this.distances = distances;
            this.predecessors = predecessors;
        }

        public Map<NodeName, Integer> getDistances() { return distances; }
        public Map<NodeName, NodeName> getPredecessors() { return predecessors; }

        /**
         * 목적지 노드까지의 최적 경로를 추적하여 리스트로 반환합니다.
         */
        public List<NodeName> getPathTo(NodeName target) {
            // [안전장치] 다익스트라 거리가 무한대라면 도달 불가능한 노드이므로 즉시 빈 리스트 반환
            if (distances.get(target) == Integer.MAX_VALUE) {
                return Collections.emptyList();
            }

            LinkedList<NodeName> path = new LinkedList<>();
            NodeName curr = target;

            // 목적지부터 역추적하여 리스트의 맨 앞에 삽입 (Target -> ... -> Start)
            while (curr != null) {
                path.addFirst(curr);
                curr = predecessors.get(curr);
            }

            return path;
        }
    }

    // 우선순위 큐(Priority Queue)에서 거리와 노드를 함께 관리하기 위한 헬퍼 클래스
    private static class NodeEntry implements Comparable<NodeEntry> {
        final NodeName node;
        final int distance;

        NodeEntry(NodeName node, int distance) {
            this.node = node;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeEntry o) {
            // 1. 거리(비용)가 다르면 오름차순 정렬
            if (this.distance != o.distance) {
                return Integer.compare(this.distance, o.distance);
            }
            // 2. [명세 반영] 경로 비용이 동률일 경우, 노드 ID 사전 순(오름차순)으로 결정
            return this.node.name().compareTo(o.node.name());
        }
    }

    /**
     * 특정 출발 노드로부터 전체 노드까지의 최단 경로 및 동적 위험도 거리를 일괄 계산합니다. [cite: 7, 22]
     * @param graph 전장 그래프 객체
     * @param start 출발 노드 위치
     * @param occupancyMap 현재 맵의 유닛 점유율 맵(해시 테이블)
     * @param myTeam 경로를 계산하는 유닛의 소속 팀
     * @param ignoreWeight 위험도 가중치를 무시할지 여부 (탱커 유닛용 옵션)
     * @return 최단 거리 및 역추적 맵이 담긴 Result 객체
     */
    public static Result calculate(Graph graph, NodeName start,
                                   Map<NodeName, List<Unit>> occupancyMap,
                                   Team myTeam, boolean ignoreWeight) {

        Map<NodeName, Integer> distances = new EnumMap<>(NodeName.class);
        Map<NodeName, NodeName> predecessors = new EnumMap<>(NodeName.class);
        PriorityQueue<NodeEntry> pq = new PriorityQueue<>(); //

        // 1. 거리 배열 초기화 (출발지는 0, 나머지는 무한대)
        for (NodeName node : NodeName.values()) {
            distances.put(node, Integer.MAX_VALUE);
            predecessors.put(node, null);
        }

        distances.put(start, 0);
        pq.add(new NodeEntry(start, 0));

        // 2. 다익스트라 핵심 루프 가동
        while (!pq.isEmpty()) {
            NodeEntry current = pq.poll();
            NodeName u = current.node;

            // 큐에서 꺼낸 거리가 기록된 최단 거리보다 크면 무시 (오래된 정보 스킵)
            if (current.distance > distances.get(u)) continue;

            // 이웃 노드들을 순회하며 완화(Relaxation) 작업 수행
            for (NodeName v : graph.getNeighbors(u)) {
                // 탱커 등 위험도 무시 옵션이 켜져 있으면 가중치는 무조건 1, 아니면 동적 가중치 계산 [cite: 22, 24]
                int weight = ignoreWeight ? 1 : graph.getDynamicWeight(v, occupancyMap, myTeam);
                int newDist = distances.get(u) + weight;

                // 더 짧은 경로를 발견한 경우 거리 갱신
                if (newDist < distances.get(v)) {
                    distances.put(v, newDist);
                    predecessors.put(v, u);
                    pq.add(new NodeEntry(v, newDist));
                }
                // 만약 경로 비용이 완전히 일치하는 동률 상황이 발생한다면?
                else if (newDist == distances.get(v)) {
                    NodeName currentPred = predecessors.get(v);
                    // 새로 진입하려는 이전 노드(u)가 기존에 등록된 이전 노드보다 사전 순으로 앞선다면 갱신
                    if (currentPred != null && u.name().compareTo(currentPred.name()) < 0) {
                        predecessors.put(v, u);
                        // 거리는 같으므로 PQ에 다시 넣을 필요는 없음
                    }
                }
            }
        }

        return new Result(distances, predecessors);
    }
}