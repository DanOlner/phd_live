/*
 * Bucket for playing about with networks connections - who's collaborating with who?
 */
package location2012.io;

import java.util.ArrayList;
import location2012.Firm;
import location2012.PersonAction;

/**
 *
 * @author Olner Dan
 */
public class AllFirmsGood1PriceCarrying extends Bucket {

    ArrayList<Firm> firms;

    public AllFirmsGood1PriceCarrying(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        //override to add a column in for mean
        //vals = new double[samplelength][entities.size()+1];

        firms = entities;

    }

    @Override
    public void grabData() {

        //Now... what do I want to do here? Hmm. Good question.
        //Let's start with: how many people are contributing to me?


        for (int i = 0; i < firms.size(); i++) {

            //if this firm actually has stock to sell...
            vals[samplePoint][i] = firms.get(i).getGoodCost();


        }

//        //add mean nearest neigbour value in last slot
//        vals[samplePoint][ac.size()] = distanceTot/ac.size();
//
        samplePoint++;

    }
}
