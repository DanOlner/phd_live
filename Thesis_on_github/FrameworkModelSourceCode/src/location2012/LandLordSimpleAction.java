package location2012;

import java.awt.geom.Point2D;
import location2012.actiontools.Action;
import location2012.observe.ShoutEvent;
import location2012.utils.Randoms;

/**
 * Simple landlord action: set price in proportion to however many people were looking to buy.
 *
 * @author Dan Olner
 */
public class LandLordSimpleAction extends Action {

    Point2D.Double init, end;
    int ct = 0;
    LandLord me;
    //Number of days to try action before assessing success. Assess against mean.
    int numDays = 3, numDaysCount = 0;
    //for reversing crawls
    double totCrawl = 0;
    //double stock1difference, stock2difference, timeInputDifference;
    double incomeDifference;
    double meanIncomeDifference;
    //Initial diff: the first rate of change found when a crawl is successful
    //For later positive crawls, the size will be adjusted relative to this.
    //If the rate of change increases, the crawl size will too.
    double initialDiff;
    //adjustment rate factor
    //public double startAdjustRate = 0.01, adjustRate = startAdjustRate;
    //counter for making adjustments grow
    double startAdjustStep = 0.0025, adjustStep = startAdjustStep;
    double startCrawlRate = 0.0015, crawlRate = startCrawlRate;
    //for random jiggle method
    boolean repeatMode = true;
    //Need to think up some encapsulation methods...
    int choose = 0;
    Point2D.Double movePoint = new Point2D.Double(0, 0);
    //used only for moving in two-region model
    boolean move = false;
    //For first negative result: if true, reset adjustrate. Otherwise, increase it
    boolean resetAdjustRate = true;
    boolean firstCrawl = true;
    //for jiggling vectors... um.
    Point2D.Double vectorJiggle = new Point2D.Double(0, 0);
    Point2D.Double startVectorJiggle = new Point2D.Double(0, 0);
    //
    double jiggleDir = 0;
    double newDir = 0;
    //if true, randomise starting values for search
    boolean changeMode = true;

    public LandLordSimpleAction(Actor me) {

        //ref to the actor acting this action!
        this.me = (LandLord) me;


    }

  
    /*
     * Randomly jiggle. Which way does revenue change? If positive, stick with that
     * If negative, randomly jiggle and try again.
     * Sort of a hill-climbing algorithm
     */
    public void heard(ShoutEvent s) {

//      //will have new revenue amounts now from last time, so can find out difference since
        //last I took action. "Income - yesterday's" will be positive if it went up from yesterday
        incomeDifference = me.income - me.yesterdayIncome;
        //blech, short-termist duplication... for data



        //just for turning off blind search completely
        if (true) {

            if (changeMode) {
                pickRandomVar();
                ct = 0;
            }

            if (numDaysCount == 0) {
                init = new Point2D.Double(me.xy.x, me.xy.y);
            }

            //if (repeatMode) {

            ct++;
            System.out.println("ct: " + ct);

            changeMode = false;

            //false: not resetting them. Positive change plz.
            setVals(false);
            //keep a record of total crawl so it can be reversed
            //totCrawl += crawlRate;
            //System.out.println("crawlRate: " + crawlRate + ", totCrawl: " + totCrawl);

            meanIncomeDifference += incomeDifference;
            numDaysCount++;

            //if done number of days, turn off changeMode ready for assessing
            if (numDaysCount == numDays) {
                numDaysCount = 0;

                assess();
                //System.out.println("incomediff: " + incomeDifference + ", meanincdiff: " + meanIncomeDifference);
            }

            // }//end if repeatMode

        }//end if true

        //set revenue back to zero and keep a record as 'yesterday's revenue'
        //my job is try to improve it.
        me.yesterdayIncome = me.income;
        me.income = 0;


    }//end heard method

    private void pickRandomVar() {


        //shake things about slightly
        //remember this for next time
        //perturb = (0.5 - Randoms.nextDouble());
        crawlRate = (0.5 - Randoms.nextDouble()) * startCrawlRate;
        if (crawlRate < 0) {
            adjustStep = -adjustStep;
            //System.out.println("adjuststep changed to neg: " + adjustStep);
        }


    }//end changeRandomVars

    //if reset, reset them to before change
    private void setVals(boolean reset) {

        //crawlRate will be reset after this, so OK to use the var here
        if (reset) {
            //System.out.println("totcrawl: " + totCrawl +", minus totcrawl: " + (-totCrawl));
            //crawlRate = -totCrawl;
            crawlRate = -crawlRate;
            crawlRate *= numDays;
        }


        if (me.getGoodCost() + (crawlRate) > 0) {
            me.setGoodCost(me.getGoodCost() + crawlRate);
        }
        //  break;



    }//end method setVals

    private void assess() {

        meanIncomeDifference /= numDays;
//
        //did average income go up for the period I was changing values?
        if (meanIncomeDifference > 0) {
            //System.out.println("meanincomediff > 0");
            //leaving crawlRate unchanged for now
            if (firstCrawl) {
                initialDiff = meanIncomeDifference;
                //System.out.println("Initialdiff found: " + initialDiff);
                firstCrawl = false;
                //Otherwise, need to adjust crawlsize in proportion to the initial difference
                //if the latest income difference is bigger, increase.
                //jiggle Direction slightly

            } else {

                //crawlRate = (meanIncomeDifference > initialDiff ? crawlRate + adjustStep : crawlRate);
                //crawlRate = (incomeDifference > initialDiff ? crawlRate + adjustStep : crawlRate - adjustStep);
                crawlRate += adjustStep;

                // System.out.print("After 1st crawl: " + meanIncomeDifference);
//                if (meanIncomeDifference > initialDiff) {
//                   // System.out.println("Crawlrate changed to: " + crawlRate);
//                } else {
//                    System.out.println("No change to crawlrate");
//                }

            }


        } else {

            //reverse value change, try again
            setVals(true);
            changeMode = true;
            firstCrawl = true;
            crawlRate = startCrawlRate;
            adjustStep = startAdjustStep;

        }

        //whatever happens here, we'll be in repeat mode again next turn
        //repeatMode = true;
        //reset mean income difference
        meanIncomeDifference = 0;

    }

 
}
