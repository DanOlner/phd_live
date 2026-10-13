/*
 * Bucket for storing people's x coords. Used for post-data creation visualisation
 * Also, in this one, going to use the first colname to store world width. Uuurgh.
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
public class AllPeopleX extends Bucket {

    ArrayList<Person> people;

    public AllPeopleX(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        people = entities;
        //Use this datastore to store world width.

        switch (gl.mode) {

            case Model:

                colnames = new String[1];
                HasLocation h = (HasLocation) people.get(0);
                colnames[0] = Integer.toString(h.getSpace().width);

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
                    vals[samplePoint][i] = people.get(i).getx();

                }

                samplePoint++;
                break;

            case DataRead:

                for (int i = 0; i < people.size(); i++) {

                    //pac = (PersonAction) people.get(i).actions.get(0);
                    people.get(i).getPoint().x = vals[samplePoint][i];

                }

                samplePoint++;

        }

    }
}
