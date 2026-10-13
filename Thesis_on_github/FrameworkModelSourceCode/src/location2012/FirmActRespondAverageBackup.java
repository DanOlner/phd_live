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
public class FirmActRespondAverageBackup extends Action {

    Point2D.Double init, end;
    int ct = 0;
    Firm me;
    //Number of days to try action before assessing success. Assess against mean.
    int numDays = 1, numDaysCount = 0;
    //for reversing crawls
    double totCrawl = 0;
    //double stock1difference, stock2difference, timeInputDifference;
    double incomeDifference, wageDifference, goodDifference;
    double meanIncomeDifference;
    double costDifference;
    //Initial diff: the first rate of change found when a crawl is successful
    //For later positive crawls, the size will be adjusted relative to this.
    //If the rate of change increases, the crawl size will too.
    double initialDiff;
    //adjustment rate factor
    //public double startAdjustRate = 0.01, adjustRate = startAdjustRate;
    //counter for making adjustments grow
    double startAdjustStep = 0.0025, adjustStep = startAdjustStep;
    double startCrawlRate = 0.025, crawlRate = startCrawlRate;
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

    public FirmActRespondAverageBackup(Actor me) {

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
    /*
     * Randomly jiggle. Which way does revenue change? If positive, stick with that
     * If negative, randomly jiggle and try again.
     * Sort of a hill-climbing algorithm
     */
    public void heard(ShoutEvent s) {

//        System.out.println("Firm's action");

//      //will have new revenue amounts now from last time, so can find out difference since
        //last I took action. "Income - yesterday's" will be positive if it went up from yesterday
        incomeDifference = me.revenue - me.yesterdayRevenue;
        //blech, short-termist duplication... for data
        me.dailyIncomeBalance = incomeDifference;

        //costDifference = me.costs - me.yesterdayCosts;
//        System.out.println("today's revenue: " + me.revenue + ", yesterday: " + me.yesterdayRevenue);



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
//            System.out.println("ct: " + ct);

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
//            choose = Randoms.nextInt(2) + 1;
            choose = Randoms.nextInt(2);
            //fix to wage and goods
            //choose = Randoms.nextInt(2)+2;
            //System.out.println("choose: " + choose);
        }

        //override
        //3: special stock changer for monopolists. Not a findable option for the randomiser above
//        choose = 2;
//        if(choose == 1) {
//            choose = 2;
//        }

        choose = 2;

//                with a given probability, choose option 4: the random leap into the unknown
//                if (Randoms.nextDouble() < 0.1) {
//                    choose = 4;
//                }


        //shake things about slightly
        //remember this for next time
        //perturb = (0.5 - Randoms.nextDouble());
//        crawlRate = startCrawlRate * 0.5;
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
                if (gl.peopleFixWage == 0) {
                    if (me.wage + (crawlRate) > 0) {
                        me.wage += (crawlRate);
                    }
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
                if (gl.firmsMobile) {
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

    public void sheard(ShoutEvent s) {



        //System.out.println("Revenue difference: " + incomeDifference);

        if (true) {

            if (repeatMode) {

                //if first new attempt
                if (numDaysCount == 0) {
                }//end if numdayscount == 0

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

                        } else {

                            me.xy.x += (movePoint.x);
                            me.xy.y += (movePoint.y);
                            me.xy = me.space.checkInTorus(me.xy);

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

                //if done number of days, turn off changeMode ready for assessing
                if (numDaysCount == numDays) {
                    repeatMode = false;
                }

                meanIncomeDifference += incomeDifference;
                numDaysCount++;

            }//end if jiggleMode
            //jiggleMode off!
            else {

                //was the jiggle successful in improving revenue?
                if (incomeDifference > 0) {

                    //Not sure if I should be reseting crawlRate each time. Possibly yes:
                    //if the ratio (incomeDiff/initialDiff) gets bigger, I want crawl rate to get bigger relative to that, not to itself. I think.

                    //If this is a first positive crawl, record the initial magnitude
                    //of the positive difference / rate of change. Later positive crawls
                    //will be adjusted relative to it, looking for gradients
                    if (firstCrawl) {
                        initialDiff = incomeDifference;
                        //System.out.println("Initialdiff found: " + initialDiff);
                        firstCrawl = false;
                        //Otherwise, need to adjust crawlsize in proportion to the initial difference
                        //if the latest income difference is bigger, increase.
                        //jiggle Direction slightly

                    } else {
                        crawlRate = (incomeDifference > initialDiff ? crawlRate + adjustStep : crawlRate);
                        //crawlRate = (incomeDifference > initialDiff ? crawlRate + adjustStep : crawlRate - adjustStep);

                        System.out.println("Crawlrate: " + crawlRate);
                        //jiggleDir = (0.5 - Randoms.nextDouble()) * (2 * Math.PI);
                        //newDir = randir + (jiggleDir * crawlRate / 2);
                        //newDir = randir + (jiggleDir);
                        //System.out.println("Jiggledir: " + jiggleDir + ", jigglDir * crawlRate: " + (jiggleDir * crawlRate * 5));

                        //change adjustStep size in proportion to ratio of difference, if the difference is more than one
//                        if ((incomeDifference / initialDiff) > 1) {
//                            adjustStep += startAdjustStep;
//                            System.out.println("changed adjuststep: " + adjustStep);
//                        }


                        //System.out.println("changing crawl rate. Ratio: " + (incomeDifference / initialDiff) + ", crawlRate: " + crawlRate + ", adjustStep: " + adjustStep);
                    }

                    //if this is a physical space vector, I need to update that vector.
                    if (choose == 2) {
                        //movePoint = Trig.getVecFromDistPlusAngle(crawlRate, newDir);
                        movePoint = Trig.getVecFromDistPlusAngle(crawlRate, randir);
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
                            //Only for space currently. Use peturb for distance
                            //use randp to store a random direction

                            //move me there
                            if (gl.space == gl.SpaceType.TwoRegion) {
                                //if in two-region, just stay put if it made things better
                            } else {

                                me.xy.x += (movePoint.x);
                                me.xy.y += (movePoint.y);
                                me.xy = me.space.checkInTorus(me.xy);

                            }

                            break;

                        //special switch for monopolists
                        case 3:

                            if (me.stock + (crawlRate) > 0) {
                                me.stock += (crawlRate);
                            }

                            break;

                        //successful leap - just go back to jiggle again, but stay where I went
                        case 4:
                            firstCrawl = true;
                            crawlRate = startCrawlRate;
                            adjustStep = startAdjustStep;
                            repeatMode = true;

                    }//end switch


                }//end of revenue check
                //nope, revenue didn't improve...
                else {

                    //if it's the first negative result, reset adjust rate
//                    if (resetAdjustRate) {
//                        adjustRate = startAdjustRate;
//                        adjustStep = 0;
//                        resetAdjustRate = false;
//                    }

                    //reverse last step and revert to randomness
                    switch (choose) {

                        case 0:
                            if (me.wage - (crawlRate) > 0) {
                                me.wage -= (crawlRate);
                            }
                            break;
                        case 1:
                            if (me.goodCost - (crawlRate) > 0) {
                                me.goodCost -= (crawlRate);
                            }
                            break;

                        case 2:
                            //Only for space currently. Use peturb for distance
                            //use randp to store a random direction

                            //move me there

                            if (gl.space == gl.SpaceType.TwoRegion) {
                                //if in two region... only move back if I moved.
                                //Don't move if I stayed put.
                                if (move) {

                                    if (me.xy.x == gl.TwoR_Region0x) {
                                        me.xy.x = gl.TwoR_Region1x;
                                    } else {
                                        me.xy.x = gl.TwoR_Region0x;
                                    }

                                    move = false;

                                }//end if move

                            } else {

                                me.xy.x -= (movePoint.x);
                                me.xy.y -= (movePoint.y);
                                me.xy = me.space.checkInTorus(me.xy);
                            }

                            break;


                        //special switch for monopolists
                        case 3:

                            if (me.stock - (crawlRate) > 0) {
                                me.stock -= (crawlRate);
                            }

                            break;

                        //random leap failed - go back to old spot
                        //used movePoint as record of old spot
                        case 4:

                            me.xy.x = movePoint.x;
                            me.xy.y = movePoint.y;

                    }//end switch

                    //increase adjustRate
                    //adjustRate += 0.01;
                    //adjustRate += Math.exp(adjustStep) - 1;
                    //adjustStep += 0.02;
                    //System.out.println("increasing adjustRate: " + adjustRate);

                    firstCrawl = true;
                    crawlRate = startCrawlRate;
                    adjustStep = startAdjustStep;
                    repeatMode = true;

                }

            }

        }//end if false

        //randomise good cost
        //me.goodCost = (Randoms.nextDouble()*4)+1;

        //today will be yesterday tomorrow!
        //me.yesterdayMoney = me.money;
        //me.yesterdayStock = me.goodStock;

        //set revenue back to zero and keep a record as 'yesterday's revenue'
        //my job is try to improve it.
        me.yesterdayRevenue = me.revenue;
        me.revenue = 0;

        me.hiredLabour = false;
        me.goodBought = 0;


    }
}
