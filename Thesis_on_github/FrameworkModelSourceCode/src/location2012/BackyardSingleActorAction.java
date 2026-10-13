package location2012;

//package location1;
//
//import java.awt.geom.Point2D;
//import java.util.ArrayList;
//import location1.actiontools.Action;
//import location1.econs.Bundle;
//import location1.econs.GoodSeller;
//import location1.econs.OptimiseHillClimb;
//import location1.geog.LocMemory;
//import location1.geog.LocMemory.ActorNLocation;
//import location1.geog.Space;
//import location1.graphics.ProcessingOptimiserGrapher;
//import location1.observe.ShoutEvent;
//import location1.utils.Randoms;
//import location1.utils.Trig;
//import location1.utils.gl;
//
///**
// * For single actors deciding between backyard and collaboration under economies of scale
// *
// * @author Dan Olner
// */
//public class BackyardSingleActorAction extends Action {
//
//    //quick hack job to test landlords
//    GoodSeller landLord;
//    public Person me;
//    ActorNLocation a;
//    //For finding the best wage in a search
//    double bestWage;
//    //for finding commute and goods delivery costs - just calculate distance once and store
//    double distance;
//    //record of remaining time after work and commute
//    double remainingTime;
//    //For casting LocMemory actors to firms, to access Firm methods
//    Firm f;
//    //Current employer
//    public Firm currentEmployer;
//    //List of all Bundle - used for storing the best options
//    //from a particular spot
//    ArrayList<Bundle> bundles = new ArrayList<Bundle>();
//    //for calculating prices of goods before storing in spacePriceList
//    double price;
//    //Spare point for various pointy calcs
//    //Set to zero initially, for finding distances to stored LocMemory of actor positions
//    //since those are all relative to this Person's position
//    //Point2D.Double spp = new Point2D.Double(0, 0);
//    //used for comparisons for picking the best bundle
//    public Bundle bestBundle = new Bundle();
//    //
//    Point2D.Double zp = new Point2D.Double(0, 0);
//    //used for drawing optimisations
//    ProcessingOptimiserGrapher pog;
//
//    public BackyardSingleActorAction(Actor me) {
//
//        //ref to the actor acting this action!
//        this.me = (Person) me;
//
//        //set up drawer for drawing optimisations
//        if (me.ID == gl.LOOKATME) {
//            //pog = new ProcessingOptimiserGrapher(this, 500, 500);
//        }
//
//
//    }
//
//    public void heard(ShoutEvent s) {
//
//
//        //reset vars
//        bundles.clear();
//
//        //If backyard involved -
//        //new bestBundle
//        bestBundle = new Bundle();
//        //System.out.println("initial bestBundle maxutility: " + bestBundle.maxUtility);
//        //set max utility for the base best bundle to backyard
//        //bestBundle.maxUtility = gl.BY_OPTIMUM;
//
//        //remove time contribution from previous firm, if I had contributed any
//        if (currentEmployer != null) {
//            currentEmployer.removeTime(me.myCurrentWorkTime);
//        }
//
//        //set back to null, in case I don't get a new employer below.
//        currentEmployer = null;
//
//
//        //find out which firms are nearby, store that in my locmemory
//        //Only check 1/2 a day away, since >1/2 = no time for actually working
//        //Nix that: get a full list of firms within 1 day, since I
//        //need 1 day's distance goods to be included.
//        me.lm = me.getSpace().whosInRangeOfType(me, me.getPoint(), "Firm", 1);
//        //System.out.println("PersonAction. number of firms found: " + me.lm.actors.size());
//
//
//
//        //Begin search for best option from where I am
//        findBestWageAndPrices(new Bundle(new Point2D.Double(0, 0)));
//
//        //Switch if I'm stopping people from moving
//        if (gl.peopleMobile) {
//
//            //space-specific code required here. Any way of making this tidier? Can't currently think of it.
//            switch (gl.space) {
//
//                case Line:
//
//                    //number of random locations to sample
//                    for (double i = 0; i < 1; i++) {
//
//                        //See below for explanation of this convoluted method
//                        Point2D.Double moveP = new Point2D.Double(Randoms.lowHighDouble(-0.2, 0.2), 0);
//                        //moveP.y = 0;
//
//                        Point2D.Double moveP2 = new Point2D.Double(moveP.x + me.getx(), moveP.y + me.gety());
//
//                        me.lm = me.getSpace().whosInRangeOfType(me, moveP2, "Firm", 1);
//                        //findBestWageAndPrices(new Bundle(new Point2D.Double(Randoms.lowHighDouble(-1, 1) * Randoms.lowHighDouble(-1, 1), 0), 0));
//                        //findBestWageAndPrices(new Bundle(new Point2D.Double(((Randoms.nextDouble()*Randoms.nextDouble())/2 ), 0)));
//
//                        findBestWageAndPrices(new Bundle(moveP));
//
//                    }
//
//                    //totally random position
//                    for (int i = 0; i < 2; i++) {
//
//                        //point relative to my location
//                        Point2D.Double moveP =
//                                new Point2D.Double((double) me.getSpace().width / 2 - (Randoms.nextDouble() * (double) me.getSpace().width), 0);
//
//                        //See above for why this horrible point hackery.
//                        //Need a point for Bundle that's relative to my current location
//                        Point2D.Double notherFeckingPoint = new Point2D.Double(me.getx() + moveP.x, me.gety() + moveP.y);
//
//                        //moveP = me.getSpace().checkInTorus(moveP);
//                        //System.out.println("point loc: " + notherFeckingPoint.x +","+notherFeckingPoint.y);
//                        //
//                        me.lm = me.getSpace().whosInRangeOfType(me, notherFeckingPoint, "Firm", 1);
//
//                        //System.out.println("size of me.lm: "+ me.lm.actors.size());
//
//                        //findBestWageAndPrices(new Bundle(Trig.getRandPointInCircleOfRadius(1)));
//                        //experimenting with trying locations anywhere on the torus
//                        //
//                        findBestWageAndPrices(new Bundle(moveP));
//
//
//                    }
//
//                    break;
//
//                //if in torus, sample other spaces too
//                case Torus:
//
//                    //hackeroo, hackeraaa!
//                    double radius = 0.1;
//
//                    //Now try some sample spots nearby for better options - currently no move cost
//                    //hardcoding number of samplepoints here for now
//                    for (int i = 0; i < 1; i++) {
//
//                        //just to explain what's happening here before I forget:
//                        //whosInRange needs an absolute location, returns relative ones
//                        //So it needs a point that represents the absolute location of
//                        //the proposed move - hence moveP2.
//                        //We need to keep moveP, because Bundle works with relative location.
//                        //See, this was my method for simplifying location. Genius, huh?
//                        Point2D.Double moveP = Trig.getRandPointInCircleOfRadius(radius);
//
//                        radius = 0.5;
//
//                        Point2D.Double moveP2 = new Point2D.Double(moveP.x + me.getx(), moveP.y + me.gety());
//
//                        //System.out.print("Starting: " + moveP.x + "," + moveP.y);
//                        //Trig.moveToRandPointInCircleOfRadius(moveP, 0.5);
//                        //System.out.println(", ending: " + moveP.x + "," + moveP.y);
//
//                        //System.out.println("Point: " + moveP.x + "," + moveP.y);
//                        //moveP = me.getSpace().checkInTorus(moveP);
//                        //
//                        me.lm = me.getSpace().whosInRangeOfType(me, moveP2, "Firm", 1);
//
//                        //System.out.println("size of me.lm: "+ me.lm.actors.size());
//
//                        //findBestWageAndPrices(new Bundle(Trig.getRandPointInCircleOfRadius(1)));
//                        //experimenting with trying locations anywhere on the torus
//                        //
//                        findBestWageAndPrices(new Bundle(moveP));
//
//                    }
//
//                    //Allow a couple of samples anywhere in the space
//                    for (int i = 0; i < 2; i++) {
//
//                        //point relative to my location
//                        Point2D.Double moveP =
//                                new Point2D.Double((double) me.getSpace().width / 2 - (Randoms.nextDouble() * (double) me.getSpace().width),
//                                (double) me.getSpace().width / 2 - (Randoms.nextDouble() * (double) me.getSpace().width));
//
//                        //See above for why this horrible point hackery.
//                        //Need a point for Bundle that's relative to my current location
//                        Point2D.Double notherFeckingPoint = new Point2D.Double(me.getx() + moveP.x, me.gety() + moveP.y);
//
//                        //moveP = me.getSpace().checkInTorus(moveP);
//                        //System.out.println("point loc: " + notherFeckingPoint.x +","+notherFeckingPoint.y);
//                        //
//                        me.lm = me.getSpace().whosInRangeOfType(me, notherFeckingPoint, "Firm", 1);
//
//                        //System.out.println("size of me.lm: "+ me.lm.actors.size());
//
//                        //findBestWageAndPrices(new Bundle(Trig.getRandPointInCircleOfRadius(1)));
//                        //experimenting with trying locations anywhere on the torus
//                        //
//                        findBestWageAndPrices(new Bundle(moveP));
//
//
//                    }
//                    break;
//
//            }//end switch gl.space
//        }//end if people mobile
//        //Test
////        for (Bundle bwp : spacePriceList) {
////            bwp.printFullResults();
////        }
//        //System.out.println(" ");
//        //System.out.print(" Number of firms/goods in each bundle: ");
//
//        //how many firms/goods in each?
////        for (Bundle bwp : bundles) {
////
////            System.out.print(bwp.GoodsList.size() + " ");
////
////        }
////System.out.print("number of bundles: " + bundles.size() + ", opts: ");
//        boolean drawPog = false;
//
//        //Autobots, optimise!
//        for (Bundle bwp : bundles) {
//
//            //Haaa--cckkerrr-eeee
//            gl.writePog = (me.ID == gl.LOOKATME ? true : false);
//            //pass bundle to the hillclimber to find the best mix of goods
//
//            OptimiseHillClimb.optimise(bwp);
//            //System.out.print(bwp.maxUtility + " ");
//            //draw the result if it's me we're drawing
//
//            if (bwp.maxUtility > bestBundle.maxUtility) {
//                //System.out.println("New best utility found in bundle: " + bwp.maxUtility);
//                bestBundle = bwp;
//                //System.out.println("new max: " + bestBundle.maxUtility);
//                //keep a record of the optimal data for the POG
//                OptimiseHillClimb.setOptimalPog();
//                drawPog = true;
//
//            }
//        }
//
//        if (me.ID == gl.LOOKATME && pog != null && drawPog) {
//            pog.heard();
//        }
//
//        //System.out.println(" ");
////        System.out.println("In PersonAction. Optimal chosen: ");
////            for (Good g : bestBundle.GoodsList) {
////
////                System.out.println("OptimalChosenAmount: " + g.optimalChosenAmount + ", Good " + g.id + ", type = " + g.f.goodType + ", spend = " + g.tempBudgetSpend);
////
////            }
////        System.out.println("number of goods bought: " + bestBundle.GoodsList.size());
//
//
//        //set my current work time (so I can remove it from my chosen firm next time.)
//        me.myCurrentWorkTime = bestBundle.optimalWorkTime;
//        //System.out.println("xx " + me.myCurrentWorkTime);
//
//        if (bestBundle.employer != null) {
//
//            currentEmployer = bestBundle.employer;
//            //agree to work for currentEmployer
//
//            currentEmployer.giveWorkTime(me.myCurrentWorkTime);
//
//            //hack landLord
//            if (gl.landCostsOn > 0) {
//
//                bestBundle.landLord.buyGood(0, bestBundle.landLord.getGoodCost());
//
//            }
//
//        } else {
//            System.out.println("PersonAction: bestBundle employer == null");
//        }
//
//        //buy the goods
//        //me.myMoney should equal this: which is does... phew.
//        //System.out.println("my money: " + me.myMoney + ", money in bundle: " + (bestBundle.optimal_1spend + bestBundle.optimal_2spend));
//
//        //note: the circularity of this - we've already worked out what the wage above could buy
//        //So these two payment lines of code should always leave the Person with zero.
//        //But worth doing explicitly...
//        //
//        //double goodsCost = bestBundle.buyGoods();
//        //me.myMoney -= bestBundle.buyGoods();
//        bestBundle.buyGoods();
//
//
//
//        //me.myMoney += (wage - goodsCost);
//
//        //and don't forget to actually move
//        //System.out.println("BEFORE::: My location: " + me.xy.x + "," + me.xy.y + ", Best Bundle location: " + bestBundle.here.x + "," + bestBundle.here.y);
//        if (gl.peopleMobile) {
//            me.moveTo(bestBundle.here);
//        }
//        //System.out.println("AFTER:::: My location: " + me.xy.x + "," + me.xy.y + ", Best Bundle location: " + bestBundle.here.x + "," + bestBundle.here.y);
//
//        //Testing: locking agent location to a firm
////        if (bestBundle.employer != null) {
////            me.setx(bestBundle.employer.getx());
////            me.sety(bestBundle.employer.gety());
////        }
//
//
//        //This is just done to make sure the LocMemory has correct refs so that Torus can draw it -
//        //it serves no other purpose apart from ensuring accurate drawing.
//        //me.lm = me.getSpace().whosInRange(me, me.getPoint(), "Firm", 0.5);
//
//        //  }//end if
//
//        //me.lm = new LocMemory();
//
//
//    }
//
//
//    /*
//     * Finds the best work and trade options based on my current LocMemory
//     */
//    private void findBestWageAndPrices(Bundle bwp) {
//
//        //Reset variables
//        bestWage = 0;
//
//        //for certain spaces, need to randomise firm order
//        //since distances won't offer a fine-grained difference between firms
////        if (gl.space == gl.SpaceType.Point) {
////            me.lm.actors = RandPermuteArray.mix(me.lm.actors);
////            //p.s("Length of me.lm.actors: " + me.lm.actors.size(), this);
////        }
//
//        //System.out.println("size of firms list: " + me.lm.actors.size());
//
//
//        //Two things to find out: what's the best wage from this point?
//        //And, what's the best prices? Prices are ranked into two objects
//        //One for each type of good
//        for (ActorNLocation ac : me.lm.actors) {
//
//
//            //start with a random firm - works with Point better
//            //for (int i = 0; i < me.lm.actors.size(); i++) {
//
//            f = (Firm) ac.actor;
//            //f = me.lm.actors.get(i).actor;
//
//            //does this firm have a better offer, accounting for commute costs to them?
//            //So - wageOffer is price per day's work. I'm proposing to contribute what
//            //time I have left over from commute.
//            //Keep the firm
//
//            //Distance between proposed location to firm.
//            //All should be relative to zero...
//            distance = (zp.distance(ac.p));
//            //distance = (bwp.here.distance(ac.p));
//            //System.out.println("found distance between point and firm: " + distance);
//
//            //what's my time left over after commute?
//            //I'll try splitting this between work and backyard
//            if (gl.peopleWageDistanceCost) {
//                remainingTime = 1 - (2 * distance);
//            } else {
//                remainingTime = 1;
//            }
//
//            //System.out.println("RemainingTime: " + remainingTime);
//
//            //int firms = 0;
//
//            //2*distance = commute cost
//            //If there's any time left after commute for actually working
//            //and the wage offer is better than others found so far...
//            if (remainingTime > 0 && f.getWageOffer() * remainingTime > bestWage) {
//
//                //set remainingTime in bwp
//                bwp.addRemainingTime(remainingTime);
//
//                //at the moment, because I'm only working, no backyard
//                //remainingTime is the same thing as my optimalWorktime
//                bwp.optimalWorkTime = bwp.remainingTime;
//
//                bestWage = f.wageoffer * remainingTime;
//                //System.out.println("bestWage: " + bestWage);
//
//                bwp.addPotentialEmployer(f, bestWage);
//
//            }//end if wage
//
//            //experimenting with allowing actors to access goods up to 1 day away
//            //in contrast to the half-day distance for commuting
//            //what price can I get from this firm for good1, per unit of good?
//            //Price is raw + (delivery cost * distance)
//            //Delivery cost is, at present, a % of raw price
//            //Price is per unit of good delivered
//            //Question: if sometimes no potential employers are found,
//            //do I want to be doing these price checks? Not in the first model run
//            //since I need the wage I've just got to be able to buy anything
//            //but maybe later if I've saved up!
//
//            //Note: the distance I can get goods from will have been limited by
//            //the size of the whosInRadius search. As long as these two things are
//            //the same, won't need to change it here, or check for it.
//            if (f.goodStock > 0) {
//                price = f.getGoodCost() + (f.getGoodCost() * f.getDeliveryCost() * distance);
//                //System.out.println("total delivery cost calculated: " + price);
//                //1 = index of the good
//                bwp.addGood(f, price);
//
//            }
//
//
//
//        }//end for each ActorNLocation
//
////        System.out.println("Number of detected goods: " + bwp.GoodsList.size());
//
//        //If landcosts are on, find the nearest landlord agent
////        if (gl.landCostsOn > 0) {
////
////            Point2D.Double newloc = new Point2D.Double(me.xy.x + bwp.here.x, me.xy.y + bwp.here.y);
////
////            GoodSeller gs = (GoodSeller) Space.whosNearest(newloc, gl.landLords);
//////            System.out.println("bwp xy: " + bwp.here.x + "," + bwp.here.y);
////            bwp.addGood(gs, gs.getGoodCost());
////            //Actor rew = (Actor) gs;
////            //System.out.println("PersonAction: added gs: " + rew.xy.x + "," + rew.xy.y + ", cost: " + gs.getGoodCost());
////
////        }
//
//
//        //don't consider this bundle if there were no goods or no firms
//        //if (bwp.employer != null && bwp.GoodsList.size() > 0) {
//        if (bwp.GoodsList.size() > 0) {
//
//            //If I'm fixing the wage, fix it here
//            if (gl.peopleFixWage > 0) {
//                bwp.bestWage = gl.peopleFixWage;
//
//            }
//
//            //different rent approach: take directly off wage. This means people are forced to pay it
//            //rather than having the option, depending on utility.
//            if (gl.landCostsOn > 0) {
//
//                Point2D.Double newloc = new Point2D.Double(me.xy.x + bwp.here.x, me.xy.y + bwp.here.y);
//
//                bwp.landLord = (GoodSeller) Space.whosNearest(newloc, gl.landLords);
//
//                bwp.bestWage = (bwp.bestWage - bwp.landLord.getGoodCost() > 0 ? bwp.bestWage - bwp.landLord.getGoodCost() : 0);
//
//                //why else if? Cos these are both land proxies. Won't be using both.
//            } else if (gl.DENSITYCOST > 0) {
//
//                //Use density cost finding method in Space
//                Point2D.Double newloc = new Point2D.Double(me.xy.x + bwp.here.x, me.xy.y + bwp.here.y);
//                LocMemory dlm = me.getSpace().whosInRange(me, newloc, gl.DENSITYCOSTRADIUS);
////                System.out.print("Density cost. Points found: " + dlm.actors.size());
//
////                System.out.println(", density cost: " + Space.findDensityCost(dlm));
//
////                System.out.print("Wage before: " + bwp.bestWage + ", ");
//                bwp.bestWage -= (gl.DENSITYCOST * Space.findDensityCost(dlm) * bwp.bestWage);
////                System.out.println("wage after: " + bwp.bestWage);
//
//            }
//
//            //            System.out.println("bwp xy: " + bwp.here.x + "," + bwp.here.y);
//            //bwp.addGood(gs, gs.getGoodCost());
//            //Actor rew = (Actor) gs;
//            //System.out.println("PersonAction: added gs: " + rew.xy.x + "," + rew.xy.y + ", cost: " + gs.getGoodCost());
//
//
//            //Add the bwp to the array
//            if (bwp.bestWage > 0) {
//                bundles.add(bwp);
//            }
//
//        }//end if bwp.GoodList size
//    }//end method findBestWorkAndPrices
//
//
//
//
//}
////Cuttinz
//
