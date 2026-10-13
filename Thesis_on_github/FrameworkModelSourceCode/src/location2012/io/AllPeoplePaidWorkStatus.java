/*
 * Bucket for storing people's employment status. Used for post-data creation visualisation
 */
package location2012.io;

import java.util.ArrayList;
import location2012.Firm;
import location2012.Person;
import location2012.PersonAction;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class AllPeoplePaidWorkStatus extends Bucket {

    ArrayList<Person> people;

    public AllPeoplePaidWorkStatus(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        //override to add a column in for mean
        //vals = new double[samplelength][entities.size()+1];

        people = entities;

        switch (gl.mode) {

            case Model:

                break;

            case DataRead:

                //need to do this in reverse: load data from file into array
                DataLoader.LoadData("data/" + name + ".csv", vals);

        }//end switch


    }

    @Override
    public void grabData() {

        //Now... what do I want to do here? Hmm. Good question.
        //Let's start with: how many people are contributing to me?

        PersonAction pac;

        switch (gl.mode) {

            case Model:

                for (int i = 0; i < people.size(); i++) {

                    pac = (PersonAction) people.get(i).actions.get(0);

                    if (pac.bestBundle.employer == null) {
                        vals[samplePoint][i] = 0;
                    } else {
                        vals[samplePoint][i] = 1;
                    }

                }

//        //add mean nearest neigbour value in last slot
//        vals[samplePoint][ac.size()] = distanceTot/ac.size();
//
                samplePoint++;
                break;

            case DataRead:

                for (int i = 0; i < people.size(); i++) {

                    pac = (PersonAction) people.get(i).actions.get(0);

                    if (vals[samplePoint][i] == 0) {
                        pac.bestBundle.employer = null;
                    } else {
                        pac.bestBundle.employer = new Firm(0);
                    }

                }

                samplePoint++;

        }//end switch

    }
}
