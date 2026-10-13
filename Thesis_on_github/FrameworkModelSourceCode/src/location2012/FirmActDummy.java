package location2012;

import location2012.actiontools.Action;
import location2012.observe.ShoutEvent;

/**
 * A dummy firm action: can e.g. stand in for "other" demand for testing monopoly
 *
 * @author Dan Olner
 */
public class FirmActDummy extends Action {

    Firm me;

    public FirmActDummy(Actor me) {

        //ref to the actor acting this action!
        this.me = (Firm) me;


    }

    public void heard(ShoutEvent s) {
        
        
    }
}
