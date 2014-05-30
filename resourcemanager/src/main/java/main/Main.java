package main;

import common.simulation.scenarios.HighLoadScenario;
import common.simulation.scenarios.IncreasingNumberOfPeersScenario;
import simulator.core.DataCenterSimulationMain;
import common.configuration.Configuration;
import common.simulation.scenarios.Scenario;
import common.simulation.scenarios.Scenario1;

public class Main {

    public static void main(String[] args) throws Throwable {

        /**
         * Instructions for
         * Attack of the Borg: Decentralized Resource Management
         *
         * Configurable variables
         *
         * seed
         *  Set to a fixed number to reproduce randomness
         * useImprovedSparrow
         *  Set whether to use improved version of Sparrow or not
         */
        long seed = 333; //System.currentTimeMillis();
        boolean useImprovedSparrow = false;

        Configuration configuration = new Configuration(seed, useImprovedSparrow);

        //Scenario scenario = new Scenario1();
        Scenario scenario = new IncreasingNumberOfPeersScenario();
        scenario.setSeed(seed);
        scenario.getScenario().simulate(DataCenterSimulationMain.class);
    }
}