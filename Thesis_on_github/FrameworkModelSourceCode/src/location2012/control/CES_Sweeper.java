/*
 * For shifting values of factors exogenously over the course of the model,
 * trying it for changing space cost change.
 */
package location2012.control;

import java.util.ArrayList;
import location2012.Actor;
import location2012.Firm;
import location2012.Person;
import location2012.econs.CESOneTypeUtility;
import location2012.econs.UtilityShell;
import location2012.graphics.CorrelateSingleVarsGraph;
import location2012.observe.Timeline;
import location2012.observe.Audible;
import location2012.observe.Listener;
import location2012.observe.ShoutEvent;
import location2012.observe.Shouter;
import location2012.utils.gl;

/**
 * For sweeping Love of Variety, resetting everything appropriately
 * An end-of-PhD hack class, not much good for re-using...
 * 
 * @author Olner Dan
 */
public class CES_Sweeper implements Listener {

    //for referencing what day it is
    Timeline time;
    ArrayList<Actor> actors;
    Firm f;
    Person p;
    double stepSize;
    int index = 0;
    int weight = 3;
    int polarity = 1;
    //use to print single outcome
    boolean printed = false;
    //
    CESOneTypeUtility util;

    public CES_Sweeper(Audible aud, int val, double stepSize, ArrayList<Actor> actors) {

//        this.time = (Timeline) aud;
        this.actors = actors;

        giveShouterTo(aud, val);

        this.stepSize = stepSize;

        //set start value for actors
//        for (Actor a : actors) {
//            f = (Firm) a;
//            f.deliverycost = minVal;
//        }

    }

    public int getWeight() {

        return weight;

    }

    public final void giveShouterTo(Audible a, double val) {

        a.registerShouter(new Shouter(this, val));

    }

    public void heard(ShoutEvent s) {

        //don't go below zero
//        if (gl.rho + (stepSize * polarity) < 0.999) {
//        if (gl.rho + (stepSize * polarity) > 0.00001) {
        if(!printed) {
            gl.rho += (stepSize * polarity);
        }
//        } 
//        else {
//            gl.rho = 0.009;
//        }


        if (gl.rho < 0.001) {
            gl.rho = 0.0015;
            polarity = -polarity;
        } else if (gl.rho > 0.99 && !printed) {
            System.out.println("ping!");
            gl.rho = 0.99;
            printed = true;
            CorrelateSingleVarsGraph.printScreen = true;
        }

        //assuming CES...
        util = (CESOneTypeUtility) UtilityShell.u;
        util.rho = gl.rho;

        System.out.println("rho = " + util.rho);


//        for (Actor a : actors) {
//
//            f = (Firm) a;
//
//            //don't go below zero
//            if (f.deliverycost + (stepSize * polarity) > 0.00001) {
//                f.deliverycost += (stepSize * polarity);
//                //for display
//                gl.deliveryCost += (stepSize * polarity);
//            }
//
//        }
//
//
//
//        if (gl.deliveryCost > 1) {
//            polarity = -polarity;
//        } else if (gl.deliveryCost < 0.01 && !printed) {
//            printed = true;
//            CorrelateSingleVarsGraph.printScreen = true;
//        }


//        for (Actor a : actors) {
//
//            f = (Firm) a;
//
//            //don't go below zero
//            if (f.goodCost + (stepSize * polarity) > 0.00001) {
//                f.goodCost += (stepSize * polarity);
//                //for display
//
//            }
//
//
//
//        }//end foreach
//
//        //test one for turning point
//        f = (Firm) actors.get(0);
//
//        if (f.goodCost > 1) {
//            polarity = -polarity;
//        } else if (f.goodCost < 0.01 && !printed) {
//            printed = true;
//            CorrelateSingleVarsGraph.printScreen = true;
//        }

//        for (Actor a : actors) {
//
//            f = (Firm) a;
//
//            //don't go below zero
//            if (f.wageOffer + (stepSize * polarity) > 0.00001) {
//                f.wageOffer += (stepSize * polarity);
//                //for display
//                
//            }
//            
//            
//
//        }//end foreach
//
//        //test one for turning point
//        f = (Firm) actors.get(0);
//
//        if (f.wageOffer > 1) {
//            polarity = -polarity;
//            //test for printing graphic
//        } else if(f.wageOffer < 0.1 && !printed) {
//            printed = true;
//            CorrelateSingleVarsGraph.printScreen = true;
//        }



//        don't go below zero
//        if (gl.COMMUTECOST + (stepSize * polarity) > 0.00001) {
//            
//            gl.COMMUTECOST += (stepSize * polarity);
//            
//        }
//
//        if (gl.COMMUTECOST > 1  ) {
//            polarity = -polarity;
//        } else if(gl.COMMUTECOST < 0.01 && !printed) {
//            printed = true;
//            CorrelateSingleVarsGraph.printScreen = true;
//        }

    }

    public void setWeight(int weight) {
    }
}
