/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.variables;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import location2012.Actor;
import location2012.Firm;
import location2012.control.VarChanger;
import location2012.econs.CESOneTypeUtility;
import location2012.econs.UtilityShell;
import location2012.graphics.CorrelateArraysGraph;
import location2012.graphics.CorrelateSingleVarsGraph;
import location2012.graphics.ProcessingSpaceDrawer;
import location2012.observe.Audible;
import location2012.observe.ShoutEvent;
import location2012.observe.Shouter;
import location2012.utils.gl;

/**
 * FirmPriceRatioTrigger uses the Firm price ratio to trigger data collection
 * for detecting the transition between production regimes
 *
 * Doing my parameter sweeping here too - need to crawl CES and del costs up in
 * turn And pause while things settle down again
 *
 * @author geodo
 */
public class VariableStoreTwoFirmRatioTrigger extends VariableStore implements Audible {

    public ArrayList<Shouter> shouters = new ArrayList<Shouter>();
    //number of times firm zero's good cost has increased
    int countPositiveGoodCostChange;
    //To avoid picking up on oscillations, taking a ratio average over how many days?
    int daysToAverageOver = 30;
    int daysToTestAgainAfterSwitch = 300, count = -1;
    double mean;
    //actual record of values, ArrayList so we can drop the first value
//    ArrayList<Double> values = new ArrayList<Double>();
    Field chkFld;
    double ratioFirm0overFirm1;
    //Using ratio to trigger, what size deadband?
    //Size is each side of 1 (perfect good ratio)
    //So the full deadband width is twice this value
    double deadBand = 0.1;
    //Presume the model starts in symmetric
    //change rho or delivery cost
    public static boolean symmetric = true, changeRho = true;
    //sweep amount
    double valChange = 0.001;
//    double valChange = 0.0005;
    CESOneTypeUtility util;
    Firm f;

    public VariableStoreTwoFirmRatioTrigger(ArrayList arrayList, String varName) {

        super(arrayList, varName);

    }

    //If label for variable is different from varName - for instance
    //If getting x,y coords for people and firms separately, need way to find them
    public VariableStoreTwoFirmRatioTrigger(ArrayList arrayList, String varName, String varNameLabel) {

        super(arrayList, varName, varNameLabel);

    }

    /**
     *
     */
    @Override
    public void acquireVariable() {

        //add new ArrayList of Doubles to contain "today's" data from 'objects'
        //Only for one actor, so just the single index
        variableData.add(new Double[1]);
//        variableData.add(new Double[objects.size()]);

        //... and this is the one I'm hacking to get a single Firm's markUp
        try {

            //firm 0
            Object o = (Object) objects.get(0);
            cls = o.getClass();

            fld = cls.getField(varName);
            ratioFirm0overFirm1 = fld.getDouble(o);

            //firm 1
            o = (Object) objects.get(1);
            cls = o.getClass();

            fld = cls.getField(varName);
            //find actual ratio
            ratioFirm0overFirm1 /= fld.getDouble(o);

            //add ratio to variableStore
            variableData.get(variableData.size() - 1)[0] = ratioFirm0overFirm1;

//            System.out.println("ratio = " + ratioFirm0overFirm1);

        } catch (Throwable e) {
            System.err.println("VariableStore " + varName + " fail: " + e.toString());
        }//end try catch


        if (gl.day > daysToAverageOver) {

            mean = 0;

            //find average from last stored slots, having made sure they exist
            for (int i = 0; i < daysToAverageOver; i++) {
                mean += variableData.get(variableData.size() - 1 - i)[0];
            }

            mean /= daysToAverageOver;

        }//if gl.day


        //use mean to test. Shout if outside deadband
        //wait for things to settle at start of model
        if (gl.day > 300) {

            count--;

//            System.out.println("count: " + count);

            if (count < 0 && symmetric && (mean > 1 + deadBand || mean < 1 - deadBand)) {

                count = daysToTestAgainAfterSwitch;
                symmetric = false;
                changeRho = (changeRho ? false : true);

                System.out.println("symmetric = FALSE. Shouting data store.");
//                System.out.println("shouting on day: " + gl.day);
                checkValueChange();

                //test for when it gets back into deadband again
                //Hmm, might need to call it the neutral zone!
            } else if (count < 0 && !symmetric && (mean < 1 + (deadBand / 2) && mean > 1 - (deadBand / 2))) {
//            } else if (count < 0 && !symmetric && (mean < 1 + (deadBand) && mean > 1 - (deadBand))) {

                changeRho = (changeRho ? false : true);
                count = daysToTestAgainAfterSwitch;
                symmetric = true;
                System.out.println("symmetric = TRUE. Shouting data store.");
                checkValueChange();
            }

//              do variable transitions
            if (false) {
//            if (count < 0 && gl.day % 100 == 0) {

                if (changeRho) {

                    if (gl.rho < 1) {
                        gl.rho += valChange;
                    } else {
//                        CorrelateSingleVarsGraph.printScreen = true;
//                        checkValueChange();
                    }

                    util = (CESOneTypeUtility) UtilityShell.u;
                    util.rho = gl.rho;
                    System.out.println("rho: " + gl.rho);

                } else {

                    gl.deliveryCost += valChange;
                    for (Actor a : gl.firms) {
//
                        f = (Firm) a;
                        f.deliverycost = gl.deliveryCost;

                    }//for Actor a

                    System.out.println("del: " + gl.deliveryCost);

                }//if changerho

                //update window, based on first varchanger being rho, second del
                ProcessingSpaceDrawer.left.displayVal = gl.rho;
                ProcessingSpaceDrawer.right.displayVal = gl.deliveryCost;

            }//if gl.day

//      
        }//if count < 0


    }//end method acquireVariable

    /**
     * Return most recent ArrayList addition to the VariableStore
     *
     * @return
     */
    public Double[] getLatestArrayListOfData() {

//        System.out.println("variableData: " + varName + " " + variableData.size());

//        if (variableData.size() > 0) {
        return variableData.get(variableData.size() - 1);
//        } else {
//            return new ArrayList<Double>();
//        }

    }//end method getLatestArrayListOfData

    @Override
    public void registerShouter(Shouter n) {
        shouters.add(n);
    }

    @Override
    public void sortListenersByWeight() {
        Collections.sort(shouters);
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
        //from symmetric
        if (!symmetric) {
            s.lstr.heard(new ShoutEvent(0));
        } else {
            //to symmetric
            s.lstr.heard(new ShoutEvent(1));
        }
    }

    public void checkShouterOrder() {

        //check order of Listeners
        for (Shouter sh : shouters) {

            System.out.println("ListWeight: " + sh.lstr.getWeight() + ", name: " + sh.lstr.getClass().getName());

        }

    }
}

/*
 * Cuttinz
 */
//            double var = 0;
//
//            for (int i = 0; i < daysToAverageOver; i++) {
//                var += Math.pow((mean - variableData.get(variableData.size() - 1 - i)[0]), 2);
//            }
//            
//            var /= daysToAverageOver;
//            
//            sd = Math.sqrt(var);