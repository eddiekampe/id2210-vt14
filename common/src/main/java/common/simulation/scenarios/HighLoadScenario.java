package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
/**
 * HighLoadScenario
 * 100 peers 750 jobs
 *
 * Peer (8, 12000)
 * Job  (1, 1000)
 */
public class HighLoadScenario extends Scenario {

    private static SimulationScenario scenario = new SimulationScenario() {{

        StochasticProcess process0 = new StochasticProcess() {{
            eventInterArrivalTime(constant(1000));
            raise(100, Operations.peerJoin(),
                uniform(0, Integer.MAX_VALUE),
                constant(8), constant(12000)
            );
        }};

        StochasticProcess process1 = new StochasticProcess() {{
            eventInterArrivalTime(constant(100));
            raise(750, Operations.requestResources(),
                uniform(0, Integer.MAX_VALUE),
                constant(1), constant(1000),
                constant(10000 * 60 * 1)
            );
        }};

        StochasticProcess terminateProcess = new StochasticProcess() {{
            eventInterArrivalTime(constant(100));
            raise(1, Operations.terminate);
        }};

        process0.start();
        process1.startAfterTerminationOf(2000, process0);
        terminateProcess.startAfterTerminationOf(10000, process1);
    }};

    public HighLoadScenario() {
        super(scenario);
    }
}
