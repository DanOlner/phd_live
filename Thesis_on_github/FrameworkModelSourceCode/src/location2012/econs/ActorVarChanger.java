/*
 * For shifting values of factors exogenously over the course of the model,
 * trying it for changing space cost change.
 */
package location2012.econs;

import java.util.ArrayList;
import location2012.Actor;
import location2012.Firm;
import location2012.observe.Timeline;
import location2012.io.HillClimbOptBucket;
import location2012.observe.Audible;
import location2012.observe.Listener;
import location2012.observe.ShoutEvent;
import location2012.observe.Shouter;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class ActorVarChanger implements Listener {

    //for referencing what day it is
    Timeline time;
    ArrayList<Actor> actors;
    Firm f;
    double changeVal = 0;
    //running through these delivery costs in order. 23 values. Minus first one, which I'll set in Firm
//    double[] costArray = {500, 400, 300, 200, 100, 75, 50, 40, 30, 20, 10, 5, 3, 2, 1, 0.75, 0.5, 0.25, 0.125, 0.1, 0.05, 0.02, 0.01};
    double[] valArray = {50, 250};
    int index = 0;
    //for doing range sweeps
    double min = 0, max = 1000;

    CESOneTypeUtility u;

    public ActorVarChanger(Audible time, double val, ArrayList<Actor> actors) {

        this.time = (Timeline) time;
        this.actors = actors;

        giveShouterTo(time, val);

    }

    public int getWeight() {

        return 0;

    }

    public final void giveShouterTo(Audible a, double val) {

        a.registerShouter(new Shouter(this, val));

    }

    public void heard(ShoutEvent s) {



        if (s.heardValue > 1000) {

            

//            gl.DENSITYCOST = valArray[index];
            //gl.EoS += 0.4;
            u = (CESOneTypeUtility) UtilityShell.u;

            u.rho += 0.0001;
            //just make sure there are values left so I don't crash it right at the end
//            if ((index + 1) < valArray.length) {
//                index++;
//            }

        }


    }//method heard

    //changing first firm's values, mix
    public void xxheard(ShoutEvent s) {

        f = (Firm) actors.get(0);

        //wait for things to settle
//        if (s.heardValue > 250 && s.heardValue % 50 == 0 &&  f.wageoffer > 0) {
//        if (s.heardValue > 250 && s.heardValue % 50 == 0 &&  f.deliverycost > 0) {
        if (s.heardValue > 100) {

//            gl.TECH += 13;

            f.deliverycost += 0.001;
//            f.wageoffer += 0.001;
//            f.goodCost += 0.015;

//            System.out.println("wageOffer: " + f.wageoffer);
//            System.out.println("good cost: " + f.goodCost);
//            System.out.println("tech: " + gl.TECH);

        }

    }

    //changing hill climb ites
    public void notheroldheard(ShoutEvent s) {

        OptimiseHillClimb.iterations = index++ * 2;


        System.out.println("day: " + s.heardValue + ", index: " + index + ", ites: " + OptimiseHillClimb.iterations);

    }

    public void xxxheard(ShoutEvent s) {

//        System.out.println("shout!" + s.heardValue);

        //pick new value every 50 days
//        if (s.heardValue - 1 == 0 || (s.heardValue - 1) % 50 == 0) {
        if (s.heardValue > 100) {

            for (Actor a : actors) {

                f = (Firm) a;

                f.deliverycost += 0.001;
//                f.wageoffer += 0.001;

            }

//            //just make sure there are values left so I don't crash it right at the end
//            if ((index + 1) < valArray.length) {
//                index++;
//            }

        }

//        System.out.println("day: " + s.heardValue + ", index: " + index);

//        System.out.println("Firms' delivery cost: " + f.deliverycost);
        //change single firm value
//        f= (Firm) actors.get(0);
//
//        f.goodCost = changeVal;
//
//        if (s.heardValue > 10) {
//            changeVal += 0.005;
//        }

//        System.out.println("Firm " + f.goodType + " good cost: " + changeVal);
        //System.out.println("Firms' wage: " + f.wageoffer);

    }

    public void setWeight(int weight) {
    }
}
