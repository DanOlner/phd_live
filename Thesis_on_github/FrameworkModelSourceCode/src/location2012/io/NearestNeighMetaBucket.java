/*
 * MetaBucket for many-run data collection
 */
package location2012.io;

import java.util.ArrayList;
import location2012.Actor;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class NearestNeighMetaBucket extends Bucket {

    ArrayList<Actor> actors;
    Actor a;
    //for working out mean
    double distanceTot, pdisttot, fdisttot;
    //all vars, and for people and firms
    double mean, variance, sd, pmean, pvar, psd, fmean, fvar, fsd;

    public NearestNeighMetaBucket(String str, int samplelength) {

        //str is set in superconstructor, samplelength is not
        super(str, samplelength);

        vals = new double[samplelength][6];

        colnames = new String[6];
//
        colnames[0] = "mean";
        colnames[1] = "sd";
        colnames[2] = "person mean";
        colnames[3] = "person sd";
        colnames[4] = "firm mean";
        colnames[5] = "firm sd";

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

        //quite different process for many-run data collection.

        //if it's the first run, we can just nab the data wholesale
        if (gl.currentRun == 0) {

            vals = b.vals.clone();

            //last run: can average after adding
        } else if (gl.currentRun == gl.numberOfRandomSeedChanges - 1) {

            for (int i = 0; i < vals[1].length; i++) {

                for (int j = 0; j < vals.length; j++) {

//                    System.out.println("val: " + b.vals[j][i]);
                    vals[j][i] += b.vals[j][i];
                    //Last run: work out averages
                    vals[j][i] /= gl.numberOfRandomSeedChanges;

                }

            }//end for loops


        } else {

            for (int i = 0; i < vals[1].length; i++) {

                for (int j = 0; j < vals.length; j++) {

//                    System.out.println("val: " + b.vals[j][i]);
                    vals[j][i] += b.vals[j][i];

                }

            }//end for loops

        }


    }
}
