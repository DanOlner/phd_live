/*
 * For shifting values of factors exogenously over the course of the model,
 * trying it for changing space cost change.
 */
package location2012.econs;

import location2012.observe.Timeline;
import location2012.observe.Audible;
import location2012.observe.Listener;
import location2012.observe.ShoutEvent;
import location2012.observe.Shouter;
import location2012.utils.gl;
import location2012.utils.p;

/**
 *
 * @author Olner Dan
 */
public class GlobalDensityCostChanger implements Listener {

    //for referencing what day it is
    Timeline time;

    public GlobalDensityCostChanger(Audible time, double val) {

        this.time = (Timeline) time;

        giveShouterTo(time, val);

    }

    public int getWeight() {

        return 0;

    }

    public void giveShouterTo(Audible a, double val) {

        a.registerShouter(new Shouter(this, val));

    }

    public void heard(ShoutEvent s) {

        //Change global space cost.
        //What shall we try? Hmm
        //100 days.
        //15 days at 0.5
        //Then drop cost from 0.5 to 1 over a period of 35 days
        //Then 15 days at 1
        //Then raise again to 1 to 0.5, but in 15 days
        //Confused? That's "drop" cos it means "distance traverseable in one day"

        //Hmm - this would be a very useful thing for a more flexible shouter!

        if (time.day > 0 && time.day < 70) {

            if (gl.DENSITYCOSTRADIUS > 0.01) {
                gl.DENSITYCOSTRADIUS -= 0.005;
                gl.DENSITYCOST += 0.1;
            }

        } else if (time.day > 70) {

            gl.DENSITYCOSTRADIUS += 0.001;

        }//end ifs

        p.p("Density cost: " + gl.DENSITYCOST);

    }

    public void setWeight(int weight) {
    }
}
