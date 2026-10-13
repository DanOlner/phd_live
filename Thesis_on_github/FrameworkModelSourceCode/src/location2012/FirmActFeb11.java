package location2012;

import java.awt.geom.Point2D;
import location2012.actiontools.Action;
import location2012.geog.LocMemory.ActorNLocation;
import location2012.observe.ShoutEvent;
import location2012.utils.Randoms;
import location2012.utils.Trig;
import location2012.utils.gl;

/**
 * Testing an Action...
 *
 * @author Dan Olner
 */
public class FirmActFeb11 extends Action {

    Firm me;
    ActorNLocation a;
    //double stock1difference, stock2difference, timeInputDifference;
    double incomeDifference, wageDifference, goodDifference;
    //adjustment rate factor
    public double startAdjustRate = 0.05, adjustRate = startAdjustRate;
    //counter for making adjustments grow
    double adjustStep = 0;
    double perturb;
    //for random jiggle method
    boolean jiggleMode = true;
    //Need to think up some encapsulation methods...
    int choose = 0;
    Point2D.Double randp = new Point2D.Double(0, 0);
    //used only for moving in two-region model
    boolean move = false;
    //Global value for hill-climb step distance
    double step = 0.001;
    //For first negative result: if true, reset adjustrate. Otherwise, increase it
    boolean resetAdjustRate = true;

    public FirmActFeb11(Actor me) {

        //ref to the actor acting this action!
        this.me = (Firm) me;


    }

    /*
     * Randomly jiggle. Which way does revenue change? If positive, stick with that
     * If negative, randomly jiggle and try again.
     * Sort of a hill-climbing algorithm
     */
    public void heard(ShoutEvent s) {

        //will have new revenue amounts now from last time, so can find out difference since
        //last I took action. "Income - yesterday's" will be positive if it went up from yesterday
        incomeDifference = me.revenue - me.yesterdayRevenue;
        //blech, short-termist duplication... for data
        me.dailyIncomeBalance = incomeDifference;

        //System.out.println("Revenue difference: " + incomeDifference);

        //just for turning off blind search completely
        if (true) {

            if (jiggleMode) {

                //income didn't change. Try random shit!
                //p.s. blech, gotta be a better way of doing this...
                if (gl.space == gl.SpaceType.Point) {
                    choose = Randoms.nextInt(2);
                    //for 1D or 2D space, we need to randomly choose to change space too
                } else if (gl.space == gl.SpaceType.Torus || gl.space == gl.SpaceType.Line
                        || gl.space == gl.SpaceType.TwoRegion) {
                    choose = Randoms.nextInt(3);
                }

                //override
                //3: special stock changer for monopolists. Not a findable option for the randomiser above
                //choose = 3;
                choose = 2;

                //shake things about slightly
                //remember this for next time
                //perturb = (0.5 - Randoms.nextDouble()) * adjustRate;
                perturb = (0.5 - Randoms.nextDouble());
                //Make a new random point
                //keep it for next time...
                //randp = Trig.getRandPointInCircleOfRadius(perturb * adjustRate * 10);
                if (gl.space == gl.SpaceType.Torus) {
                    randp = Trig.getRandPointInCircleOfRadius(perturb);
                } else if (gl.space == gl.SpaceType.Line) {
                    randp = new Point2D.Double(perturb, 0);
                } else if (gl.space == gl.SpaceType.TwoRegion) {
                    //for this one, firms only need to try randomly shifting between
                    //the two regions.
                    move = (Randoms.nextDouble() > 0.5) ? true : false;

                }

                switch (choose) {

                    case 0:
                        if (me.wage + (perturb * adjustRate) > 0) {
                            me.wage += (perturb * adjustRate);
                        }
                        break;
                    case 1:
                        if (me.goodCost + (perturb * adjustRate) > 0) {
                            me.goodCost += (perturb * adjustRate);
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

                            me.xy.x += (randp.x * adjustRate);
                            me.xy.y += (randp.y * adjustRate);
                            me.xy = me.space.checkInTorus(me.xy);

                        }

                        break;

                    //special switch for monopolists
                    case 3:

                        if (me.stock + (perturb * adjustRate) > 0) {
                            me.stock += (perturb * adjustRate);
                        }
                        break;


                }//end switch

                //turn off jiggleMode for now
                jiggleMode = false;

            }//end if jiggleMode
            //jiggleMode off!
            else {

                //was the jiggle successful in improving revenue?
                if (incomeDifference > 0) {
                    //if so, repeat the jiggle action
                    //set resetAdjustRate back to true, so that the next negative result
                    //initially resets it
                    resetAdjustRate = true;

                    //also reset adjustRate
//                    adjustRate = startAdjustRate;
//                    adjustStep = 0;

                    switch (choose) {

                        case 0:
                            if (me.wage + (perturb * adjustRate) > 0) {
                                me.wage += (perturb * adjustRate);
                            }
                            break;
                        case 1:
                            if (me.goodCost + (perturb * adjustRate) > 0) {
                                me.goodCost += (perturb * adjustRate);
                            }
                            break;

                        case 2:
                            //Only for space currently. Use peturb for distance
                            //use randp to store a random direction

                            //move me there
                            if (gl.space == gl.SpaceType.TwoRegion) {
                                //if in two-region, just stay put if it made things better
                            } else {

                                me.xy.x += (randp.x * adjustRate);
                                me.xy.y += (randp.y * adjustRate);
                                me.xy = me.space.checkInTorus(me.xy);

                            }

                            break;

                        //special switch for monopolists
                        case 3:

                            if (me.stock + (perturb * adjustRate) > 0) {
                                me.stock += (perturb * adjustRate);
                            }

                    }//end switch


                }//end of revenue check
                //nope, revenue didn't improve...
                else {

                    //if it's the first negative result, reset adjust rate
                    if (resetAdjustRate) {
                        adjustRate = startAdjustRate;
                        adjustStep = 0;
                        resetAdjustRate = false;
                    }

                    //reverse last step and revert to randomness
                    switch (choose) {

                        case 0:
                            if (me.wage - (perturb * adjustRate) > 0) {
                                me.wage -= (perturb * adjustRate);
                            }
                            break;
                        case 1:
                            if (me.goodCost - (perturb * adjustRate) > 0) {
                                me.goodCost -= (perturb * adjustRate);
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

                                me.xy.x -= (randp.x * adjustRate);
                                me.xy.y -= (randp.y * adjustRate);
                                me.xy = me.space.checkInTorus(me.xy);
                            }

                            break;


                        //special switch for monopolists
                        case 3:

                            if (me.stock - (perturb * adjustRate) > 0) {
                                me.stock -= (perturb * adjustRate);
                            }


                    }//end switch

                    //increase adjustRate
                    adjustRate += 0.01;
                    //adjustRate += Math.exp(adjustStep) - 1;
                    //adjustStep += 0.02;
                    //System.out.println("increasing adjustRate: " + adjustRate);

                    jiggleMode = true;

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
