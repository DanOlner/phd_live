/*
 * For looking at a small number of  firms only. Each will have several columns to itself; lots could be put in, but it could be a little unmanageable
 */
package location2012.io;

import java.util.ArrayList;
import location2012.Firm;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class SmallNumFirmsOverviewBucket extends Bucket {

    ArrayList<Firm> firms;

    public SmallNumFirmsOverviewBucket(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        //override to add a column in for mean
        //vals = new double[samplelength][entities.size()+1];

        firms = entities;

        //for easy change of order and adding
        ArrayList<String> colNameArrayList = new ArrayList<String>();

        //p.s. this was completely unnecessary - note how it's done below when they're added to the output array
        //Hmm - actually, it is useful: don't have to set array length in hardcode.

        Firm f;

        for (int i = 0; i < firms.size(); i++) {

            f = (Firm) firms.get(i);

            System.out.println("Firm " + i + ": " + f.goodType);

        }

        //Make a column for each firm
        for (int i = 0; i < firms.size(); i++) {
            f = (Firm) firms.get(i);
            colNameArrayList.add("income_F" + f.goodType + i);
        }
        for (int i = 0; i < firms.size(); i++) {
            f = (Firm) firms.get(i);
            colNameArrayList.add("netIncome_F" + f.goodType + i);
        }
        for (int i = 0; i < firms.size(); i++) {
            f = (Firm) firms.get(i);
            colNameArrayList.add("wage_F" + f.wage + i);
        }
        for (int i = 0; i < firms.size(); i++) {
            f = (Firm) firms.get(i);
            colNameArrayList.add("goodcost_F" + f.goodType + i);
        }
        for (int i = 0; i < firms.size(); i++) {
            f = (Firm) firms.get(i);
            colNameArrayList.add("stock_F" + f.goodType + i);
        }
        for (int i = 0; i < firms.size(); i++) {
            f = (Firm) firms.get(i);
            colNameArrayList.add("contribTime_F" + f.goodType + i);
        }
//        colNameArrayList.add("F2_income");
//        colNameArrayList.add("F1_incomechange");
//        colNameArrayList.add("F2_incomechange");
//        colNameArrayList.add("F1_goodcost");
//        colNameArrayList.add("F2_goodcost");
        //colNameArrayList.add("distanceBetw");

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

        //Now... what do I want to do here? Hmm. Good question.
        //Let's start with: how many people are contributing to me?

        int i = 0;

        for (int j = 0; j < firms.size(); j++) {
            vals[samplePoint][i++] = firms.get(j).yesterdayRevenue;
        }
        for (int j = 0; j < firms.size(); j++) {
            vals[samplePoint][i++] = firms.get(j).dailyIncomeBalance;
        }
        for (int j = 0; j < firms.size(); j++) {
            vals[samplePoint][i++] = firms.get(j).wage;
        }
        for (int j = 0; j < firms.size(); j++) {
            vals[samplePoint][i++] = firms.get(j).goodCost;
        }
        for (int j = 0; j < firms.size(); j++) {
            vals[samplePoint][i++] = firms.get(j).getGoodStock();
        }
        for (int j = 0; j < firms.size(); j++) {
            vals[samplePoint][i++] = firms.get(j).currentContributedTime;
        }

        //vals[samplePoint][i++] = firms.get(0).getPoint().distance(firms.get(1).getPoint());


//        //add mean nearest neigbour value in last slot
//        vals[samplePoint][ac.size()] = distanceTot/ac.size();
//
        samplePoint++;

    }
}
