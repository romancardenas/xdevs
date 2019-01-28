from xdevs import INFINITY
from xdevs.models import Atomic, Coupled, Port
from xdevs.sim import Coordinator
import logging


PHASE_WAIT = 'wait'
PHASE_SEND = 'send'


class Ball:
    def __init__(self):
        self.count = 0


class InitialBall(Atomic):

    def __init__(self, name):
        super().__init__(name)
        self.o_output = Port(Ball, 'o_output')

        self.add_out_port(self.o_output)

    def initialize(self):
        self.hold_in(PHASE_SEND, 0)

    def deltint(self):
        self.hold_in(PHASE_WAIT, INFINITY)

    def deltext(self, e):
        pass

    def lambdaf(self):
        ball = Ball()
        self.o_output.add_value(ball)

    def exit(self):
        pass


class Player(Atomic):

    def __init__(self, name, proc_time):
        super().__init__(name)

        self.i_input = Port(Ball, 'i_input')
        self.o_output = Port(Ball, 'o_output')

        self.add_in_port(self.i_input)
        self.add_out_port(self.o_output)

        self.current_job = None
        self.proc_time = proc_time
        self.clock = 0

    def initialize(self):
        self.hold_in(PHASE_WAIT, INFINITY)

    def deltint(self):
        self.clock += self.sigma
        if self.phase == PHASE_SEND:
            logging.info("%s launched ball number %d @ t = %.1f" % (self.name, self.current_job.count, self.clock))
            self.current_job = None
            self.hold_in(PHASE_WAIT, INFINITY)

    def deltext(self, e):
        self.clock += e
        if self.phase == PHASE_WAIT:
            if self.i_input:
                job = self.i_input.get_single_value()
                logging.info("%s received ball number %d @ t = %.1f" % (self.name, job.count, self.clock))
                self.current_job = job
                self.hold_in(PHASE_SEND, self.proc_time)

    def lambdaf(self):
        if self.phase == PHASE_SEND:
            self.current_job.count += 1
            self.o_output.add_value(self.current_job)

    def exit(self):
        pass


class PingPong(Coupled):
    def __init__(self, name, proc_time):
        super().__init__(name)

        player1 = Player("player_1", proc_time)
        player2 = Player("player_2", proc_time)
        initial_ball = InitialBall("initial_ball")

        self.add_component(player1)
        self.add_component(player2)
        self.add_component(initial_ball)

        self.add_coupling(player1.o_output, player2.i_input)
        self.add_coupling(player2.o_output, player1.i_input)
        self.add_coupling(initial_ball.o_output, player1.i_input)


if __name__ == '__main__':
    pingpong = PingPong('pingpong', 0.1)
    coord = Coordinator(pingpong)
    coord.initialize()
    coord.simulate(num_iters=20)
