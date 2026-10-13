/*
 * Bucket for playing about with networks connections - who's collaborating with who?
 */
package location2012.io;

import java.util.ArrayList;
import location2012.Firm;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class AllFirmsContribTime extends Bucket {

    ArrayList<Firm> firms;

    public AllFirmsContribTime(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        firms = entities;

        switch (gl.mode) {

            case Model:
                //use colnames to label firms for which good they're making
                colnames = new String[firms.size()];

                for (int i = 0; i < firms.size(); i++) {

                    if (firms.get(i).makingGood1) {
                        colnames[i] = "1";
                    } else {
                        colnames[i] = "2";
                    }
                }

                break;

            case DataRead:

                //need to do this in reverse: load data from file into array
                DataLoader.LoadData("data/" + name + ".csv", vals);
                

        }//end switch


    }

    @Override
    public void grabData() {

        //change depending on run mode
        switch (gl.mode) {

            case Model:

                for (int i = 0; i < firms.size(); i++) {

                    vals[samplePoint][i] = firms.get(i).currentContributedTime;

                }

//        //add mean nearest neigbour value in last slot
//        vals[samplePoint][ac.size()] = distanceTot/ac.size();
//
                samplePoint++;

                break;

            case DataRead:

                for (int i = 0; i < firms.size(); i++) {

                    firms.get(i).currentContributedTime = vals[samplePoint][i];

                }

                samplePoint++;

        }//end switch

    }
}
