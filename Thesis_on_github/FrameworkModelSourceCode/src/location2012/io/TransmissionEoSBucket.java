/*
 * 
 */
package location2012.io;

import java.util.ArrayList;
import location2012.Actor;
import location2012.PersonAction;
import location2012.SingleActorPerson;
import location2012.econs.CESOneTypeUtility;
import location2012.econs.UtilityShell;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class TransmissionEoSBucket extends Bucket {

    ArrayList<SingleActorPerson> people;
    PersonAction p;
    SingleActorPerson sp;
    //total number of actors with contributed time above a given value
    //used to indicate centralisation of production
    int totNumHigherThan;
    //Total number less than..
    int totNumLowerThan;

    CESOneTypeUtility u;

    public TransmissionEoSBucket(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        vals = new double[samplelength][6];

        people = entities;

        colnames = new String[6];

        colnames[0] = "totNumHigh";
        colnames[1] = "%numHigh";
        colnames[2] = "totNumLow";
        colnames[3] = "%numLow";
        colnames[4] = "rho";
        colnames[5] = "deliv";

    }

    @Override
    public void grabData() {


//        for (int i = 0; i < people.size(); i++) {
//        }

        totNumHigherThan = 0;
        totNumLowerThan = 0;

        for (Actor a : people) {

            sp = (SingleActorPerson) a;

            if (sp.currentContributedTime > 1.5) {
                totNumHigherThan++;
            } else {
                totNumLowerThan++;
            }

        }

        vals[samplePoint][0] = totNumHigherThan;
        vals[samplePoint][1] = (((double) totNumHigherThan)/people.size())*100;
        vals[samplePoint][2] = totNumLowerThan;
        vals[samplePoint][3] = (((double) totNumLowerThan)/people.size())*100;

        u = (CESOneTypeUtility) UtilityShell.u;
        vals[samplePoint][4] = u.rho;

        vals[samplePoint][5] = people.get(0).deliverycost;

        samplePoint++;

    }
}
