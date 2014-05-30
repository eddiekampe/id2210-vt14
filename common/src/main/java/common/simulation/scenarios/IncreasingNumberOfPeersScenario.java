package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

/**
 * Created by eddkam on 30/05/14.
 */
public class IncreasingNumberOfPeersScenario extends Scenario {

    private static SimulationScenario scenario = new SimulationScenario() {{

        // Start out with 50 peers
        StochasticProcess process0 = new StochasticProcess() {{
            eventInterArrivalTime(constant(1000));
            raise(25, Operations.peerJoin(),
                uniform(0, Integer.MAX_VALUE),
                constant(8), constant(12000)
            );
        }};

        // Ask for more resources than available in total
        StochasticProcess process1 = new StochasticProcess() {{
            eventInterArrivalTime(constant(100));
            raise(150, Operations.requestResources(),
                uniform(0, Integer.MAX_VALUE),
                constant(2), constant(2000),
                constant(10000 * 60 * 1)
            );
        }};

        // Add additional 50 peers
        StochasticProcess process2 = new StochasticProcess() {{
            eventInterArrivalTime(constant(1000));
            raise(50, Operations.peerJoin(),
                uniform(0, Integer.MAX_VALUE),
                constant(8), constant(12000)
            );
        }};

        // Ask for more resources after new peers
        StochasticProcess process3 = new StochasticProcess() {{
            eventInterArrivalTime(constant(100));
            raise(75, Operations.requestResources(),
                uniform(0, Integer.MAX_VALUE),
                constant(2), constant(2000),
                constant(10000 * 60 * 1)
            );
        }};

        StochasticProcess terminateProcess = new StochasticProcess() {{
            eventInterArrivalTime(constant(100));
            raise(1, Operations.terminate);
        }};

        // Start out with 50 peers
        process0.start();
        // Ask for more resources than available in total
        process1.startAfterTerminationOf(2000, process0);
        // Add additional 50 peers
        process2.startAfterTerminationOf(2000, process1);
        process3.startAfterTerminationOf(2000, process2);
        terminateProcess.startAfterTerminationOf(50000, process3);
    }};

    public IncreasingNumberOfPeersScenario() {
        super(scenario);
    }
}
