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
public class AllFirmsMoneyBucket1 extends Bucket {

    ArrayList<Firm> firms;

    
    public AllFirmsMoneyBucket1(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        //override to add a column in for mean
        //vals = new double[samplelength][entities.size()+1];

        firms = entities;

    }

    @Override
    public void grabData() {

        //Now... what do I want to do here? Hmm. Good question.
        //Let's start with: how many people are contributing to me?
       

        for(int i = 0; i < firms.size(); i++) {

            vals[samplePoint][i] = firms.get(i).money;

        }

//        //add mean nearest neigbour value in last slot
//        vals[samplePoint][ac.size()] = distanceTot/ac.size();
//
        samplePoint++;

    }





}
