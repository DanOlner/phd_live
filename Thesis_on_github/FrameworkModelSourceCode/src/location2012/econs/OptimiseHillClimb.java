/*
 * Optimise a CES utility function with random quantities and prices
 * 
 * Random restart Hill-climb, I think. 
 */
package location2012.econs;

import java.util.ArrayList;
import location2012.actiontools.RandPermuteArray;
import location2012.utils.Randoms;
import location2012.utils.gl;
import location2012.utils.p;

/**
 *
 * @author Dan
 */
public class OptimiseHillClimb {

    //for data output
//    CSVWriter writer;
    //HillClimbOptBucket bob;
    //number of iterations to try
    public static int iterations = 1000;
    //static double rho;
    static double budget;
    static double startBudget;
    static double leftOverMoney;
    //good amounts and cost, one for each good
    //double[] goodamount = new double[goodnum];
    //double[] goodcost = new double[goodnum];
    //available goods
    public static ArrayList<Good> goods = new ArrayList<Good>();
    //goods to test CES utility with
    //ArrayList<Good> testGoods = new ArrayList<Good>();
    //Cos I want to know the exact amount for each good type
    //and this is easier than giving them IDs. I think.
    //public double[] chosenGoodQuantities = new double[goodnum];
    //for utility calcs
    static double CEStot;
    //record of utility optimum result
    public static double CESopt;
    //permanent record of best details
    public static double finalCESopt = 0;
    //Maximum jiggle distance for each value in random search
    //Will slide jiggleSize values between these two over the range of the
    //iterations. Cf. comment below on reduction factor. The basic intuition:
    //As the algorithm homes in the optimal values, it's better to search in a
    //more local area. This will end up giving more precise results.
//    static double startJiggleSize = 0.01, endJiggleSize = 0.001, jiggleSize;
    static double startJiggleSize = 0.01, endJiggleSize = 0.001, jiggleSize;
    //static double startJiggleSize = 0.05, endJiggleSize = 0, jiggleSize;
    //This was a multiplier but it was too crude. It needs working out for
    //particular iteration-lengths and jiggle factors so that the stochastic
    //search - while focusing in - remains effective.
    //static double jiggleReductionFactor = 1;
    //
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
    /*
     * Vars for drawing optimisations. ticks by number of goods
     */
    public static double[][] pogData;
    //record of set that produced the final optimal result
    //Just giving it values for now to avoid problems
    public static double[][] optimalPogData = new double[iterations][99];

    //public static ArrayList<PogData> pogData = new ArrayList<PogData>();
    public static void optimise(Bundle b) {

//        System.out.println("In hill climb: number of goods in bundle = " + b.GoodsList.size());

        //resets
        finalCESopt = 0;
        jiggleSize = startJiggleSize;

        //calculate effective jiggle reduction factor based on iteration number and jiggle size
        //aim for an end-jiggle


        //set up values
        //set up goods
        goods.clear();
        goods.addAll(b.GoodsList);



        //if we're drawing this
        //an extra value for the utility
        if (gl.writePog) {
            pogData = new double[iterations][goods.size() + 1];
            //make one PogData per good

        }

        //set budget to wage from firm, proportionate to my time
        //bestWage has already been worked out as bestWage = f.wageoffer * remainingTime;
        //PersonAction findBestWagesAndPrices().
        startBudget = b.bestWage;

//        System.out.println("Optimiser start budget: " + startBudget);

        for (int i = 0; i < b.GoodsList.size(); i++) {

            //ID, amount, price
            //goods.add(new Good(i, Randoms.nextDouble() * 2, Randoms.nextDouble() * 4));
            //Add one to stop very cheap goods massively warping result
            //goods.add(new Good(i, 10, (Randoms.nextDouble() * 4)+1));
            //randomised quantities, fixed prices in increments
            //goods.add(new Good(i, Randoms.nextDouble()*2, ((double)i+5)/50));
            //testing setting prices, fixed high quantity
            //goods.add(new Good(i, 10, i + 5));
            //Fixed price: equivalent to goods entering symmetrically into demand
            //goods.add(new Good(i, 10, 1));

            //Set initial random pre-pie-slice values, to be hill-climbed
            goods.get(i).slice = Randoms.nextDouble();

            //copy for testGoods. Couldn't get clone to work, don't know why.
            //testGoods.add(new Good(i, goods.get(i).amount, goods.get(i).price));
            //System.out.println("good " + i + ": quantity = " + goods.get(i).amount + ", price = " + goods.get(i).price);

        }

        normaliseSlices();
        //Normalise slices to one.
        //They always stay in proportion to each other, and it's only that proportion that matters
        //Normalising them means that jiggleSize doesn't change over time, relatively.
        //normaliseSlices();

        //Just shifted this below - didn't seem to make the blindest bit of difference


        //run optimisation, setting to a specific value for now, to test
        for (int i = 0; i < iterations; i++) {

            iterate();
            //Normalise slices to one.
            //They always stay in proportion to each other, and it's only that proportion that matters
            //Normalising them means that jiggleSize doesn't change over time, relatively.

            if (gl.writePog) {

                for (Good g : goods) {
                    pogData[i][g.id] = g.optimalChosenAmount;
                    //System.out.println("id: " + g.id);
                }

                pogData[i][goods.size()] = finalCESopt;
                //System.out.println("podsdf: " + pogData[i][goods.size()]);


            }

        }

        //use final CES optimum for best utility
        b.maxUtility = finalCESopt;
//        System.out.println("maxUtility in Optimise hill climb: " + finalCESopt);
        if (leftOverMoney > 0) {
            //if any left over, need to reduce wage ask by this amount
            if (gl.peopleFixWage == 0) {
                System.out.println("At end of hill climb, leftover Money: " + leftOverMoney + ", wage: " + b.bestWage);
                System.out.println("my current work time: " + b.optimalWorkTime);
                //correct optimal work time too
                System.out.print("Optimal work time before: " + b.optimalWorkTime);
                b.optimalWorkTime *= ((b.bestWage - leftOverMoney)/b.bestWage);
                System.out.println(", after: " + b.optimalWorkTime);
                b.bestWage -= leftOverMoney;
            }
        }

    }

    /*
     * Iterate the optimisation
     */
    public static void iterate() {

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
            g.jiggledSlice = g.slice + ((0.5 - Randoms.nextDouble()) * jiggleSize);
            //g.jiggledSlice = Math.abs(g.jiggledSlice);
            //p.a("jigglechange: " + ((0.5 - Randoms.nextDouble()) * jiggleSize));
            //g.jiggledSlice = g.slice + (Randoms.nextDouble() * jiggleSize);
            //normaliseJiggledSlices();
            //System.out.print(g.jiggledSlice + " ");

        }

        //check all those jiggleSlice values were positive. If they weren't, bounce them back
        //from zero
        checkJiggledSlicesPositive();


        //System.out.println(" ");

        //loop through assigning money, until all money gone or no more goods could be bought
        //int spendCount = 0;

        while (budget > 0 && anyGoodsLeft) {

//            spendCount++;
//            p.p("spendcount: " + spendCount);

            //get initial amounts to spend on each good
            //values set in Goods tempBudgetSpend
            p.p("budget: " + budget);

            splitBudget(budget);



            //If the amount of money I want to spend on each good doesn't exceed the quantity available
            leftOverMoney = 0;

            //Record the amount I can get for my money
            //Also record if there isn't enough stock - if my money would buy everything out

            //Randomise order of goods; can affect spending outcome
            goods = RandPermuteArray.mix(goods);

//            System.out.print("Good order: ");
//            for(Good g : goods) {System.out.print(g.id + ", amount: " + g.amount);}

            for (Good g : goods) {

                double unitsIcanBuyAtThisPrice = g.tempBudgetSpend / g.price;
                //p.p("units I can buy at this price: " + unitsIcanBuyAtThisPrice);

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


            //reassign remaining money as budget
            budget = leftOverMoney;

            //check goods situation for while test
            anyGoodsLeft = false;

            for (Good g : goods) {

                if (g.tempAmountLeft > 0) {
                    anyGoodsLeft = true;
                    //p.p("Goods left: " + g.id + ", " + g.amount);
                }

            }

        }//end while


//        for (Good g : goods) {
//            p.p("Optimise: goods going into utility: " + g.chosenAmount);
//        }
        CESopt = UtilityShell.giveUtility(goods);

        //p.p("Utility: " + CESopt);

        //check for new best
        if (CESopt > finalCESopt) {

            finalCESopt = CESopt;

            //System.arraycopy(chosenGoodQuantities, 0, persistentChosenGoodQuantities, 0, chosenGoodQuantities.length);
            for (Good g : goods) {
                g.optimalChosenAmount = g.chosenAmount;
                //make randomly altered slice positions the new base
                g.slice = g.jiggledSlice;

            }

            normaliseSlices();

        }//end if

        //normalise slices back to a sum of one.
        //Wish I could remember why this needs to go here...

        //reduce jigglesize slightly each time to make search more precise
        //jiggleSize *= jiggleReductionFactor;
        //Slide values e.g. start = 0.5, end = 0.2. Slide between them in even steps. End - start = range (and polarity). Range/iterations = steps.
        jiggleSize += ((endJiggleSize - startJiggleSize) / iterations);
        //System.out.println("jiggleSize: " + jiggleSize);


    }//end constructor

    private static void splitBudget(double budget) {

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



        for (Good g : goods) {

            g.tempBudgetSpend = (g.tempBudgetSpend / averagingTotal) * budget;


            //p.p("Optimise: Good " + g.id + ", type = " + g.f.goodType + ", spend = " + g.tempBudgetSpend);

        }

        //tests
        double testTot = 0;
        for (Good g : goods) {

            testTot += g.tempBudgetSpend;

        }

        //p.p("optimise: budget = " + budget + ", totalled good spend slices: " + testTot);


    }//end splitBudget method


    /*
     * For making sure all slices total one. Necessary to stop jiggling from
     * shrinking in effect over time, relatively
     */
    private static void normaliseSlices() {

        double averagingTotal = 0;
        //split into chunks, one for each good
        //double[] budgetSplit = new double[goods.size()];

        //System.out.println("pre-m");

        for (Good g : goods) {

            //System.out.println(g.slice);
            averagingTotal += g.slice;

        }

        //System.out.println("post-n");

        for (Good g : goods) {

            //renormalise jiggle sizes.
            g.slice = (g.slice / averagingTotal) * 1;
            //System.out.println(g.slice);

        }

        //test
        averagingTotal = 0;
        for (Good g : goods) {

            averagingTotal += g.slice;

        }

        //System.out.println("Normalised slice total: " + averagingTotal);


    }//end method normaliseSlices

    /*
     * For making sure all slices total one. Necessary to stop jiggling from
     * shrinking in effect over time, relatively
     */
    private static void normaliseJiggledSlices() {

        double averagingTotal = 0;
        //split into chunks, one for each good
        //double[] budgetSplit = new double[goods.size()];

        for (Good g : goods) {

            averagingTotal += g.jiggledSlice;

        }

        for (Good g : goods) {

            //renormalise jiggle sizes.
            g.jiggledSlice = (g.jiggledSlice / averagingTotal) * 1;

        }

        //test
        averagingTotal = 0;
        for (Good g : goods) {

            averagingTotal += g.jiggledSlice;

        }

        //p.p("Normalised jiggleSlice total: " + averagingTotal);


    }//end method normaliseSlices


    /*
     * check all those jiggleSlice values were positive. If they weren't, bounce them back
     * from zero
     */
    private static void checkJiggledSlicesPositive() {

        for (Good g : goods) {

            g.jiggledSlice = (g.jiggledSlice > 0 ? g.jiggledSlice : Math.abs(g.jiggledSlice));

        }

    }

    //get utility from given range of goods
//    private static double getCESUtility() {
//
//        CEStot = 0;
//        //add the quantities, with exponent
//        for (Good g : goods) {
//
//            CEStot += Math.pow(g.chosenAmount, rho);
//
//        }
//
//        //raise the lot to 1/rho
//        return Math.pow(CEStot, (1 / rho));
//
//    }//end method getCESUtility

    /*
     * If I'm visualising this data, set when a new bundle optimum is found
     */
    public static void setOptimalPog() {

        optimalPogData = pogData;

    }
}//END OF DA CLASS, WE IZ ALL GOINZ OME NOAW
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

public int getWeight() {
return 0;
}

public void giveShouterTo(Audible a, double val) {

//While I'm here, set up the data outputter that'll be used
//Put it first so it gets called by timeline first
//        DataStore ds = new DataStore(a, val);
//        a.registerShouter(new Shouter(this, val));
//        writer = new CSVWriter(ds, a, 0);
//        //turn optimalPogData into arraylist
//        //List list = new ArrayList(Arrays.asList(optimalPogData));
//        //just placeholder ArrayList
//        bob = new HillClimbOptBucket("HillClimbData", new ArrayList(), iterations);
//        //set array - different method here cos I'm not using arraylist
//        bob.setOptData(optimalPogData);
//        ds.addBucket(bob);


}

public void heard(ShoutEvent s) {

//write data
//set array - different method here cos I'm not using arraylist
//        bob.setOptData(optimalPogData);
//        writer.heard(s);



}

public void setWeight(int weight) {
}

}//end inner class*/
