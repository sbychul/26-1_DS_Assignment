package DS_Assignment.model.graph;

// 노드를 숫자가 아닌 Instruction에 명시된 문자열로 관리하기 위한 enum
public enum NodeName {
    SPAWN_A,  // Blue Spawn
    TL, TC, TR,
    ML,
    P,       // 거점 (Point)
    MR,
    BL, BC, BR,
    SPAWN_B   // Red Spawn
}