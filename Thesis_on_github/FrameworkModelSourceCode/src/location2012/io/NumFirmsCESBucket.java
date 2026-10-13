/*
 * Just for recording number of Firms vs utility levels
 */
package location2012.io;

import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import location2012.Person;
import location2012.PersonAction;

/**
 *
 * @author Olner Dan
 */
public class NumFirmsCESBucket extends Bucket {

    ArrayList<Person> people;
    Person p;
    PersonAction pa;
    //So People can use to set their own circle size
    public static double circleSize = 0.2;
    //used for the test
    Ellipse2D.Double circle;
    //tests: if both true, both Firms have got into circle at least once
    public static boolean firm1 = false, firm2 = false;
    //to avoid infinite runs. Oo, that sounds unpleasant.
    int timeLimit = 10000;

    public NumFirmsCESBucket(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

//        System.out.println("Making new FirmsCircleBucket");

        //only grabbing one number: a single Person's utility
        vals = new double[1][1];

        
        people = entities;

      


    }

    @Override
    public void grabData() {

        pa = (PersonAction) people.get(0).actions.get(0);

        vals[0][0] = pa.bestBundle.maxUtility;
      
    }//end method grabdata
}
