/*
 * Bucket for playing about with networks connections - who's collaborating with who?
 */
package location2012.io;

import java.util.ArrayList;
import location2012.Person;

/**
 *
 * @author Olner Dan
 */
public class AllPeopleMoney extends Bucket {

    ArrayList<Person> people;

    
    public AllPeopleMoney(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        //override to add a column in for mean
        //vals = new double[samplelength][entities.size()+1];

        people = entities;

    }

    @Override
    public void grabData() {

        //Now... what do I want to do here? Hmm. Good question.
        //Let's start with: how many people are contributing to me?
       

        for(int i = 0; i < people.size(); i++) {

            vals[samplePoint][i] = people.get(i).myMoney;

        }

//        //add mean nearest neigbour value in last slot
//        vals[samplePoint][ac.size()] = distanceTot/ac.size();
//
        samplePoint++;

    }





}
