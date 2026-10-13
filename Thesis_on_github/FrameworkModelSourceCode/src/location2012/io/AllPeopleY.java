/*
 * Bucket for storing people's employment status. Used for post-data creation visualisation
 */
package location2012.io;

import java.util.ArrayList;
import location2012.Person;
import location2012.geog.HasLocation;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class AllPeopleY extends Bucket {

    ArrayList<Person> people;

    public AllPeopleY(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

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

           switch (gl.mode) {

            case Model:

                for (int i = 0; i < people.size(); i++) {

                    //pac = (PersonAction) people.get(i).actions.get(0);
                    vals[samplePoint][i] = people.get(i).gety();

                }

                samplePoint++;
                break;

            case DataRead:

                for (int i = 0; i < people.size(); i++) {

                    //pac = (PersonAction) people.get(i).actions.get(0);
                    people.get(i).getPoint().y = vals[samplePoint][i];

                }

                samplePoint++;

        }


    }
}
