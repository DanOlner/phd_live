package location2012;

import java.awt.geom.Point2D;
import location2012.econs.Bundle;
import location2012.econs.Good;
import location2012.econs.OptimiseHillClimb;
import location2012.econs.UtilityShell;
import location2012.geog.HasLocation;
import location2012.geog.LocMemory;
import location2012.geog.LocMemory.ActorNLocation;
import location2012.geog.SpaceTools;
import location2012.observe.ShoutEvent;
import location2012.utils.gl;

/**
 * For single-actor-model
 *
 * @author Dan Olner
 */
public class SingleActorAction extends PersonAction {

    SingleActorPerson sap;
    //singe actor-specific vars
//    public Bundle bestBundle = new Bundle();
    public SingleActorPerson me;
    //utility-max feedback vars
    private double lastMaxUtility = 0;

    public SingleActorAction(Actor me) {

        super(me);

        this.me = (SingleActorPerson) me;


    }

    @Override
    public void heard(ShoutEvent s) {
//        System.out.println("ID: " + me.ID);

        initialBundles.clear();
        bundlesToOptimise.clear();
//
//        if (me.stock < -100) {
//            System.out.println("stock: " + me.stock);
//            me.stock = 0;
//        }

        if (gl.setStockToZero) {
            me.stock = 0;
        }

        //bestBundle = new Bundle();

        //use bestBundle to remove time from other actors
        bestBundle.removeTimeFromOthers();

        bestBundle = new Bundle();

        Bundle.addToBundles(initialBundles, 0, 0.2, 1);


        //test - only difference being, I need to remove bundle, not add
        for (Bundle b : initialBundles) {

            me.lm = me.getSpace().whosInRangeOfArray(gl.goodSellers, (HasLocation) me,
                    SpaceTools.convertRelativeToAbsolutePoint(b.here, me.getPoint()), !gl.excludeMe, 0.2);
////            
//            me.lm = me.getSpace().firmsInRange(me, notherFeckingPoint, 1);
//
            getBundlePrices(b);

        }

        //Autobots, optimise!
        for (Bundle bwp : bundlesToOptimise) {

            //Haaa--cckkerrr-eeee
//            gl.writePog = (me.ID == gl.LOOKATME ? true : false);
            //pass bundle to the hillclimber to find the best mix of goods

            if (gl.optimiser == gl.Optimiser.Greedy) {
                OptimiseHillClimb.optimise(bwp);
            } else if (gl.optimiser == gl.Optimiser.Constrained) {
                UtilityShell.giveUtility(bwp);
//                System.out.println("In constrained optimisation");
            }

            //System.out.print(bwp.maxUtility + " ");
            //draw the result if it's me we're drawing

            if (bwp.maxUtility > bestBundle.maxUtility) {
//                System.out.println("New best utility found in bundle: " + bwp.maxUtility);
                bestBundle = bwp;
                chosenDensityCost = bwp.adjustedDensityCost;
                //System.out.println("new max: " + bestBundle.maxUtility);
                //keep a record of the optimal data for the POG
                OptimiseHillClimb.setOptimalPog();
//                drawPog = true;

            }
        }//end for Bundles

        bestBundle.buyGoods();

        //sanity check: "cost" from bundle should add up to one.
        //price in bundle includes distance cost
        Actor a;
//        double totSpend = 0;
//        double spendOnMyself = 0;
//        for (Good g : bestBundle.GoodsList) {
//            totSpend += (g.optimalChosenAmount * g.price);
//            a = (Actor) g.gs;
//            if (a.ID == me.ID) {
//                spendOnMyself = g.optimalChosenAmount * g.price;
//            }
//        }
//
//        System.out.println("Total spend: " + totSpend
//                + ", spend on myself: " + spendOnMyself);

        //picking out the amount of my own time I put into me 
        //(if I was able to buy from myself...)
        for (Good g : bestBundle.GoodsList) {
            a = (Actor) g.gs;
            if (a.ID == me.ID) {
//                System.out.println("amount of time I spent on me = "
//                        + (g.optimalChosenAmount * g.price));
                me.timeContributedToMyself = g.optimalChosenAmount * g.price;
            }
        }

        //For consistent data. 
        me.maxUtility = bestBundle.maxUtility;

        //and don't forget to actually move
        //System.out.println("BEFORE::: My location: " + me.xy.x + "," + me.xy.y + ", Best Bundle location: " + bestBundle.here.x + "," + bestBundle.here.y);
        if (gl.peopleMobile) {
            me.moveTo(bestBundle.here);
        }

        //Utility feedback tests
        //If not just stock target, but aiming to squeeze markup out..
        if (!gl.stockTarget) {

            //if last best utility was higher than now, utility's gone down
            //and I'm going to blame my own price-setting
            if (lastMaxUtility > me.maxUtility) {
                System.out.println("goin down by: " + ((lastMaxUtility - me.maxUtility) / 50));
//                me.goodCost -= ((lastMaxUtility - me.maxUtility) / 1);
//                me.myGoodCost -= ((lastMaxUtility - me.maxUtility) / 1);
//                me.myGoodCost -= 0.05;
                me.goodCost -= 0.05;
            } else {
                System.out.println("goin up by: " + ((me.maxUtility - lastMaxUtility) / 50));
//                me.goodCost += ((me.maxUtility - lastMaxUtility) / 1);
//                me.myGoodCost += ((me.maxUtility - lastMaxUtility) / 1);
//                me.myGoodCost += 0.05;
                me.goodCost += 0.05;
            }


            lastMaxUtility = me.maxUtility;

        }



    }//end heard

    /**
     * set prices for this bundle to give to others
     *
     * @param b
     */
    public void getBundlePrices(Bundle b) {

        //For single Actor, bestWage is their day's time.
        b.bestWage = 1;

//        System.out.println("In setPrices: size of found actors: " + me.lm.actors.size());

        //jiggle em
//        me.lm.actors = RandPermuteArray.mix(me.lm.actors);


        //For each of the actors in my list... 
        for (ActorNLocation anl : me.lm.actors) {

            sap = (SingleActorPerson) anl.actor;

            //Distance between proposed location to firm.
            //All should be relative to zero...
            distance = (zp.distance(anl.p));

            //get prices for all goods. Check if it's me, to make sure no space cost is added
            //(because I might be looking at a bundle on a distant spot and would add my distant self: wrong)
            if (anl.actor.ID == me.ID) {

//                System.out.println("Found me!");


                //if just doing basic stocklevel targetting, I treat my own good like any other.
//                if (gl.stockTarget) {
//                    price = sap.getGoodCost();
//                } else {
                //otherwise I'm going to use my own price to see if I can get a markup
                //If I can change lower for my own good, I can be better off
                price = me.goodCost;
//                }

            } else {
                //standard space price
                price = sap.getGoodCost() + (sap.getDeliveryCost() * distance);
                //hard limit space price
//                price = sap.getGoodCost()/(1-(distance/gl.TECH));
            }
            //System.out.println("total delivery cost calculated: " + price);
            //1 = index of the good
            b.addGood(sap, price);

        }//end for actornlocation


        if (gl.DENSITYCOST > 0) {

            //Use density cost finding method in SpaceTools
            pt = new Point2D.Double(me.xy.x + b.here.x, me.xy.y + b.here.y);
            LocMemory dlm = me.getSpace().whosInRange(me, pt, gl.DENSITYCOSTRADIUS);
//                System.out.print("Density cost. Points found: " + dlm.actors.size());

//                System.out.println(", density cost: " + SpaceTools.findDensityCost(dlm));

//                System.out.print("Wage before: " + b.bestWage + ", ");
//                densityCost = gl.DENSITYCOST * SpaceTools.findDensityCost(dlm) * bwp.bestWage;
            adjustedDensityCost = gl.DENSITYCOST * SpaceTools.findDensityCost(dlm);
//                dlm = null;
            b.bestWage -= adjustedDensityCost;
//                System.out.println("wage after: " + b.bestWage);
            //store for picking whichever we ended up with
            b.adjustedDensityCost = adjustedDensityCost;

        }


        //Add the bwp to the array
        if (b.bestWage > 0) {
            bundlesToOptimise.add(b);
        }

    }
}
//Cuttinz

