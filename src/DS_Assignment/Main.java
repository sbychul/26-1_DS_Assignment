package DS_Assignment;

import DS_Assignment.engine.SimulatorManager;

public class Main {
    public static void main(String[] args) {
        System.out.println("[동적 가중치 그래프 기반 멀티 유닛 전투 시뮬레이터]");
        SimulatorManager manager = new SimulatorManager();
        manager.simStart(); 
    }
}
