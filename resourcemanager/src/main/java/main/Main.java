package main;

import simulator.core.DataCenterSimulationMain;
import common.configuration.Configuration;
import common.simulation.scenarios.Scenario;
import common.simulation.scenarios.Scenario1;

public class Main {

    public static void main(String[] args) throws Throwable {
        // TODO - change the random seed, have the user pass it in.
        long seed = 333; //System.currentTimeMillis();
        boolean useImprovedSparrow = true;
        Configuration configuration = new Configuration(seed, useImprovedSparrow);

        Scenario scenario = new Scenario1();
        scenario.setSeed(seed);
        scenario.getScenario().simulate(DataCenterSimulationMain.class);
    }
}
