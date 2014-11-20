/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mitris.sim.core.lib.examples;

import mitris.sim.core.modeling.DevsAtomic;
import mitris.sim.core.modeling.Port;

/**
 *
 * @author jlrisco
 */
public class Processor extends DevsAtomic {

  protected Port<Job> iIn = new Port<>("iIn");
  protected Port<Job> oOut = new Port<>("oOut");
  protected Job currentJob = null;
  protected double processingTime;

  public Processor(String name, double processingTime) {
	  super(name);
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
