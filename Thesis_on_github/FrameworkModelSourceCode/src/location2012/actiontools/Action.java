package location2012.actiontools;

import location2012.observe.Audible;
import location2012.observe.Listener;
import location2012.observe.Shouter;

/**
 * Action: listener's 'heard' method can carry out the action itself
 *
 * @author Olner Dan
 */
public abstract class Action implements Listener {

    //one listener registers with them
    int weight;

    
    //implemented method, but this makes it tidier
    /**
     * 
     *
     * @param a
     * @param val
     */
    public void giveShouterTo(Audible a, double val) {

        a.registerShouter(new Shouter(this, val));

    }

    public int getWeight() {

        return weight;

    }

    public void setWeight(int weight) {

        this.weight = weight;

    }
}
