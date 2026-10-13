/*
 * For shifting values of factors exogenously over the course of the model,
 * trying it for changing space cost change.
 * This one does it via the space Processing window, using the mouse.
 */
package location2012.econs;

import java.util.ArrayList;
import location2012.Actor;
import location2012.Firm;
import location2012.observe.Timeline;
import location2012.observe.Audible;
import location2012.observe.Listener;
import location2012.observe.ShoutEvent;
import location2012.observe.Shouter;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class AllFirmMouseVarChanger1 implements Listener {

    //for referencing what day it is
    Timeline time;
    ArrayList<Actor> actors;
    Firm f;

    public AllFirmMouseVarChanger1(Audible time, double val, ArrayList<Actor> actors) {

        this.time = (Timeline) time;
        this.actors = actors;

        giveShouterTo(time, val);

        gl.changeVal = 0.67;

    }

    public int getWeight() {

        return 0;

    }

    public void giveShouterTo(Audible a, double val) {

        a.registerShouter(new Shouter(this, val));

    }

    public void heard(ShoutEvent s) {

        //System.out.println("shout!" + s.heardValue);

        for (Actor a : actors) {

            f = (Firm) a;

            if (gl.wageMouse) {
                f.wage = gl.changeVal;
            } else {
                f.deliverycost = gl.changeVal;
            }


        }

        if (gl.wageMouse) {
            System.out.println("Firms' wage: " + f.wage);
        } else {
            System.out.println("Firms' delivery cost: " + gl.changeVal);
        }

        //change single firm value
//        f= (Firm) actors.get(0);
//
//        f.goodCost = changeVal;
//
//        if (s.heardValue > 10) {
//            changeVal += 0.005;
//        }


//        System.out.println("Firm " + f.goodType + " good cost: " + changeVal);

    }

    public void setWeight(int weight) {
    }
}
