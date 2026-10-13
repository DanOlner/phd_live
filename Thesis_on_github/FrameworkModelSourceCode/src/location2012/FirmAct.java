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
public class FirmAct extends Action {

    Firm me;
    ActorNLocation a;
    //double stock1difference, stock2difference, timeInputDifference;
    double revenueDifference, wageDifference, goodDifference;
    //adjustment rate factor
    public double adjustRate = 0.01;
    //counter for making adjustments grow
    double adjustStep = 0;
    double chooseAction, perturb;
    //for random jiggle method
    boolean jiggleMode = true;
    //Need to think up some encapsulation methods...
    int choose = 0;
    Point2D.Double randp = new Point2D.Double(0, 0);
    //used only for moving in two-region model
    boolean move = false;
    //Global value for hill-climb step distance
    double step = 0.001;

    public FirmAct(Actor me) {

        //ref to the actor acting this action!
        this.me = (Firm) me;


    }

    /*
     * Randomly jiggle. Which way does revenue change? If positive, stick with that
     * If negative, randomly jiggle and try again.
     * Sort of a hill-climbing algorithm
     */
    public void heard(ShoutEvent s) {

        System.out.println("Ping!");

        //do some data things. First - have I shifted anything since yesterday?
        //If not, I don't want to include this Firm's price in the price averages
        //Note: hire Labour check is done by setting hiredLabour to false at
        //the bottom of this method, and to true in Firm when a person accepts work
        goodDifference = me.stock - me.yesterdayStock;

        me.shiftedStock = (goodDifference == 0) ? false : true;

        //System.out.println("1: " + price1difference + ", 2: " + price2difference);

        //try to maximise revenue through random groping!
        chooseAction = Randoms.nextDouble();

        //if more money than yesterday, this is positive
        revenueDifference = me.money - me.yesterdayMoney;


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

            }//end switch

            //turn off jiggleMode for now
            jiggleMode = false;

        }//end if jiggleMode
        //jiggleMode off!
        else {

            //was the jiggle successful in improving revenue?
            if (revenueDifference > 0) {
                //if so, repeat the jiggle action

                //also reset adjustRate
                adjustRate = 0.01;
                adjustStep = 0;

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

                }//end switch


            }//end if revenue check
            //nope, revenue didn't improve...
            else {

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

                }//end switch

                //increase adjustRate
                adjustRate += Math.exp(adjustStep) - 1;
                adjustStep += 0.005;
                //System.out.println("increasing adjustRate: " + adjustRate);

                jiggleMode = true;

            }

        }

        //today will be yesterday tomorrow!
        me.yesterdayMoney = me.money;
        me.yesterdayStock = me.stock;

        //me.hiredLabour = false;
        me.goodBought = 0;


    }

   
}
