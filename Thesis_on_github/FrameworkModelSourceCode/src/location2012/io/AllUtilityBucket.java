/*
 * Bucket for playing about with networks connections - who's collaborating with who?
 */
package location2012.io;

import location2012.Person;
import java.util.ArrayList;
import location2012.PersonAction;
import location2012.PersonAction;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class AllUtilityBucket extends Bucket {

    ArrayList<Person> ac;
    PersonAction pa;

    public AllUtilityBucket(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        //override to add a column in for mean
        //vals = new double[samplelength][entities.size()+1];

        ac = entities;

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



        switch (gl.mode) {

            case Model:

                for (int i = 0; i < ac.size(); i++) {

                    pa = (PersonAction) ac.get(i).actions.get(0);

                    vals[samplePoint][i] = pa.bestBundle.maxUtility;

                }

//        //add mean nearest neigbour value in last slot
//        vals[samplePoint][ac.size()] = distanceTot/ac.size();
//
                samplePoint++;
                break;

            case DataRead:

                for (int i = 0; i < ac.size(); i++) {

                    pa = (PersonAction) ac.get(i).actions.get(0);

                    pa.bestBundle.maxUtility = vals[samplePoint][i];

                }

                samplePoint++;

        }

    }
}
