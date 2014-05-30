package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
public class Scenario1 extends Scenario {
	private static SimulationScenario scenario = new SimulationScenario() {{

    int scenario = 1;
    if (scenario == 1) {
      /**
       * Scenario1 100 peers 790 jobs
       * Peer (8, 12000)
       * Job  (1, 1000)
       */
      StochasticProcess process0 = new StochasticProcess() {{
        eventInterArrivalTime(constant(1000));
        raise(100, Operations.peerJoin(),
                      uniform(0, Integer.MAX_VALUE),
                      constant(8), constant(12000)
              );
      }};

      StochasticProcess process1 = new StochasticProcess() {{
              eventInterArrivalTime(constant(100));
              raise(790, Operations.requestResources(),
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
      //terminateProcess.startAfterTerminationOf(1000 * 1000, process1);

      // -Scenario1 END------------------------------------------------------------------

    } else if (scenario == 2) {

      /**
       * Scenario2 100 peers 501 jobs
       * Peer (10, 12000)
       * Job  (1, 1000)
       */
      StochasticProcess process0 = new StochasticProcess() {{
        eventInterArrivalTime(constant(1000));
        raise(100, Operations.peerJoin(),
            uniform(0, Integer.MAX_VALUE),
            constant(10), constant(12000)
        );
      }};

      StochasticProcess process1 = new StochasticProcess() {{
        eventInterArrivalTime(constant(100));
        raise(501, Operations.requestResources(),
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
      //terminateProcess.startAfterTerminationOf(1000 * 1000, process1);

      // -Scenario2 END------------------------------------------------------------------

    } else if (scenario == 3) {
      /**
       * Scenario3 100 peers 297 jobs
       * Peer (10, 12000)
       * Job 99 * (5, 1000)
       * 99 * (3, 1000)
       * 99 * (2, 1000)
       */
      StochasticProcess process0 = new StochasticProcess() {{
        eventInterArrivalTime(constant(1000));
        raise(100, Operations.peerJoin(),
            uniform(0, Integer.MAX_VALUE),
            constant(10), constant(12000)
        );
      }};

      StochasticProcess process1 = new StochasticProcess() {{
        eventInterArrivalTime(constant(100));
        raise(99, Operations.requestResources(),
            uniform(0, Integer.MAX_VALUE),
            constant(5), constant(1000),
            constant(10000 * 60 * 1)
        );
      }};

      StochasticProcess process2 = new StochasticProcess() {{
        eventInterArrivalTime(constant(100));
        raise(99, Operations.requestResources(),
            uniform(0, Integer.MAX_VALUE),
            constant(3), constant(1000),
            constant(10000 * 60 * 1)
        );
      }};

      StochasticProcess process3 = new StochasticProcess() {{
        eventInterArrivalTime(constant(100));
        raise(99, Operations.requestResources(),
            uniform(0, Integer.MAX_VALUE),
            constant(2), constant(1000),
            constant(10000 * 60 * 1)
        );
      }};

      StochasticProcess terminateProcess = new StochasticProcess() {{
        eventInterArrivalTime(constant(100));
        raise(1, Operations.terminate);
      }};

      process0.start();
      process1.startAfterTerminationOf(2000, process0);
      process2.startAfterTerminationOf(0, process1);
      process3.startAfterTerminationOf(0, process2);
      //terminateProcess.startAfterTerminationOf(1000 * 1000, process1);

      // -Scenario3 END------------------------------------------------------------------
    }

  }};

	// -------------------------------------------------------------------
	public Scenario1() {
		super(scenario);
	}
}
