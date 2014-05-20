package simulator.core;

import common.configuration.RmConfiguration;
import common.configuration.CyclonConfiguration;
import common.configuration.TManConfiguration;
import common.simulation.SimulatorInit;
import common.simulation.SimulatorPort;
import java.io.IOException;

import se.sics.kompics.ChannelFilter;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Kompics;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.model.king.KingLatencyMap;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;
import se.sics.kompics.p2p.bootstrap.server.BootstrapServer;
import se.sics.kompics.p2p.bootstrap.server.BootstrapServerInit;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.p2p.simulator.P2pSimulator;
import se.sics.kompics.p2p.simulator.P2pSimulatorInit;
import se.sics.kompics.simulation.SimulatorScheduler;
import se.sics.kompics.timer.Timer;

public final class DataCenterSimulationMain extends ComponentDefinition {

    private static final SimulationScenario scenario = SimulationScenario.load(System.getProperty("scenario"));
    private static final SimulatorScheduler simulatorScheduler = new SimulatorScheduler();

    public static void main(String[] args) {
        Kompics.setScheduler(simulatorScheduler);
        Kompics.createAndStart(DataCenterSimulationMain.class, 1);
    }

	
    public DataCenterSimulationMain() throws IOException {
        P2pSimulator.setSimulationPortType(SimulatorPort.class);

        Component bootstrapServer = create(BootstrapServer.class);
        Component p2pSimulator = create(P2pSimulator.class);
        Component rmSimulator = create(DataCenterSimulator.class);

        final BootstrapConfiguration bootConfig = BootstrapConfiguration.load(System.getProperty("bootstrap.configuration"));
        final CyclonConfiguration cyclonConfig = CyclonConfiguration.load(System.getProperty("cyclon.configuration"));
        final TManConfiguration tmanConfig = TManConfiguration.load(System.getProperty("tman.configuration"));
        final RmConfiguration rmConfig = RmConfiguration.load(System.getProperty("rm.configuration"));

        trigger(new SimulatorInit(bootConfig, cyclonConfig, tmanConfig, rmConfig), rmSimulator.getControl());
        trigger(new P2pSimulatorInit(simulatorScheduler, scenario, new KingLatencyMap()), p2pSimulator.getControl());
        trigger(new BootstrapServerInit(bootConfig), bootstrapServer.getControl());

        connect(bootstrapServer.getNegative(Network.class), p2pSimulator.getPositive(Network.class), new MessageDestinationFilter(bootConfig.getBootstrapServerAddress()));
        connect(bootstrapServer.getNegative(Timer.class), p2pSimulator.getPositive(Timer.class));
        connect(rmSimulator.getNegative(Network.class), p2pSimulator.getPositive(Network.class));
        connect(rmSimulator.getNegative(Timer.class), p2pSimulator.getPositive(Timer.class));
        connect(rmSimulator.getNegative(SimulatorPort.class), p2pSimulator.getPositive(SimulatorPort.class));

    }

	
    private final static class MessageDestinationFilter extends ChannelFilter<Message, Address> {

        public MessageDestinationFilter(Address address) {
            super(Message.class, address, true);
        }

        public Address getValue(Message event) {
            return event.getDestination();
        }
    }
}
