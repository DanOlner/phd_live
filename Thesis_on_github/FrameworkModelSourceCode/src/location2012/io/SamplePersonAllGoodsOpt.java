/*
 * For recording one person's optimal goods, work and backyard
 */
package location2012.io;

import java.util.ArrayList;
import location2012.Person;
import location2012.PersonAction;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class SamplePersonAllGoodsOpt extends Bucket {

    ArrayList<Person> people;
    PersonAction p;

    
    public SamplePersonAllGoodsOpt(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        //override, only need four columns
        vals = new double[samplelength][4];

        people = entities;

        colnames = new String[vals[1].length];
        colnames[0] = "optWork1";
        colnames[1] = "optWork2";
        colnames[2] = "optBY1";
        colnames[3] = "optBY2";

    }

    @Override
    public void grabData() {

        //Now... what do I want to do here? Hmm. Good question.
        //Let's start with: how many people are contributing to me?
       

        

            p = (PersonAction) people.get(gl.LOOKATME).actions.get(0);

            //vals[samplePoint][0] = p.bestBundle.optimalWorkgood;
            //vals[samplePoint][1] = p.bestBundle.optimalWorkGood2;
            //vals[samplePoint][2] = p.bestBundle.optimumBY;
            //vals[samplePoint][3] = p.bestBundle.optimalBYgood2;

        

//        //add mean nearest neigbour value in last slot
//        vals[samplePoint][ac.size()] = distanceTot/ac.size();
//
        samplePoint++;

    }





}
