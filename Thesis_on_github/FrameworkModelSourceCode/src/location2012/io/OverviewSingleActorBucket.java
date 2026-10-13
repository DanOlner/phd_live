/*
 * Bucket for storing distance measurements:
 * Columns are, in order:
 *
 * move distance
 * move cost
 *
 * commute distance
 * commute cost
 *
 * total distance
 * total cost
 * 
 * work time
 * space cost
 *
 * Just added worktime. All time is here, then, and should sum to 1 x number of actors
 */
package location2012.io;

import java.lang.String;
import java.util.ArrayList;
import location2012.Actor;
import location2012.SingleActorAction;
import location2012.SingleActorPerson;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class OverviewSingleActorBucket extends Bucket {

    ArrayList<Actor> actors;
    Actor a;
    SingleActorPerson sap;
    SingleActorAction saa;
    double umean, uvar, usd, pmean, pvar, psd;
    double utot, ptot;

    public OverviewSingleActorBucket(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,


        actors = entities;


        //for easy change of order and adding
        ArrayList<String> colNameArrayList = new ArrayList<String>();

        //p.s. this was completely unnecessary - note how it's done below when they're added to the output array
        //Hmm - actually, it is useful: don't have to set array length in hardcode.
        colNameArrayList.add("EoS");
        colNameArrayList.add("UtilityMean");
        colNameArrayList.add("UtilitySD");
        colNameArrayList.add("PriceMean");
        colNameArrayList.add("PriceSD");
//          colNameArrayList.add("av_good1price");
//        colNameArrayList.add("av_good2price");
//        colNameArrayList.add("av_wageOffer");
//        colNameArrayList.add("av_contributedTime");
//        colNameArrayList.add("optBYgood1");
//        colNameArrayList.add("optBYgood2");

        //Overwrite: setting to 3 vars for space
        vals = new double[samplelength][colNameArrayList.size()];
        colnames = new String[vals[1].length];

        //set up column names - passed by superclass
        for (int i = 0; i < colNameArrayList.size(); i++) {
            colnames[i] = colNameArrayList.get(i);
        }

    }

    @Override
    public void grabData() {


        //reset vars
        utot = 0;
        ptot = 0;
        uvar = 0;
        pvar = 0;

        vals[samplePoint][0] = gl.EoS_Curve;

        for (int i = 0; i < actors.size(); i++) {

//            a = actors.get(i);
            sap = (SingleActorPerson) actors.get(i);
            saa = (SingleActorAction) actors.get(i).actions.get(0);

            utot += saa.bestBundle.maxUtility;
            ptot += sap.goodCost;

        }

        //add mean nearest neigbour value in last slot
        umean = utot / actors.size();
        pmean = ptot / actors.size();

        vals[samplePoint][1] = umean;
        vals[samplePoint][3] = pmean;


        //now we have means, work out variance
        for (int i = 0; i < actors.size(); i++) {

            sap = (SingleActorPerson) actors.get(i);
            saa = (SingleActorAction) actors.get(i).actions.get(0);

            uvar += Math.pow((umean - saa.bestBundle.maxUtility), 2);
            pvar += Math.pow((pmean - sap.goodCost), 2);

        }


        //find final variance by averaging total squared differences
        uvar /= actors.size();
        pvar /= actors.size();


        //find SD
        usd = Math.sqrt(uvar);
        psd = Math.sqrt(pvar);

        //add to data
        vals[samplePoint][2] = usd;
        vals[samplePoint][4] = psd;

        samplePoint++;



    }
}
