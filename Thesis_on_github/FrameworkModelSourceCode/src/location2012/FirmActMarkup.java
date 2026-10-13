package location2012;

import location2012.actiontools.Action;
import location2012.observe.ShoutEvent;

/**
 * For seeking more profitable markup in the Transmission Belt model
 *
 * @author Dan Olner
 */
public class FirmActMarkup extends Action {

    Firm me;
    //Number of days to try action before assessing success. Assess against mean.
    int numDays = 1, numDaysCount = 0;
    //two states to action. Action mode (starts with)
    //and response mode, testing effectiveness of action
    //Remember that the action itself is triggered in Firm's Transmission action
    //When priceflux drops to a calm enough level
    //All a hacked tangle at the minute, but it'll do.
    boolean responseMode = false;
    //the amount of `revenue' change between action and response 
    //(revenue being in output)
    double revenueDifference;
    //Amount to change markup 

    public FirmActMarkup(Actor me) {

        //ref to the actor acting this action!
        this.me = (Firm) me;


    }

    public void heard(ShoutEvent s) {

        if (responseMode) {

            responseMode();
            responseMode = false;

        } else {

            actMode();
            responseMode = true;

        }

    }//method heard

    private void actMode() {

        //gently alter target stock level in a random direction
//        me.markUp;

    }

    private void responseMode() {
    }
}
