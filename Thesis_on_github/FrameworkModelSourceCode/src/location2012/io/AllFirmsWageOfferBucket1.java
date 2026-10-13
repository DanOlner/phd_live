/*
 * Bucket for playing about with networks connections - who's collaborating with who?
 */
package location2012.io;

import java.util.ArrayList;
import location2012.Firm;

/**
 *
 * @author Olner Dan
 */
public class AllFirmsWageOfferBucket1 extends Bucket {

    ArrayList<Firm> firms;

    public AllFirmsWageOfferBucket1(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        //override to add a column in for mean
        //vals = new double[samplelength][entities.size()+1];

        firms = entities;

    }

    @Override
    public void grabData() {

        //Now... what do I want to do here? Hmm. Good question.
        //Let's start with: how many people are contributing to me?
        //System.out.println("firms size: " + firms.size());


        for (int i = 0; i < firms.size(); i++) {

            //will return zero if firm cannot afford wage...
//            if (firms.get(i).hiredLabour) {
                vals[samplePoint][i] = firms.get(i).wage;

                //this is here cos all people's bucket is earlier... there's a note
                //there on this being bad - need a better flag solution.
                firms.get(i).hiredLabour = false;

//            }

        }

//        //add mean nearest neigbour value in last slot
//        vals[samplePoint][ac.size()] = distanceTot/ac.size();
//
        samplePoint++;

    }
}
