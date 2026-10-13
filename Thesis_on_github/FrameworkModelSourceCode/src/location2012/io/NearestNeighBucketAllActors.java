/*
 * Bucket for nearest neighour distances, with mean in last entry
 */
package location2012.io;

import java.util.ArrayList;
import location2012.Actor;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class NearestNeighBucketAllActors extends Bucket {

    ArrayList<Actor> actors;
    Actor a;
    //for working out mean
    double distanceTot, pdisttot, fdisttot;
    //all vars, and for people and firms
    double mean, variance, sd, pmean, pvar, psd, fmean, fvar, fsd;

    public NearestNeighBucketAllActors(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        //override to add a column in for mean
        vals = new double[samplelength][entities.size() + 6];
        colnames = new String[vals[1].length];

        colnames[vals[1].length - 6] = "mean";
        colnames[vals[1].length - 5] = "sd";
        colnames[vals[1].length - 4] = "person mean";
        colnames[vals[1].length - 3] = "person sd";
        colnames[vals[1].length - 2] = "firm mean";
        colnames[vals[1].length - 1] = "firm sd";

        actors = entities;

    }

    @Override
    public void grabData() {

        //reset vars
        distanceTot = 0;
        pdisttot = 0;
        fdisttot = 0;
        variance = 0;
        pvar = 0;
        fvar = 0;

        for (int i = 0; i < actors.size(); i++) {

            a = actors.get(i);

            vals[samplePoint][i] = a.nearestNeighbour;


            //type-specific vars
            if (a.getClass().getSimpleName().equals("Firm")) {
                fdisttot += a.nearestNeighbour;
            } else if (a.getClass().getSimpleName().equals("Person")) {
                pdisttot += a.nearestNeighbour;
            }

            distanceTot += a.nearestNeighbour;

        }

        //add mean nearest neigbour value in last slot
        mean = distanceTot / actors.size();
        pmean = pdisttot / gl.people.size();
        fmean = fdisttot / gl.firms.size();

        vals[samplePoint][actors.size()] = mean;
        vals[samplePoint][actors.size() + 2] = pmean;
        vals[samplePoint][actors.size() + 4] = fmean;

        //now we have means, work out variance
        for (int i = 0; i < actors.size(); i++) {
            
            a = actors.get(i);

            variance += Math.pow((mean - vals[samplePoint][i]), 2);
            if (a.getClass().getSimpleName().equals("Firm")) {
                fvar += Math.pow((fmean - vals[samplePoint][i]), 2);
            } else if (a.getClass().getSimpleName().equals("Person")) {
                pvar += Math.pow((pmean - vals[samplePoint][i]), 2);
            }
            
        }


        //find final variance by averaging total squared differences
        variance /= actors.size();
        pvar /= gl.people.size();
        fvar /= gl.firms.size();

        //find SD
        sd = Math.sqrt(variance);
        psd = Math.sqrt(pvar);
        fsd = Math.sqrt(fvar);

        //add to data
        vals[samplePoint][actors.size() + 1] = sd;
        vals[samplePoint][actors.size() + 3] = psd;
        vals[samplePoint][actors.size() + 5] = fsd;

        samplePoint++;


    }
}
