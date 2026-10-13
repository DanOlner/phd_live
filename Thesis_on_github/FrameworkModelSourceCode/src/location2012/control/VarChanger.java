package location2012.control;

import location2012.observe.Audible;
import location2012.observe.Listener;
import location2012.observe.ShoutEvent;

/**
 * Varchanger: top-level class used to change values as the model runs,
 * either parameter sweeps or mouse manipulations.
 * 
 * @author Dan
 */
public abstract class VarChanger implements Listener {

    //two-slot value held by VarChanger. Both slots used by some things e.g. taking in mouseX and mouseY
    //Is normalised: 0 < val < 1. This is so outside elements like mouse control
    //can deal with all VarChangers consistently. Up to the varchanger to use the value
    //as it wants to.
    public double[] inVals = new double[2];
    //If a displayVal is shown...
    public double displayVal = - 999;
    
    public VarChanger(Audible a, double val) {


    }

    public int getWeight() {
        return 0;
    }

    public void giveShouterTo(Audible a, double val) {
        //leaving blank VarChangers are shouted indirectly by ListenerWrappers
    }

    public void heard(ShoutEvent s) {
        
    }

    public void setWeight(int weight) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Set VarChange value, between zero and 1. VarChanger will decide what to do with that.
     * @param val
     */
    public void setValue(double[] val) {

        this.inVals = val;

    }//end method setValue

   
}
