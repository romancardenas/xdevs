import _thread
import logging
import pickle
from abc import ABC, abstractmethod
from xmlrpc.server import SimpleXMLRPCServer

from xdevs import INFINITY
from xdevs.models import Atomic, Coupled


class SimulationClock:
    def __init__(self, time=0):
        self.time = time


class AbstractSimulator(ABC):
    def __init__(self, clock):
        self.clock = clock
        self.time_last = 0
        self.time_next = 0

    @abstractmethod
    def initialize(self):
        pass

    @abstractmethod
    def exit(self):
        pass

    @abstractmethod
    def ta(self):
        pass

    @abstractmethod
    def lambdaf(self):
        pass

    @abstractmethod
    def deltfcn(self):
        pass

    @abstractmethod
    def clear(self):
        pass


class Simulator(AbstractSimulator):
    def __init__(self, model, clock):
        super().__init__(clock)
        self.model = model

    @property
    def ta(self):
        return self.model.ta

    def initialize(self):
        self.model.initialize()
        self.time_last = self.clock.time
        self.time_next = self.time_last + self.model.ta

    def exit(self):
        self.model.exit()

    def deltfcn(self):
        t = self.clock.time
        in_empty = self.model.is_input_empty()

        if in_empty:
            if t != self.time_next:
                return
            self.model.deltint()
        else:
            e = t - self.time_last
            self.model.sigma -= e

            if t == self.time_next:
                self.model.deltcon(e)
            else:
                self.model.deltext(e)

        self.time_last = t
        self.time_next = self.time_last + self.model.ta

    def lambdaf(self):
        if self.clock.time == self.time_next:
            self.model.lambdaf()

    def clear(self):
        for in_port in self.model.in_ports:
            in_port.clear()

        for out_port in self.model.out_ports:
            out_port.clear()


class Coordinator(AbstractSimulator):
    def __init__(self, model, clock=None, flatten=False):
        super().__init__(clock or SimulationClock())
        self.model = model.flatten() if flatten else model
        self.simulators = []
        self.ports_to_serve = {}

    def initialize(self):
        self._build_hierarchy()

        for sim in self.simulators:
            sim.initialize()

        self.time_last = self.clock.time
        self.time_next = self.time_last + self.ta()

    def _build_hierarchy(self):
        for comp in self.model.components:
            # logging.info("%s -> %s" % (self, comp))
            if isinstance(comp, Coupled):
                coord = Coordinator(comp, self.clock)
                self.simulators.append(coord)
                self.ports_to_serve.update(coord.ports_to_serve)
            elif isinstance(comp, Atomic):
                sim = Simulator(comp, self.clock)
                self.simulators.append(sim)
                for pts in sim.model.in_ports:
                    if pts.serve:
                        port_name = "%s.%s" % (pts.parent.name, pts.name)
                        self.ports_to_serve[port_name] = pts

    def serve(self, host="localhost", port=8000):
        server = SimpleXMLRPCServer((host, port))
        server.register_function(self.inject)
        _thread.start_new_thread(server.serve_forever, ())

    def test(self, msg):
        return msg + "aa"

    def exit(self):
        for sim in self.simulators:
            sim.exit()

    def ta(self):
        return min([sim.time_next for sim in self.simulators], default=0) - self.clock.time

    def lambdaf(self):
        for sim in self.simulators:
            sim.lambdaf()

        self.propagate_output()

    def propagate_output(self):
        for coup in self.model.ic:
            coup.propagate_values()

        for coup in self.model.eoc:
            coup.propagate_values()

    def deltfcn(self):
        self.propagate_input()

        for sim in self.simulators:
            sim.deltfcn()

        self.time_last = self.clock.time
        self.time_next = self.time_last + self.ta()

    def propagate_input(self):
        for coup in self.model.eic:
            coup.propagate_values()

    def clear(self):
        for sim in self.simulators:
            sim.clear()

        for in_port in self.model.in_ports:
            in_port.clear()

        for out_port in self.model.out_ports:
            out_port.clear()

    def inject(self, port, values, e=0):
        logging.debug("INJECTING")
        time = self.time_last + e

        if type(values) is not list:
            values = [values]

        if type(port) is str:
            values = list(map(lambda x: pickle.loads(x.encode()), values))
            if port in self.ports_to_serve:
                port = self.ports_to_serve[port]
            else:
                logging.error("Port '%s' not found" % port)
                return True

        if time <= self.time_next or time != time:
            port.add_values(values)
            self.clock.time = time
            self.deltfcn()
            return True
        else:
            logging.error("Time %d - Input rejected: elapsed time %d is not in bounds" % (self.time_last, e))
            return False

    def simulate(self, num_iters=10000):
        logging.info("STARTING SIMULATION...")
        self.clock.time = self.time_next

        cont = 0
        while cont < num_iters and self.clock.time < INFINITY:
            self.lambdaf()
            self.deltfcn()
            self.clear()
            self.clock.time = self.time_next
            cont += 1

    def simulate_time(self, time_interv=10000):
        logging.info("START SIMULATION")
        self.clock.time = self.time_next
        tf = self.clock.time + time_interv

        while self.clock.time < tf:
            self.lambdaf()
            self.deltfcn()
            self.clear()
            self.clock.time = self.time_next

    def simulate_inf(self):

        while True:
            self.lambdaf()
            self.deltfcn()
            self.clear()
            self.clock.time = self.time_next
