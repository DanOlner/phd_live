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
public class GlobalSpaceCostChanger implements Listener {

    //for referencing what day it is
    Timeline time;

    public GlobalSpaceCostChanger(Audible time, double val) {

        this.time = (Timeline) time;

        //initial value
        gl.SPACECOST = 0.5;

        //set start day whilst I'm here - i.e. it starts at 1.
        //then spacecost/startday gives us how many startdays an actor can now
        //travel in a day
        gl.STARTSPACECOST = 0.5;

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

        if(time.day > 10 && time.day <70) {

            //just in case... we'll check this though
            if(gl.SPACECOST<1) {

                //35 days to go from 0.5 to 1
                gl.SPACECOST += (0.5/60.0);

            }//end if

        } else if (time.day >95) {
            //let's destroy!
        //} else if (time.day >75) {

            //to stop division by zero
            if(gl.SPACECOST>0.5) {

                gl.SPACECOST -= (0.5/30);

            }

        }//end ifs

        p.p("gl.SPACECOST: " + gl.SPACECOST);

    }

    public void setWeight(int weight) {

    }



}
