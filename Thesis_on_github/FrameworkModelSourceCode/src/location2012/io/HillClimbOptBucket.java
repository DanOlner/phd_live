/*
 * 
 */
package location2012.io;

import java.util.ArrayList;

/**
 *
 * @author Olner Dan
 */
public class HillClimbOptBucket extends Bucket {

    int samplelength;
    //double[][] optData;
   

    public HillClimbOptBucket(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        //vals = new double[samplelength][entities.size()];

        //ignore input arraylist; use another method in the hillclimb to get data across
        //Have to do this to get around the need for it to remain a bucket

        
//        colnames = new String[vals[1].length];
//
//        //set up column names - passed by superclass
//        colnames[0] = "opt_necGood";
//        colnames[1] = "opt_luxGood";
//        colnames[2] = "av_utility";
//        colnames[3] = "good1stock";
//        colnames[4] = "good2stock";
//        colnames[5] = "av_good1price";
//        colnames[6] = "av_good2price";
//        colnames[7] = "av_wageOffer";
//        colnames[8] = "av_contributedTime";
//
//        colnames[9] = "optBYgood1";
//        colnames[10] = "optBYgood2";

    }

    public void setOptData(double[][] d) {

        vals = d;

    }

    @Override
    public void grabData() {


        //this is already done in setOptData - just needs pointing to
        //the correct array

        //System.out.println("In opt grabdata - ping!");
        //In this one, I''l want to grab the entire data in one go
        //As it will then immediately be written. thusly:
//        for (int i = 0; i < samplelength; i++) {
//
//            for (int j = 0; j < entities.size(); j++) {
//                double[] ds = vals[j];
//
//            }
//
//        }


      

       

       

//        System.out.println("numbers, good1sellers: " + good1SoldCount
//                + ", good2sellers: " + good2SoldCount+ ", hiring: " + hiredCount);
        //add these to the array
//        vals[samplePoint][0] = totalNecGood;
//        vals[samplePoint][1] = totalLuxGood;
//        vals[samplePoint][2] = totalUtility;
//        vals[samplePoint][3] = stock1;
//        vals[samplePoint][4] = stock2;
//        vals[samplePoint][5] = good1price;
//        vals[samplePoint][6] = good2price;
//        vals[samplePoint][7] = wageOffer;
//        vals[samplePoint][8] = contribTime;
//        vals[samplePoint][9] = optBYgood1;
//        vals[samplePoint][10] = optBYgood2;

        //reset
//        totalNecGood = 0;
//        totalLuxGood = 0;
//        totalUtility = 0;
//        stock1 = 0;
//        stock2 = 0;
//        good1price = 0;
//        good2price = 0;
//        wageOffer = 0;
//        contribTime = 0;
//        optBYgood1 = 0;
//        optBYgood2 = 0;
//
//        samplePoint++;

        //System.out.println("max nec: " + testMaxNec + ", max lux: " + testMaxLux);

    }
}
