/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mitris.sim.core.lib.examples;

import mitris.sim.core.modeling.Atomic;
import mitris.sim.core.modeling.Port;

/**
 *
 * @author jlrisco
 */
public class Processor extends Atomic {

  protected Port<Job> iIn = new Port<>();
  protected Port<Job> oOut = new Port<>();
  protected Job currentJob = null;
  protected double processingTime;

  public Processor(double processingTime) {
    super.addInPort(iIn);
    super.addOutPort(oOut);
    this.processingTime = processingTime;
  }

  @Override
  public void deltint() {
    super.passivate();
  }

  @Override
  public void deltext(double e) {
    if (super.phaseIs("passive")) {
      Job job = iIn.getSingleValue();
      currentJob = job;
      super.holdIn("active", processingTime);
    }
  }

  @Override
  public void lambda() {
    oOut.addValue(currentJob);
  }
}
