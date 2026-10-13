/*
 * For shifting values of factors exogenously over the course of the model,
 * trying it for changing space cost change.
 */
package location2012.control;

import java.util.ArrayList;
import location2012.Actor;
import location2012.Firm;
import location2012.FirmActDummy;
import location2012.Person;
import location2012.econs.CESOneTypeUtility;
import location2012.econs.UtilityShell;
import location2012.geog.SpaceTools;
import location2012.graphics.CorrelateSingleVarsGraph;
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
public class ShouterParamSweeper implements Listener, Audible {

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
    //
    public ArrayList<Shouter> shouters = new ArrayList<Shouter>();

    public ShouterParamSweeper(Audible aud, int val, double stepSize, ArrayList<Actor> actors) {

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


//        gl.EoS_Curve += stepSize;
        gl.markUp += stepSize;

        f = (Firm) gl.firms.get(0);

        f.markUp = gl.markUp;

//        gl.EoS_Curve = (gl.EoS_Curve > 1 ? 1 : gl.EoS_Curve);        
//        System.out.println("EoS_C: " + gl.EoS_Curve + ", day: " + gl.day);
        System.out.println("Markup: " + gl.markUp + ", day: " + gl.day);

        //Let listeners know also. HACKEROO! (Well, just not using methods...)
        checkValueChange();

        //don't go below zero
//        if (gl.rho + (stepSize * polarity) < 0.999) {
////        if (gl.rho + (stepSize * polarity) > 0.00001) {
//            if (!printed) {
//                //hacking for a particular graph
////            if(gl.rho < 0.59) {            
////            gl.rho += (stepSize * polarity);
////            } else {
////            gl.rho += (stepSize * polarity * 0.1);                
////            }
//                gl.rho += (stepSize * polarity);
//                System.out.println("rho changed to: " + gl.rho);
//            }
//        }
//
//
//        //single direction, up
//        if (gl.EoS_Curve > 0.5 && !printed) {
//        if (gl.EoS_Curve > 0.5) {
//            
//            System.out.println("ping!");
//            
//            printed = true;
//            CorrelateSingleVarsGraph.printScreen = true;
//            
//        }
////
////        //assuming CES...
//        util = (CESOneTypeUtility) UtilityShell.u;
//        util.rho = gl.rho;



    }

    @Override
    public void checkValueChange() {

//        System.out.println("SHOUTING!");

        for (Shouter s : shouters) {

            shout(s);

        }

    }

    @Override
    public void shout(Shouter s) {
        s.lstr.heard(new ShoutEvent(0));
    }

    @Override
    public void sortListenersByWeight() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void registerShouter(Shouter n) {
        shouters.add(n);
    }

    public void setWeight(int weight) {
    }
}
