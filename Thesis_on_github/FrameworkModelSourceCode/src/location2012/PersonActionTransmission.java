package location2012;

import java.awt.geom.Point2D;
import location2012.actiontools.RandPermuteArray;
import location2012.econs.Bundle;
import location2012.econs.Good;
import location2012.econs.GoodSeller;
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
public class PersonActionTransmission extends PersonAction {

//    Person sap;
    GoodSeller sap;
    //singe actor-specific vars
//    public Bundle bestBundle = new Bundle();
    public Person me;
    //utility-max feedback vars
    private double lastMaxUtility = 0;

    public PersonActionTransmission(Actor me) {

        super(me);

        this.me = (Person) me;


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

        //bestBundle = new Bundle();

        //use bestBundle to remove time from other actors
        bestBundle.removeTimeFromOthers();

        bestBundle = new Bundle();

        Bundle.addToBundles(initialBundles, 3, 0.2, 5);


        //test - only difference being, I need to remove bundle, not add
        for (Bundle b : initialBundles) {

            me.lm = me.getSpace().whosInRangeOfArray(gl.goodSellers, (HasLocation) me,
                    SpaceTools.convertRelativeToAbsolutePoint(b.here, me.getPoint()), false, 0.2);

            //checking relative locations
//            Firm f;
//            System.out.println("Bundle " + bundleNum++);
//
//            for (ActorNLocation anl : me.lm.actors) {
//
//                f = (Firm) anl.actor;
//
//                System.out.println(me.ID + ", " + anl.actor.ID + ": "
//                        + f.currentContributedTime + ", " + anl.p.x + "," + anl.p.y);
//
//            }

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
                //hack
                me.chosenDensityCost = chosenDensityCost;
                //System.out.println("new max: " + bestBundle.maxUtility);
                //keep a record of the optimal data for the POG
                OptimiseHillClimb.setOptimalPog();
//                drawPog = true;

            }
        }//end for Bundles

        //check what happened
//        System.out.println("Optimised bundles:");
//        for (Bundle bwp : bundlesToOptimise) {
//
//            System.out.println("Utility: " + bwp.maxUtility + ", rel loc: " + bwp.here.x);
//            //+ ", " + bwp.GoodsList.get(0).gs);
//
//        }

        bestBundle.buyGoods();

        //hack for Hotelling
        Firm t;
        t = (Firm) bestBundle.GoodsList.get(0).gs;

        //people size cos that's when Firm IDs start
        if (gl.firms.size() > 1) {
            if (t.ID == gl.people.size()) {
//            System.out.println("Firm 0 in 0");
                me.firm0demand = bestBundle.GoodsList.get(0).optimalChosenAmount;
                me.firm1demand = bestBundle.GoodsList.get(1).optimalChosenAmount;
            } else {
//            System.out.println("Firm 1 in 0");
                me.firm0demand = bestBundle.GoodsList.get(1).optimalChosenAmount;
                me.firm1demand = bestBundle.GoodsList.get(0).optimalChosenAmount;
            }
        }

//        System.out.println("bestBundle number of suppliers: " + bestBundle.GoodsList.size());
//        Firm f;
//        for (Good g : bestBundle.GoodsList) {
//            f = (Firm) g.gs;
//            System.out.println("PersonActionTransmission. Firm ID: " + f.ID);
//        }
//        
//        System.out.println(" ");

        //data hack to see ratio of time spent in nearest producer
        //for two-region so we know there are only two goods
        //Cheapest will be home region
        //Make home region denominator
        //Amount of time for nearest producer. Other one's just going to be -1. So we don't have to work out distances n stuff. But note, that's different from "how much I'm putting into production."
        //Amount of time for nearest producer: optimal amount * price.
        //If I acquired less, that one's further away. 
        if (gl.firms.size() > 1) {
            if (bestBundle.GoodsList.get(0).optimalChosenAmount < bestBundle.GoodsList.get(1).optimalChosenAmount) {
                me.ratioOfWageSpentOnNearestProducer =
                        (bestBundle.GoodsList.get(1).price * bestBundle.GoodsList.get(1).optimalChosenAmount) / 1;
            } else {
                me.ratioOfWageSpentOnNearestProducer =
                        (bestBundle.GoodsList.get(0).price * bestBundle.GoodsList.get(0).optimalChosenAmount) / 1;
            }
        }


//            me.ratioOfWageSpentOnNearestProducer = 



        //hacking in for drawing
        for (Bundle b : initialBundles) {
            b.myAbsoluteLocation.x = me.getx();
            b.myAbsoluteLocation.y = me.gety();
        }

//        System.out.println("Here!");

        //sanity check: "cost" from bundle should add up to one.
        //price in bundle includes distance cost
//        Actor a;
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
//        for (Good g : bestBundle.GoodsList) {
//            a = (Actor) g.gs;
//            if (a.ID == me.ID) {
////                System.out.println("amount of time I spent on me = "
////                        + (g.optimalChosenAmount * g.price));
//                me.timeContributedToMyself = g.optimalChosenAmount * g.price;
//            }
//        }

        //For consistent data. 
        me.maxUtility = bestBundle.maxUtility;

        //and don't forget to actually move
//        System.out.println("BEFORE::: My location: " + me.xy.x + "," + me.xy.y + ", Best Bundle location: " + bestBundle.here.x + "," + bestBundle.here.y);
        //quick test hack: fixing half People in place
//        if (gl.peopleMobile && me.ID % 5 == 0) {
        if (gl.peopleMobile) {
            me.moveTo(bestBundle.here);
        }
//        System.out.println("AFTER::: My location: " + me.xy.x + "," + me.xy.y + ", Best Bundle location: " + bestBundle.here.x + "," + bestBundle.here.y);

//        System.out.println(" ");

        //Utility feedback tests
        //If not just stock target, but aiming to squeeze markup out..
//        if (!gl.stockTarget) {
//
//            //if last best utility was higher than now, utility's gone down
//            //and I'm going to blame my own price-setting
//            if (lastMaxUtility > me.maxUtility) {
//                System.out.println("goin down by: " + ((lastMaxUtility - me.maxUtility) / 50));
////                me.goodCost -= ((lastMaxUtility - me.maxUtility) / 1);
////                me.myGoodCost -= ((lastMaxUtility - me.maxUtility) / 1);
////                me.myGoodCost -= 0.05;
//                me.goodCost -= 0.05;
//            } else {
//                System.out.println("goin up by: " + ((me.maxUtility - lastMaxUtility) / 50));
////                me.goodCost += ((me.maxUtility - lastMaxUtility) / 1);
////                me.myGoodCost += ((me.maxUtility - lastMaxUtility) / 1);
////                me.myGoodCost += 0.05;
//                me.goodCost += 0.05;
//            }
//
//
//            lastMaxUtility = me.maxUtility;
//
//        }


        me.distanceToCentralFirm = gl.firms.get(0).getPoint().distance(me.getPoint());


    }//end heard

    /**
     * set prices for this bundle to give to others
     *
     * @param b
     */
    public void getBundlePrices(Bundle b) {

        //For transmission Actor, bestWage is their day's time.
        b.bestWage = 1;

//        System.out.println("In setPrices: size of found actors: " + me.lm.actors.size());

        //jiggle em
        me.lm.actors = RandPermuteArray.mix(me.lm.actors);

        //For each of the actors in my list... 
        for (ActorNLocation anl : me.lm.actors) {

            sap = (Firm) anl.actor;

            //Distance between proposed location to firm.
            //All should be relative to zero...
            distance = (zp.distance(anl.p));
//            System.out.println("distance: " + distance);

//            distance = 5;

            //get prices for all goods. Check if it's me, to make sure no space cost is added
            //(because I might be looking at a bundle on a distant spot and would add my distant self: wrong)
//            if (anl.actor.ID == me.ID) {

//                System.out.println("Found me!");


            //if just doing basic stocklevel targetting, I treat my own good like any other.
//                if (gl.stockTarget) {
//                    price = sap.getGoodCost();
//                } else {
            //otherwise I'm going to use my own price to see if I can get a markup
            //If I can change lower for my own good, I can be better off
//                price = (GoodSeller) me.goodCost;
//                }

//            } else {
            //standard space price
            price = sap.getGoodCost() + (sap.getDeliveryCost() * distance);

//                System.out.println("Found price for Firm good: " + price);

            //hard limit space price
//                price = sap.getGoodCost()/(1-(distance/gl.TECH));
//            }
            //System.out.println("total delivery cost calculated: " + price);
            //1 = index of the good
            b.addGood(sap, price);

        }//end for actornlocation


        if (gl.DENSITYCOST > 0) {

            //Use density cost finding method in SpaceTools
            pt = new Point2D.Double(me.xy.x + b.here.x, me.xy.y + b.here.y);
//            LocMemory dlm = me.getSpace().whosInRange(me, pt, gl.DENSITYCOSTRADIUS);
//            LocMemory dlm = me.getSpace().whosInRange(me, pt, gl.DENSITYCOSTRADIUS);
            LocMemory dlm = me.getSpace().peopleInRange(me, pt, gl.DENSITYCOSTRADIUS);

            //horrible hack - remove any Firms from that list
//            for (ActorNLocation anl : dlm.getActorNLocationArray()) {

//                for (int i = 0; i < dlm.actors.size(); i++) {
//
//                    //i.e. if it's a firm...
//                    if (dlm.actors.get(i).actor.ID >= gl.people.size()) {
//                        
//                    }
//
//                }




//            LocMemory dlm = me.getSpace().whosInRangeOfArray(gl.people, (HasLocation) me,
//                    SpaceTools.convertRelativeToAbsolutePoint(b.here, me.getPoint()),
//                    false, gl.DENSITYCOSTRADIUS);

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
