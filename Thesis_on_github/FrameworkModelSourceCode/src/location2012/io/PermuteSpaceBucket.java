/*
 * 
 */
package location2012.io;

import java.util.ArrayList;
import location2012.Actor;
import location2012.utils.gl;

/**
 * Uses the first x slots to store bins for
 *
 * @author Olner Dan
 */
public class PermuteSpaceBucket extends Bucket {

    ArrayList<Actor> actors;
    Actor a;
    double prod;
    //number of bins to stick data into
    int binNum = 24;
    double[][] binVals = new double[binNum][gl.people.size()];

    public PermuteSpaceBucket(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength);

        actors = entities;

    }

    @Override
    public void grabData() {

        for (int i = 0; i < actors.size(); i++) {
//        Put each value into a specific bin, and store it, using [samplepoint] as the bin.

            a = actors.get(i);

            //from yesterday and today, need to work out distance between two points
            //in days - so using position in permute order.

            //Add people size to today's value: is then maxVal
            //Subtract yesterday's - gives range
            //make proportional to max 2 day range.

            vals[samplePoint][i] = (a.permutePos + gl.people.size()) - a.yesterdayPermutePos;
            vals[samplePoint][i] /= gl.people.size();



        }

        samplePoint++;

        if (samplePoint == gl.modelRunDays -1) {
            putDataIntoBins();
        }

    }

    private void putDataIntoBins() {
        //so now we have: for each actor (row) their day gap for each model run day (column)
        //We want: bins - of binNum number - filled for specific values, for each actor over modelRunDays
        //And each bin stored in vals. Replacing the old vals.
        //The range of vals is between 0 and 2, so need to split that into binNum bins.

//        Multiply number to map to 0 – 50. So 50/2 = 25. So x * 25 = within that bin. e.g. 2 * 25 = 50. Int it! Done
        //Except none will be at 2 - so result depends a little on the rounding. Might be we make an extra bin. Let's see...

        //for each actor
        for (int i = 0; i < actors.size(); i++) {

            //for each day
            for (int j = 0; j < samplelength; j++) {

                //Count in appropriate bin
                binVals[((int) (vals[j][i] * (binNum / 2)))][i]++;

            }//for j

        }//for i

        //Now replace the Bucket data for writing
        vals = new double[samplelength][entities.size()];

        //for each actor
        for (int i = 0; i < actors.size(); i++) {

            //for each day
            for (int j = 0; j < binNum; j++) {

                //Count in appropriate bin
                vals[j][i] = binVals[j][i];

            }//for j

        }//for i

    }
}
