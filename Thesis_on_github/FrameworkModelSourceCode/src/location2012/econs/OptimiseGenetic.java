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
public class OptimiseGenetic {

   
    //number of iterations to try
    public static int iterations = 50000;
    //Size of initial chromosome population
    public static int chromoPop = 500;
    //Array for holding them
    public static Chromosome[] chromosomes = new Chromosome[chromoPop];
    static double budget;
    static double startBudget;
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
    //static double CEStot;
    //record of utility optimum result
    public static double utilityOpt;
    //permanent record of best details
    public static double maxUtilityOpt = 0;
    
    /*
     * Vars for drawing optimisations. ticks by number of goods
     */
    public static double[][] pogData;
    //record of set that produced the final optimal result
    //Just giving it values for now to avoid problems
    public static double[][] optimalPogData = new double[iterations][99];

    //public static ArrayList<PogData> pogData = new ArrayList<PogData>();
    public static void optimise(Bundle b) {

        //resets
        maxUtilityOpt = 0;
        
        //set up values
        //set up goods
        goods.clear();
        goods.addAll(b.GoodsList);

        //set up chromosomes
        for(Chromosome ch : chromosomes) {
            ch = new Chromosome(goods.size());
        }


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

                pogData[i][goods.size()] = maxUtilityOpt;
                //System.out.println("podsdf: " + pogData[i][goods.size()]);


            }

        }

        //use final CES optimum for best utility
        b.maxUtility = maxUtilityOpt;
        //System.out.println("maxUtility in Optimise hill climb: " + finalCESopt);

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
            //g.jiggledSlice = g.slice + ((0.5 - Randoms.nextDouble()) * jiggleSize);
            //g.jiggledSlice = Math.abs(g.jiggledSlice);
            //p.a("jigglechange: " + ((0.5 - Randoms.nextDouble()) * jiggleSize));
            //g.jiggledSlice = g.slice + (Randoms.nextDouble() * jiggleSize);
            //normaliseJiggledSlices();
            //System.out.print(g.jiggledSlice + " ");

        }

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
            double leftOverMoney = 0;

            //Record the amount I can get for my money
            //Also record if there isn't enough stock - if my money would buy everything out

            //Randomise order of goods; can affect spending outcome
            goods = RandPermuteArray.mix(goods);

//            System.out.print("Good order: ");
//            for(Good g : goods) {System.out.print(g.id + " ");}

            for (Good g : goods) {

                double unitsIcanBuyAtThisPrice = g.tempBudgetSpend / g.price;
                p.p("units I can buy at this price: " + unitsIcanBuyAtThisPrice);

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

            //p.p("Leftover Money: " + leftOverMoney);
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
        utilityOpt = UtilityShell.giveUtility(goods);

        //p.p("Utility: " + CESopt);

        //check for new best
        if (utilityOpt > maxUtilityOpt) {

            maxUtilityOpt = utilityOpt;

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
        //jiggleSize += ((endJiggleSize - startJiggleSize) / iterations);
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
