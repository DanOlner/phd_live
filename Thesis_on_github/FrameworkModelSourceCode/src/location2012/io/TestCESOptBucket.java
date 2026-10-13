/*
 * Bucket for playing about with networks connections - who's collaborating with who?
 */
package location2012.io;

import java.util.ArrayList;
import location2012.econs.TestCESOptimiseHillClimb;
import location2012.econs.Good;

/**
 *
 * @author Olner Dan
 */
public class TestCESOptBucket extends Bucket {

    TestCESOptimiseHillClimb ceso;

    //for producing the difference between them
    double lastOptimum;

    public TestCESOptBucket(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        ceso = (TestCESOptimiseHillClimb) entities.get(0);

        //One column for each randomised budget mix, one for the optimum.
        //Use column names to write in original good amounts
        //Overwrite: setting to 3 vars for space
        vals = new double[samplelength][ceso.goodnum + 2];

        colnames = new String[vals[1].length+1];

        for (int i = 0; i < vals[1].length - 2; i++) {

            //colnames give initial quantity and price of goods
            //colnames[i] = "q:" + Double.toString(ceso.goods.get(i).amount) + " p:" + Double.toString(ceso.goods.get(i).price);
            //Just price:
            colnames[i] = "p:" + Double.toString(ceso.goods.get(i).price);


        }

        colnames[ceso.goodnum] = "optimum";
        colnames[ceso.goodnum+1] = "opt_diff";

    }

    @Override
    public void grabData() {

        //Write each final good amount...
//        for (int i = 0; i < ceso.chosenGoodQuantities.length; i++) {
//
//            //vals[samplePoint][i] = ceso.chosenGoodQuantities[i];
//            vals[samplePoint][i] = ceso.persistentChosenGoodQuantities[i];
//
//        }
//

        for(Good g : ceso.goods) {

            vals[samplePoint][g.id] = g.optimalChosenAmount;

        }


        //...and the found optimum
        vals[samplePoint][ceso.goods.size()] = ceso.finalCESopt;
        //...and the marginal optimum improvements
        vals[samplePoint][ceso.goods.size()+1] = ceso.finalCESopt-lastOptimum;

        lastOptimum = ceso.finalCESopt;

        samplePoint++;

    }
}
