/*
 * For outputting misc values from a single firm, the first in the firm array. Using at the moment
 * to get values for spatial eq tests
 */
package location2012.io;

import java.util.ArrayList;
import location2012.Firm;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class FirstFirmMiscValueBucket extends Bucket {

    ArrayList<Firm> firms;

    public FirstFirmMiscValueBucket(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        //override to add a column in for mean
        vals = new double[samplelength][1];

        firms = entities;

    }

    @Override
    public void grabData() {

        //Now... what do I want to do here? Hmm. Good question.
        //Let's start with: how many people are contributing to me?
        //System.out.println("firms size: " + firms.size());


        

            //will return zero if firm cannot afford wage...
//            if (firms.get(i).hiredLabour) {
                vals[samplePoint][0] = gl.DENSITYCOST;
//                vals[samplePoint][0] = firms.get(0).goodCost;
//                vals[samplePoint][0] = firms.get(0).deliverycost;
//                vals[samplePoint][0] = firms.get(0).wageoffer;

                //this is here cos all people's bucket is earlier... there's a note
                //there on this being bad - need a better flag solution.
                firms.get(0).hiredLabour = false;

//            }

        

//        //add mean nearest neigbour value in last slot
//        vals[samplePoint][ac.size()] = distanceTot/ac.size();
//
        samplePoint++;

    }
}
