/*
 * Testing ways to optimise a CES utility function with random quantities and prices
 * So objective functions don't work.
 *
 * Hill-climb version
 */
package location2012.econs;

import java.util.ArrayList;
import location2012.actiontools.RandPermuteArray;
import location2012.observe.Audible;
import location2012.observe.Listener;
import location2012.observe.ShoutEvent;
import location2012.observe.Shouter;
import location2012.utils.Randoms;
import location2012.utils.p;

/**
 *
 * @author Dan
 */
public class TestCESOptimiseHillClimb implements Listener {

    //number of goods in the CES
    public int goodnum = 10;
    //love of variety
    //0<rho<1
    //As rho -> 0, consumer is better off with a mix of goods.
    double rho = 0.5;
    double budget;
    double startBudget = 3;
    //good amounts and cost, one for each good
    //double[] goodamount = new double[goodnum];
    //double[] goodcost = new double[goodnum];
    //available goods
    public ArrayList<Good> goods = new ArrayList<Good>();
    //goods to test CES utility with
    //ArrayList<Good> testGoods = new ArrayList<Good>();
    //Cos I want to know the exact amount for each good type
    //and this is easier than giving them IDs. I think.
    //public double[] chosenGoodQuantities = new double[goodnum];
    //for utility calcs
    double CEStot;
    //record of utility optimum result
    public double CESopt;
    //permanent record of best details
    public double finalCESopt = 0;
    //Maximum jiggle distance for each value in random search
    double jiggleSize = 0.005;
    //jiggleSize will be multiplied by this each time a new optimum is found
    double jiggleReductionFactor = 0.995;
    //These vars: not used now, keeping just in case...
    //Normalise jiggleSize change to the difference of the average of the first
    //three optimum changes. Not the first because it's too large in proportion
    //to the rest
    //double[] first3OptimaChanges = {0, 0, 0};
    //average of the three
    //double av3opts;
    //int optCount = -2;
    //double optimaNorm;
    //t-1 optimum

    public TestCESOptimiseHillClimb(Audible a, double val) {

        giveShouterTo(a, val);

        //set up values
        //set up goods
        for (int i = 0; i < goodnum; i++) {

            //ID, amount, price
            //goods.add(new Good(i, Randoms.nextDouble() * 2, Randoms.nextDouble() * 4));
            //Add one to stop very cheap goods massively warping result
            //goods.add(new Good(i, 10, (Randoms.nextDouble() * 4)+1));
            //randomised quantities, fixed prices in increments
            //goods.add(new Good(i, Randoms.nextDouble()*2, ((double)i+5)/50));
            //testing setting prices, fixed high quantity
            //goods.add(new Good(i, 10, i + 5));
            //Fixed price: equivalent to goods entering symmetrically into demand
            goods.add(new Good(i, 10, 1));

            //Set initial random pre-pie-slice values, to be hill-climbed
            goods.get(i).slice = Randoms.nextDouble();

            //copy for testGoods. Couldn't get clone to work, don't know why.
            //testGoods.add(new Good(i, goods.get(i).amount, goods.get(i).price));
            //p.p("good " + i + ": quantity = " + goods.get(i).amount + ", price = " + goods.get(i).price);

        }

        //Normalise slices to one.
        //They always stay in proportion to each other, and it's only that proportion that matters
        //Normalising them means that jiggleSize doesn't change over time, relatively.
        normaliseSlices();

    }

    /*
     * Run once a day, to get data out
     */
    public void heard(ShoutEvent s) {

        //a record of the temporary total quantity of goods available
        boolean anyGoodsLeft = true;

        //reset vars
        for (Good g : goods) {

            g.tempAmountLeft = g.amount;
            g.chosenAmount = 0;

        }

        budget = startBudget;

        //randomly choose a nearby position in the search space. Remember where,
        //since if it doesn't improve things, we go back to the previous values
        //System.out.print("new jiggle sizes: ");
        for (Good g : goods) {
            //Note, this doesn't need to jiggle "in both directions" since these
            //values are all totalled as used as proportions to slice the budget "pie"
            //Their role is only in exploring the space randomly, not locating anywhere
            //within it.
            //g.jiggledSlice = g.slice + ((0.5 - Randoms.nextDouble()) * jiggleSize);
            //g.jiggledSlice = Math.abs(g.jiggledSlice);
            //p.a("jigglechange: " + ((0.5 - Randoms.nextDouble()) * jiggleSize));
            g.jiggledSlice = g.slice + (Randoms.nextDouble() * jiggleSize);
            //System.out.print(g.jiggledSlice + " ");
        }

        //System.out.println(" ");

        //loop through assigning money, until all money gone or no more goods could be bought
        while (budget > 0 && anyGoodsLeft) {

            //get initial amounts to spend on each good
            //values set in Goods tempBudgetSpend
            splitBudget(budget);

            //If the amount of money I want to spend on each good doesn't exceed the quantity available
            double leftOverMoney = 0;

            //Record the amount I can get for my money
            //Also record if there isn't enough stock - if my money would buy everything out

            //Randomise order of goods; can affect spending outcome
            goods = RandPermuteArray.mix(goods);

//            System.out.print("Good order: ");
//            for(Good g : goods) {System.out.print(g.id + " ");}

            for (Good g : goods) {

                double unitsIcanBuyAtThisPrice = g.tempBudgetSpend / g.price;
                //System.out.println("units I can buy at this price: " + unitsIcanBuyAtThisPrice);

                //If the stock is more than or equal to the amount I want to buy...
                if (g.tempAmountLeft >= unitsIcanBuyAtThisPrice) {

                    //record that amount
                    g.chosenAmount += unitsIcanBuyAtThisPrice;
                    //also keep a record of how much is left, in case I end up reassigning money
                    g.tempAmountLeft -= unitsIcanBuyAtThisPrice;


                    //Otherwise, there's not enough stock to cover my demand
                    //So I need to take what I can and re-assign the money
                } else {

                    g.chosenAmount += g.tempAmountLeft;
                    //keep any leftover money
                    //which should be my budget for this good, minus what the amount left cost.
                    leftOverMoney += (g.tempBudgetSpend - (g.tempAmountLeft * g.price));
                    //Took all that was left.
                    g.tempAmountLeft = 0;

                }

                //p.p("Testing tempAmountLeft. This should never get below zero: " + g.tempAmountLeft);

            }//end for each Good g

            //System.out.println("Leftover Money: " + leftOverMoney);
            //reassign remaining money as budget
            budget = leftOverMoney;

            //check goods situation for while test
            anyGoodsLeft = false;

            for (Good g : goods) {

                if (g.tempAmountLeft > 0) {
                    anyGoodsLeft = true;
                }

            }

        }//end while

        //p.p("Good amounts going into utility: ");
        for (Good g : goods) {

            p.p(g.chosenAmount + " ");

        }

        p.p(" ");

        CESopt = getCESUtility();

        p.p("Utility: " + CESopt);

        //check for new best
        if (CESopt > finalCESopt) {

            finalCESopt = CESopt;

            //System.arraycopy(chosenGoodQuantities, 0, persistentChosenGoodQuantities, 0, chosenGoodQuantities.length);
            for (Good g : goods) {
                g.optimalChosenAmount = g.chosenAmount;
                //make randomly altered slice positions the new base
                g.slice = g.jiggledSlice;
            }

            //normalise slices back to a sum of one.
            normaliseSlices();

        }//end if


        //reduce jigglesize slightly each time to make search more precise
        jiggleSize *= jiggleReductionFactor;


    }//end constructor

    private void splitBudget(double budget) {

        //Randomly split budget.
        //RandTotal: use this as denominator to split budget
        double averagingTotal = 0;
        //split into chunks, one for each good
        //double[] budgetSplit = new double[goods.size()];

        for (Good g : goods) {

            //if there are any goods left over for this good
            if (g.tempAmountLeft > 0) {
                //use jiggled slice as basis for budget split
                g.tempBudgetSpend = g.jiggledSlice;
                averagingTotal += g.tempBudgetSpend;
            } else {
                g.tempBudgetSpend = 0;
            }

        }


        //test
        //double testTot = 0;

        p.p("Budget total: " + budget);
        //repeat, assigning actual money amounts to each good chunk
//        for (int i = 0; i < goods.size(); i++) {
//
//            //so budgetsplit[i] divided by randTotal should give proportion
//            budgetSplit[i] = (budgetSplit[i] / randTotal) * budget;
//            //testTot += budgetSplit[i];
//            System.out.println("Spend for good " + i + " = " + budgetSplit[i]);
//
//        }

        for (Good g : goods) {

            g.tempBudgetSpend = (g.tempBudgetSpend / averagingTotal) * budget;

            //renormalise jiggle sizes.
            //g.jiggledSlice = (g.jiggledSlice / averagingTotal) * 1;

            p.p("Spend for good " + g.id + " = " + g.tempBudgetSpend);

        }

        //System.out.println("Test, this should equal budget: " + testTot);
        //return budgetSplit;

    }//end splitBudget method


    /*
     * For making sure all slices total one. Necessary to stop jiggling from
     * shrinking in effect over time, relatively
     */
    private void normaliseSlices() {

        double averagingTotal = 0;
        //split into chunks, one for each good
        //double[] budgetSplit = new double[goods.size()];

        for (Good g : goods) {

            averagingTotal += g.slice;

        }

        for (Good g : goods) {

            //renormalise jiggle sizes.
            g.slice = (g.slice / averagingTotal) * 1;

        }

        //test
        averagingTotal = 0;
        for (Good g : goods) {

            averagingTotal += g.slice;

        }

        System.out.println("Normalised slice total: " + averagingTotal);


    }//end method normaliseSlices

    //get utility from given range of goods
    private double getCESUtility() {

        CEStot = 0;
        //add the quantities, with exponent
        for (Good g : goods) {

            CEStot += Math.pow(g.chosenAmount, rho);

        }

        //raise the lot to 1/rho
        return Math.pow(CEStot, (1 / rho));

    }//end method getCESUtility

    /*inner class, good
    public class Good {

        //initial slice sizes for budget split
        public double slice;
        //randomly jiggled position of slices; will keep if finds better optima,
        //or will revert to previous if not
        public double jiggledSlice;
        //
        public double amount, price;
        //for temporarily recording amounts left
        public double tempAmountLeft;
        //Temporary var: proposed amount of budget spend
        public double tempBudgetSpend;
        //ID of good. Makes code tidier elsewhere, e.g. allows foreaches
        public int id;
        //chosen amount, temporary
        public double chosenAmount;
        //The amount the final optimisation finds
        public double optimalChosenAmount;

        public Good(int id, double amount, double price) {

            this.id = id;
            this.amount = amount;
            tempAmountLeft = amount;
            this.price = price;

        }

        public void tempAmountLeft(double amountLeft) {

            tempAmountLeft = amountLeft;

        }
    }//end inner class*/


    /*
     * Implemented listener methods
     *
     */
    public void giveShouterTo(Audible a, double val) {

        a.registerShouter(new Shouter(this, val));

    }

    public int getWeight() {

        return 0;

    }

    public void setWeight(int weight) {
    }
}
/*
 * CUTTINZ
 *
 *
 */

/*
 * This was after "if (CESopt > finalCESopt) {"
 * Attempting to reduce jiggle size adaptively. Problem: massive positive feedback shrinks
 * it to useless, I think. 



//check on changing jiggle size
//Dismiss first one - usually too big
if (optCount > 0 && optCount < 4) {

first3OptimaChanges[optCount-1] = CESopt - finalCESopt;
p.a("In 1st 3 opt count: " + optCount + ", " + first3OptimaChanges[optCount-1]);


//if we have 3 values, find average
if (optCount == 3) {
av3opts = (first3OptimaChanges[0] + first3OptimaChanges[1] + first3OptimaChanges[2]) / 3;
//normalise change value to this

}

//otherwise, change jiggle size
} else if(optCount>0) {
//change jiggle size in proportion to average of first three diffs
//CESopt - finalCESopt is the current difference
//av3opts was the average start difference
//Most likely the difference will decrease over time
//So we want the jiggle size to reduce accordingly
//By using the ratio of the two.
//Always in proportion to the jiggle size we started with
jiggleSize = startJiggleSize * ((CESopt - finalCESopt)/av3opts);
p.a("Jiggle size changed to " + jiggleSize + ", opt diff was: " + (CESopt - finalCESopt));

}

optCount++;
  optCount++;

            //check on changing jiggle size
            //Dismiss first one - usually too big
            if (optCount > 0 && optCount < 4) {

                first3OptimaChanges[optCount - 1] = CESopt - finalCESopt;
                p.a("In 1st 3 opt count: " + optCount + ", " + first3OptimaChanges[optCount - 1]);


                //if we have 3 values, find average
                if (optCount == 3) {
                    av3opts = (first3OptimaChanges[0] + first3OptimaChanges[1] + first3OptimaChanges[2]) / 3;
                    //normalise change value to this

                }

                //otherwise, change jiggle size
            } else if (optCount > 0) {
                //change jiggle size in proportion to average of first three diffs
                //CESopt - finalCESopt is the current difference
                //av3opts was the average start difference
                //Most likely the difference will decrease over time
                //So we want the jiggle size to reduce accordingly
                //By using the ratio of the two.
                //Always in proportion to the jiggle size we started with
                jiggleSize = startJiggleSize * ((CESopt - finalCESopt) / av3opts);
                p.a("Jiggle size changed to " + jiggleSize + ", opt diff was: " + (CESopt - finalCESopt) + "av3opts is: " + av3opts);

            }

 */
