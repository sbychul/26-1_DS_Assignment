package DS_Assignment.model.graph;

import java.util.*;

public class Graph {
    // 각 노드별 인접한 이웃 노드들을 저장하는 인접 리스트
    private final Map<NodeName, List<NodeName>> adjList;

    public Graph() {
        this.adjList = new HashMap<>();
        // 11개의 정점 초기화
        for (NodeName node : NodeName.values()) {
            adjList.put(node, new ArrayList<>());
        }
        initializeEdges(); // 전장 간선 연결 설정
    }

    // PDF 이미지 기준 무방향 간선 구조 세팅
    private void initializeEdges() {
        // 1. 스폰 지역 연결 [cite: 13]
        addEdge(NodeName.SpawnA, NodeName.ML);
        addEdge(NodeName.SpawnB, NodeName.MR);

        // 2. 외곽 링 구조 연결
        addEdge(NodeName.TL, NodeName.TC); addEdge(NodeName.TC, NodeName.TR);
        addEdge(NodeName.TR, NodeName.MR); addEdge(NodeName.MR, NodeName.BR);
        addEdge(NodeName.BR, NodeName.BC); addEdge(NodeName.BC, NodeName.BL);
        addEdge(NodeName.BL, NodeName.ML); addEdge(NodeName.ML, NodeName.TL);

        // 3. 거점(P) 중심의 방사형(스타) 구조 연결
        addEdge(NodeName.P, NodeName.TL); addEdge(NodeName.P, NodeName.TC); addEdge(NodeName.P, NodeName.TR);
        addEdge(NodeName.P, NodeName.ML);                                  addEdge(NodeName.P, NodeName.MR);
        addEdge(NodeName.P, NodeName.BL); addEdge(NodeName.P, NodeName.BC); addEdge(NodeName.P, NodeName.BR);
    }

    // 무방향 그래프이므로 양방향 모두 추가
    private void addEdge(NodeName source, NodeName destination) {
        adjList.get(source).add(destination);
        adjList.get(destination).add(source);
    }

    // 특정 노드의 이웃 노드 리스트 반환 (다익스트라 탐색용)
    public List<NodeName> getNeighbors(NodeName node) {
        return adjList.get(node);
    }

    /**
     * ✨ [핵심] 동적 가중치(위험도) 계산 메서드
     * 간선의 기본 위험도는 1이지만, 목적지 노드에 적 탱커나 DPS가 존재하면 위험도 2가 추가됩니다.
     * @param targetNode 진입하려는 목적지 노드
     * @param enemyPositions 현재 적군들의 위치 정보가 담긴 구조 (GameManager에서 제공 예정)
     * @return 최종 계산된 동적 위험도 비용
     */
    public int getDynamicWeight(NodeName targetNode, Set<NodeName> enemyPositions) {
        int baseWeight = 1; // 기본 위험도

        // 목적지에 적(Tank 또는 DPS)이 존재한다면 위험도 2 추가
        if (enemyPositions.contains(targetNode)) {
            baseWeight += 2; //
        }

        return baseWeight;
    }
}