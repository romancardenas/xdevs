package mitris.sim.core.simulation;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import mitris.logger.core.MitrisLogger;

import mitris.sim.core.Constants;
import mitris.sim.core.lib.examples.Efp;
import mitris.sim.core.modeling.Coupled;
import mitris.sim.core.modeling.Coupling;
import mitris.sim.core.modeling.InPort;
import mitris.sim.core.modeling.OutPort;
import mitris.sim.core.modeling.api.ComponentInterface;
import mitris.sim.core.modeling.api.AtomicInterface;
import mitris.sim.core.modeling.api.CoupledInterface;
import mitris.sim.core.simulation.api.CoordinatorInterface;
import mitris.sim.core.simulation.api.SimulatorInterface;
import mitris.sim.core.simulation.api.SimulationClock;
import mitris.sim.core.util.Util;

/**
 *
 * @author José Luis Risco Martín
 */
public class Coordinator extends AbstractSimulator implements CoordinatorInterface {

    private static final Logger logger = Logger.getLogger(Coordinator.class
            .getName());

    protected CoupledInterface model;
    protected LinkedList<SimulatorInterface> simulators = new LinkedList<>();

    public Coordinator(SimulationClock clock, Coupled model, boolean flatten) {
        super(clock);
        logger.fine("Hierarchical...\n" + Util.printCouplings(model));
        if (flatten) {
            this.model = model.flatten();
        } else {
            this.model = model;
        }
        // Build hierarchy
        Collection<ComponentInterface> components = model.getComponents();
        for (ComponentInterface component : components) {
            if (component instanceof Coupled) {
                Coordinator coordinator = new Coordinator(clock,
                        (Coupled) component, false);
                simulators.add(coordinator);
            } else if (component instanceof AtomicInterface) {
                Simulator simulator = new Simulator(clock,
                        (AtomicInterface) component);
                simulators.add(simulator);
            }
        }

        logger.fine("After flattening.....\n" + Util.printCouplings(this.model));
        logger.fine(this.model.toString());
        Iterator<ComponentInterface> itr = this.model.getComponents().iterator();
        while (itr.hasNext()) {
            logger.fine("Component: " + itr.next());
        }
    }

    public Coordinator(Coupled model, boolean flatten) {
        this(new SimulationClock(), model, flatten);
    }

    public Coordinator(Coupled model) {
        this(model, true);
    }

    @Override
    public void initialize() {
        logger.fine("START SIMULATION");
        for (SimulatorInterface simulator : simulators) {
            simulator.initialize();
        }
        tL = clock.getTime();
        tN = tL + ta();
    }

    @Override
    public Collection<SimulatorInterface> getSimulators() {
        return simulators;
    }

    @Override
    public final double ta() {
        double tn = Constants.INFINITY;
        for (SimulatorInterface simulator : simulators) {
            if (simulator.getTN() < tn) {
                tn = simulator.getTN();
            }
        }
        return tn - clock.getTime();
    }

    @Override
    public void lambda() {
        for (SimulatorInterface simulator : simulators) {
            simulator.lambda();
        }
        propagateOutput();
    }

    @Override
    public void propagateOutput() {
        LinkedList<Coupling<?>> ic = model.getIC();
        for (Coupling<?> c : ic) {
            c.propagateValues();
        }

        LinkedList<Coupling<?>> eoc = model.getEOC();
        for (Coupling<?> c : eoc) {
            c.propagateValues();
        }
    }

    @Override
    public void deltfcn() {
        propagateInput();
        for (SimulatorInterface simulator : simulators) {
            simulator.deltfcn();
        }
        tL = clock.getTime();
        tN = tL + ta();
    }

    @Override
    public void propagateInput() {
        LinkedList<Coupling<?>> eic = model.getEIC();
        for (Coupling<?> c : eic) {
            c.propagateValues();
        }
    }

    @Override
    public void clear() {
        for (SimulatorInterface simulator : simulators) {
            simulator.clear();
        }
        Collection<InPort<?>> inPorts;
        inPorts = model.getInPorts();
        for (InPort<?> port : inPorts) {
            port.clear();
        }
        Collection<OutPort<?>> outPorts;
        outPorts = model.getOutPorts();
        for (OutPort<?> port : outPorts) {
            port.clear();
        }
    }

    @Override
    public void simInject(double e, InPort port, Collection<Object> values) {
        double time = tL + e;
        if (time <= tN) {
            port.addValues(values);
            clock.setTime(time);
            deltfcn();
        } else {
            logger.severe("Time: " + tL + " - ERROR input rejected: elapsed time " + e + " is not in bounds.");
        }
    }

    @Override
    public void simInject(InPort port, Collection<Object> values) {
        simInject(0.0, port, values);
    }

    @Override
    public void simInject(double e, InPort port, Object value) {
        LinkedList values = new LinkedList();
        values.add(value);
        simInject(e, port, values);
    }

    @Override
    public void simInject(InPort port, Object value) {
        simInject(0.0, port, value);
    }

    @Override
    public void simulate(long numIterations) {
        clock.setTime(tN);
        long counter;
        for (counter = 1; counter < numIterations
                && clock.getTime() < Constants.INFINITY; counter++) {
            lambda();
            deltfcn();
            clear();
            clock.setTime(tN);
        }
    }

    @Override
    public void simulate(double timeInterval) {
        clock.setTime(tN);
        double tF = clock.getTime() + timeInterval;
        while (clock.getTime() < Constants.INFINITY && clock.getTime() < tF) {
            lambda();
            deltfcn();
            clear();
            clock.setTime(tN);
        }
    }

    @Override
    public CoupledInterface getModel() {
        return model;
    }

    public static void main(String[] args) {
        MitrisLogger.setup(Level.FINE);
        Efp efp = new Efp("EFP", 1, 3, 30);
        Coordinator coordinator = new Coordinator(efp);
        coordinator.initialize();
        coordinator.simulate(600.0);
    }

}
