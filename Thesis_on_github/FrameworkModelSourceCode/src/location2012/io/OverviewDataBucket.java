/*
 * Bucket for storing distance measurements:
 * Columns are, in order:
 *
 * move distance
 * move cost
 *
 * commute distance
 * commute cost
 *
 * total distance
 * total cost
 * 
 * work time
 * space cost
 *
 * Just added worktime. All time is here, then, and should sum to 1 x number of actors
 */
package location2012.io;

import java.lang.String;
import java.util.ArrayList;
import location2012.Actor;
import location2012.Firm;
import location2012.PersonAction;
import location2012.econs.Good;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class OverviewDataBucket extends Bucket {

    ArrayList<Actor> actors;
    Actor a;
    PersonAction p;
    Firm f;
    //For totals
    //c = cost, d = distance
    double movec, moved, commutec, commuted, workTime, prod;
    double totalNecGood, totalLuxGood, totalUtility;
    double stock1, stock2;
    double good1price, good2price, wageOffer, contribTime;
    double optBYgood1, optBYgood2;
    //for counting who bought and sold, for working out proper averages
    int good1SoldCount, good2SoldCount, hiredCount;
    int necGoodBoughtCount, luxGoodBoughtCount;
    //need to know if a type of good has been bought. Total up amount from
    //different firms, but only count that good-type as bought once.
    boolean necGoodBought;
    boolean luxGoodBought;
    double testNec, testLux, testMaxNec = 0, testMaxLux = 0;

    public OverviewDataBucket(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,


        actors = entities;


        //for easy change of order and adding
        ArrayList<String> colNameArrayList = new ArrayList<String>();

        //p.s. this was completely unnecessary - note how it's done below when they're added to the output array
        //Hmm - actually, it is useful: don't have to set array length in hardcode.
        colNameArrayList.add("opt_totGood");
        colNameArrayList.add("opt_necGood");
        colNameArrayList.add("opt_luxGood");
        colNameArrayList.add("av_utility");
        colNameArrayList.add("good1stock");
        colNameArrayList.add("good2stock");
        colNameArrayList.add("av_good1price");
        colNameArrayList.add("av_good2price");
        colNameArrayList.add("av_wageOffer");
        colNameArrayList.add("av_contributedTime");
        colNameArrayList.add("optBYgood1");
        colNameArrayList.add("optBYgood2");
        colNameArrayList.add("delcost");

        //Overwrite: setting to 3 vars for space
        vals = new double[samplelength][colNameArrayList.size()];
        colnames = new String[vals[1].length];

        //set up column names - passed by superclass
        for (int i = 0; i < colNameArrayList.size(); i++) {
            colnames[i] = colNameArrayList.get(i);
        }

    }

    @Override
    public void grabData() {

        //We only want to record wage offers and prices for those firms
        //that bought or sold. So, count those that did...
        good1SoldCount = 0;
        good2SoldCount = 0;
        hiredCount = 0;
        necGoodBoughtCount = 0;
        luxGoodBoughtCount = 0;

        for (int i = 0; i < actors.size(); i++) {

            a = actors.get(i);

            if (a.getClass().getSimpleName().equals("Person")) {

                p = (PersonAction) a.actions.get(0);

                //reset boughtgood switches
                necGoodBought = false;
                luxGoodBought = false;
                testNec = 0;
                testLux = 0;

                //System.out.println("in write. size of goods list: " + p.bestBundle.GoodsList.size());

                //test if its nec or lux good
                //Go through each good, add quantity to appropriate type
                for (Good g : p.bestBundle.GoodsList) {

                    //if (g.f.goodType == gl.GoodType.Necessity) {
                    switch (g.gs.getGoodType()) {

                        case Necessity:

                            necGoodBought = true;
                            totalNecGood += g.optimalChosenAmount;
                            testNec += g.optimalChosenAmount;

                            break;

                        case Luxury:

                            luxGoodBought = true;
                            totalLuxGood += g.optimalChosenAmount;
                            testLux += g.optimalChosenAmount;

                    }//end switch

                }//end for each

                testMaxNec = (testNec > testMaxNec ? testNec : testMaxNec);
                testMaxLux = (testLux > testMaxLux ? testLux : testMaxLux);

                if (necGoodBought) {
                    necGoodBoughtCount++;
                }
                if (luxGoodBought) {
                    luxGoodBoughtCount++;
                }

                //totalGood1 += p.bestBundle.optimalWorkgood;
                //totalGood2 += p.bestBundle.optimalWorkGood2;
                totalUtility += p.bestBundle.maxUtility;
                //System.out.println("In data, p.bestBundle.maxUtility: " + p.bestBundle.maxUtility);
                //optBYgood1 += p.bestBundle.optimumBY;
                //optBYgood2 += p.bestBundle.optimalBYgood2;

            } else if (a.getClass().getSimpleName().equals("Firm")) {

                f = (Firm) a;

                stock1 += f.getGoodStock();
                //stock2 += f.getGood2Stock();
                if (f.shiftedStock) {
                    good1price += f.getGoodCost();
                    good1SoldCount++;
                    //System.out.println("shifted 1");
                } else {
                    //System.out.println("didn't shift 1");
                }

                if (f.hiredLabour) {
                    wageOffer += f.wage;
                    hiredCount++;
                    //reset hiredLabour switch here
                    //Now resetting in all firms wage offer - this is WRONG! What's a better way to do it?
                    //Problem to fix: other objects having dependencies on flags; timing clashes
                    //f.hiredLabour = false;
                    //System.out.println("hired");
                } else {
                    //System.out.println("didn't hire labour");
                }
                contribTime += f.currentContributedTime;

            }



        }

        //System.out.println("hiredCount: " + hiredCount + ", sold2goodcount" + good2SoldCount);

        //find average of max utility
        totalUtility /= gl.numPeople;
        //only find the average of those that bought or sold
        //good1price /= good1SoldCount;
        //good2price /= good2SoldCount;
        totalNecGood /= necGoodBoughtCount;
        totalLuxGood /= luxGoodBoughtCount;
        wageOffer /= hiredCount;
        contribTime /= gl.numFirms;

        totalNecGood = (Double.isNaN(totalNecGood) ? 0 : totalNecGood);
        totalLuxGood = (Double.isNaN(totalLuxGood) ? 0 : totalLuxGood);

//        System.out.println("numbers, good1sellers: " + good1SoldCount
//                + ", good2sellers: " + good2SoldCount+ ", hiring: " + hiredCount);
        //add these to the array
        int i = 0;

        vals[samplePoint][i++] = totalNecGood + totalLuxGood;
        vals[samplePoint][i++] = totalNecGood;
        vals[samplePoint][i++] = totalLuxGood;
        vals[samplePoint][i++] = totalUtility;
        vals[samplePoint][i++] = stock1;
        vals[samplePoint][i++] = stock2;
        vals[samplePoint][i++] = good1price;
        vals[samplePoint][i++] = good2price;
        vals[samplePoint][i++] = wageOffer;
        vals[samplePoint][i++] = contribTime;
        vals[samplePoint][i++] = optBYgood1;
        vals[samplePoint][i++] = optBYgood2;
        vals[samplePoint][i++] = gl.deliveryCost;

        //reset
        totalNecGood = 0;
        totalLuxGood = 0;
        totalUtility = 0;
        stock1 = 0;
        stock2 = 0;
        good1price = 0;
        good2price = 0;
        wageOffer = 0;
        contribTime = 0;
        optBYgood1 = 0;
        optBYgood2 = 0;

        samplePoint++;

        //System.out.println("max nec: " + testMaxNec + ", max lux: " + testMaxLux);

    }
}
