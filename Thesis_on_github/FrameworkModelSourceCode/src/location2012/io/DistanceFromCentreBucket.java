/*
 * Bucket for measuring People's distance from centre of torus. Specific for some density cost tests.
 */
package location2012.io;

import java.awt.geom.Point2D;
import location2012.Person;
import java.util.ArrayList;
import location2012.Actor;
import location2012.PersonAction;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class DistanceFromCentreBucket extends Bucket {

    ArrayList<Person> ac;
//    PersonAction pa;
    Actor pa;
    //Used as proxy for centrepoint
    Point2D.Double cp;

    public DistanceFromCentreBucket(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        //override to add a column in for mean
        //vals = new double[samplelength][entities.size()+1];

        ac = entities;

        //set centrepoint
        cp = new Point2D.Double(gl.width/2, gl.width/2);
      

    }

    @Override
    public void grabData() {

        //Now... what do I want to do here? Hmm. Good question.
        //Let's start with: how many people are contributing to me?



      

                for (int i = 0; i < ac.size(); i++) {

                    pa = ac.get(i);

                    vals[samplePoint][i] = pa.getPoint().distance(cp);

                }

//        //add mean nearest neigbour value in last slot
//        vals[samplePoint][ac.size()] = distanceTot/ac.size();
//
                samplePoint++;
             

    }
}
