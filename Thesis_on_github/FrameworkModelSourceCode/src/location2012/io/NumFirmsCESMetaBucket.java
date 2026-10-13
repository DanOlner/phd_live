/*
 * MetaBucket for many-run data collection: firm circle game meta bucket stores only
 * the run's days for one firm, then two firms arriving in centre.
 *
 */
package location2012.io;

import java.util.ArrayList;
import location2012.Actor;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class NumFirmsCESMetaBucket extends Bucket {

    ArrayList<Actor> actors;
    Actor a;
    //testing bucket is working
    FirmsCircleGameBucket fcg;
    //for working out mean
    double distanceTot, pdisttot, fdisttot;
    //all vars, and for people and firms
    double firstMean, firstVariance, firstSD, bothMean, bothVariance, bothSD;
    //local array for storing values before finding aggregate stats
    double[][] data;
    boolean first = true;

    public NumFirmsCESMetaBucket(String str, int samplelength) {

        //str is set in superconstructor, samplelength is not
        super(str, samplelength);

        //We need two arrays:
        //One stores the param-run’s data, then averages at end.
        //So that’s 2 x random number
        //The main data one, which is only three vars:
        //Param val
        //First firm in (average)
        //Both firms in (average)
        //And stores one val for each param sweep point. Samplelength has that value
        //worked out from Main.

        //data before averaging etc
        data = new double[gl.numberOfRandomSeedChanges][1];

        //ignore samplelength: this needs to
        vals = new double[samplelength][2];

        colnames = new String[5];
//
        colnames[0] = "paramval";
        colnames[1] = "utility";

    }

//    public NearestNeighMetaBucket(String str, ArrayList entities, int samplelength) {
//        super(str, entities, samplelength); //- not passing these up here,
//
//        //override to add a column in for mean
//        vals = new double[samplelength][6];
//
//        actors = entities;
//
//    }
    @Override
    public void grabData(Bucket b) {

        System.out.println("Grabbing multi-run data");

//      
        //get this randomisation's data
        data[gl.currentRun][0] = b.vals[0][0];
        
        if (gl.currentRun + 1 == gl.numberOfRandomSeedChanges) {

            System.out.println("Multirun: time to grab data. Samplepoint: " + samplePoint + ", data length: " + data.length);

            //write param val in first column
            vals[samplePoint][0] = gl.paramSweepVal;

            vals[samplePoint][1] = data[gl.currentRun][0];

            samplePoint++;

        }//end aggregate data write check


    }
}
//quite different process for many-run data collection.
//if it's the first run, we can just nab the data wholesale
//        if (gl.currentRun == 0) {
//
//            vals = b.vals.clone();
//
//            //last run: can average after adding
//        } else if (gl.currentRun == gl.numberOfRuns - 1) {
//
//            for (int i = 0; i < vals[1].length; i++) {
//
//                for (int j = 0; j < vals.length; j++) {
//
////                    System.out.println("val: " + b.vals[j][i]);
//                    vals[j][i] += b.vals[j][i];
//                    //Last run: work out averages
//                    vals[j][i] /= gl.numberOfRuns;
//
//                }
//
//            }//end for loops
//
//
//        } else {
//
//            for (int i = 0; i < vals[1].length; i++) {
//
//                for (int j = 0; j < vals.length; j++) {
//
////                    System.out.println("val: " + b.vals[j][i]);
//                    vals[j][i] += b.vals[j][i];
//
//                }
//
//            }//end for loops
//
//        }

