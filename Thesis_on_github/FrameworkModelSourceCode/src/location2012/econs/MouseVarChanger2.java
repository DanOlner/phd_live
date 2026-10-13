/*
 * For shifting values of factors exogenously over the course of the model,
 * This one temporarily for CES rho value
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
public class MouseVarChanger2 implements Listener {

    //for referencing what day it is
    Timeline time;
    ArrayList<Actor> actors;
    CESOneTypeUtility ces;

    public MouseVarChanger2(Audible time, double val, CESOneTypeUtility ces) {

        this.time = (Timeline) time;
        this.ces = ces;

        giveShouterTo(time, val);

//        gl.changeVal = 0.01;

    }

    public int getWeight() {

        return 0;

    }

    public void giveShouterTo(Audible a, double val) {

        a.registerShouter(new Shouter(this, val));

    }

    public void heard(ShoutEvent s) {

      ces.rho = gl.changeVal1;

      System.out.println("rho changed to: " + gl.changeVal1);
//        System.out.println("Firm " + f.goodType + " good cost: " + changeVal);
        //System.out.println("Firms' wage: " + f.wageoffer);

    }

    public void setWeight(int weight) {
    }
}
