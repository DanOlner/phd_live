package location2012;

import java.awt.geom.Point2D;
import location2012.actiontools.Action;
import location2012.observe.ShoutEvent;
import location2012.utils.Randoms;
import location2012.utils.Trig;
import location2012.utils.gl;

/**
 * Testing an Action...
 *
 * @author Dan Olner
 */
public class FirmActRespondAvMCMR extends Action {

    Point2D.Double init, end;
    int ct = 0;
    Firm me;
    //Number of days to try action before assessing success. Assess against mean.
    int numDays = 1, numDaysCount = 0;
    //for reversing crawls
    double totCrawl = 0;
    //double stock1difference, stock2difference, timeInputDifference;
    double revenueDifference, wageDifference, goodDifference;
    double costDifference;
    double meanIncomeDifference;
    //Initial diff: the first rate of change found when a crawl is successful
    //For later positive crawls, the size will be adjusted relative to this.
    //If the rate of change increases, the crawl size will too.
    double initialDiff;
    //adjustment rate factor
    //public double startAdjustRate = 0.01, adjustRate = startAdjustRate;
    //counter for making adjustments grow
    double startAdjustStep = 0.0025, adjustStep = startAdjustStep;
    double startCrawlRate = 0.005, crawlRate = startCrawlRate;
    //record of random direction chosen
    double randir;
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

    public FirmActRespondAvMCMR(Actor me) {

        //ref to the actor acting this action!
        this.me = (Firm) me;


    }

//    public void heard(ShoutEvent s) {
//
//        //with a given probability, choose a completely random location
//        if(Randoms.nextDouble()<0.2) {
//            randomLeap();
//        } else {
//            jiggle();
//        }
//
//    }//end heard method
//
//    private void randomLeap() {
//
//    }
    public void heard(ShoutEvent s) {

        me.revenueDiff = me.revenue - me.yesterdayRevenue;
        me.costDiff = me.costs - me.yesterdayCosts;

        //blech, short-termist duplication... for data
        me.dailyIncomeBalance = me.revenue - me.costs;

//        costDifference = me.costs - me.yesterdayCosts;
//
//        totIncomeDifference = revenueDifference - costDifference;

//        System.out.println("In firmactAVMCMR heard: revenue diff = " + revenueDifference + ", costdiff = " + costDifference);

        if (gl.peopleFixWage > 0) {
            choose = 1;
        } else {
            choose = Randoms.nextInt(2);
        }
        //If MC > MR, reduce wages or increase prices
        if (me.revenueDiff > 0) {
        //if (costDifference > revenueDifference) {


            switch (choose) {

                case 0:
                    if (me.wage - (crawlRate) > 0) {
                        me.wage -= (crawlRate);
                    }
                    break;
                case 1:
                    if (me.goodCost + (crawlRate) > 0) {
                        me.goodCost += (crawlRate);
                    }

            }//end switch

        } else {
//        } else if (costDifference < revenueDifference) {

            switch (choose) {

                case 0:
                    if (me.wage + (crawlRate) > 0) {
                        me.wage += (crawlRate);
                    }
                    break;
                case 1:
                    if (me.goodCost - (crawlRate) > 0) {
                        me.goodCost -= (crawlRate);
                    }

            }

        }

        //this is where MC=MR. Do nothing.
//        } else {
//            //hello! Nothing happening here. Just thought it worth putting in in case
//            //you wondered why the double test above.
//        }

        //reset vars
        //set revenue back to zero and keep a record as 'yesterday's revenue'
        //my job is try to improve it.
        me.yesterdayRevenue = me.revenue;
        me.yesterdayCosts = me.costs;
        me.revenue = 0;
        me.costs = 0;

        me.hiredLabour = false;
        me.goodBought = 0;

    }


    /*
     * Randomly jiggle. Which way does revenue change? If positive, stick with that
     * If negative, randomly jiggle and try again.
     * Sort of a hill-climbing algorithm
     */
    public void sheard(ShoutEvent s) {

//      //will have new revenue amounts now from last time, so can find out difference since
        //last I took action. "Income - yesterday's" will be positive if it went up from yesterday
        revenueDifference = me.revenue - me.yesterdayRevenue;

        //blech, short-termist duplication... for data
        me.dailyIncomeBalance = revenueDifference;



        //just for turning off blind search completely
        if (true) {

            if (changeMode) {
                pickRandomVars();
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

            meanIncomeDifference += revenueDifference;
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
        me.yesterdayRevenue = me.revenue;
        me.revenue = 0;

        me.hiredLabour = false;
        me.goodBought = 0;

    }//end heard method

    private void pickRandomVars() {

        //income didn't change. Try random shit!
        //p.s. blech, gotta be a better way of doing this...
        if (gl.space == gl.SpaceType.Point) {
            choose = Randoms.nextInt(2);
            //for 1D or 2D space, we need to randomly choose to change space too
        } else if (gl.space == gl.SpaceType.Torus || gl.space == gl.SpaceType.Line
                || gl.space == gl.SpaceType.TwoRegion) {
            //choose = Randoms.nextInt(3);
            //fix random int to two options, move and goods:
            choose = Randoms.nextInt(2) + 1;
            //fix to wage and goods
            //choose = Randoms.nextInt(2)+2;
            //System.out.println("choose: " + choose);
        }

        //override
        //3: special stock changer for monopolists. Not a findable option for the randomiser above
        choose = 2;
        //choose = 3;

////                with a given probability, choose option 4: the random leap into the unknown
//                if (Randoms.nextDouble() < 0.1) {
//                    choose = 4;
//                }

        //shake things about slightly
        //remember this for next time
        //perturb = (0.5 - Randoms.nextDouble());
        crawlRate = (0.5 - Randoms.nextDouble()) * startCrawlRate;
        if (crawlRate < 0) {
            adjustStep = -adjustStep;
            //System.out.println("adjuststep changed to neg: " + adjustStep);
        }
        //Make a new random point
        //keep it for next time...
        //randp = Trig.getRandPointInCircleOfRadius(perturb * 10);
        if (gl.space == gl.SpaceType.Torus) {

            //random direction in the range of 2*PI radians
            //useful to keep randir for changing movePoint later
            randir = Randoms.nextDouble() * 2 * Math.PI;
            //System.out.println("movePoint coords: " + movePoint.x + "," + movePoint.y);
        } else if (gl.space == gl.SpaceType.Line) {
            //movePoint = new Point2D.Double(crawlRate, 0);
        } else if (gl.space == gl.SpaceType.TwoRegion) {
            //for this one, firms only need to try randomly shifting between
            //the two regions.
            move = (Randoms.nextDouble() > 0.5) ? true : false;

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

        switch (choose) {

            case 0:
                if (me.wage + (crawlRate) > 0) {
                    me.wage += (crawlRate);
                }
                break;
            case 1:
                if (me.goodCost + (crawlRate) > 0) {
                    me.goodCost += (crawlRate);
                }
                break;

            case 2:
                //Only for space currently. Use perturb for distance
                //use randp to store a random direction

                //move me there
                //If in two-region:
                if (gl.space == gl.SpaceType.TwoRegion) {

                    if (move) {
                        if (me.xy.x == gl.TwoR_Region0x) {
                            me.xy.x = gl.TwoR_Region1x;
                        } else {
                            me.xy.x = gl.TwoR_Region0x;
                        }
                    }//end if move

                } else if (gl.space == gl.SpaceType.Line) {

                    movePoint = new Point2D.Double(crawlRate, 0);
                    me.xy.x += (movePoint.x);
                    me.xy.y += (movePoint.y);
                    me.xy = me.space.checkInTorus(me.xy);

                } else if (gl.space == gl.SpaceType.Torus) {

                    //random direction in the range of 2*PI radians
                    //useful to keep randir for changing movePoint later
                    movePoint = Trig.getVecFromDistPlusAngle(crawlRate, randir);
                    me.xy.x += (movePoint.x);
                    me.xy.y += (movePoint.y);
                    me.xy = me.space.checkInTorus(me.xy);
                    //System.out.println("movePoint coords: " + movePoint.x + "," + movePoint.y);
                }

                break;

            //special switch for monopolists
            case 3:

                if (me.stock + (crawlRate) > 0) {
                    me.stock += (crawlRate);
                }
                break;

            //random leap
            case 4:

                //use movePoint as record of old position
                movePoint.x = me.xy.x;
                movePoint.y = me.xy.y;
                me.xy.x = Randoms.nextDouble() * gl.width;
                if (gl.space == gl.SpaceType.Torus) {
                    me.xy.y = Randoms.nextDouble() * gl.width;
                }

        }//end switch



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
