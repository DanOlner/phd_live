package location2012;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import location2012.actiontools.Action;
import location2012.actiontools.RandPermuteArray;
import location2012.econs.Bundle;
import location2012.econs.GoodSeller;
import location2012.econs.OptimiseHillClimb;
import location2012.econs.UtilityShell;
import location2012.geog.HasLocation;
import location2012.geog.LocMemory;
import location2012.geog.LocMemory.ActorNLocation;
import location2012.geog.SpaceTools;
import location2012.graphics.ProcessingOptimiserGrapher;
import location2012.observe.ShoutEvent;
import location2012.utils.gl;
import location2012.io.staticArrayWriter;
import processing.core.PApplet;

/**
 * Testing an Action...
 *
 * @author Dan Olner
 */
//public class PersonAction extends Action implements Drawable {
public class PersonAction extends Action {

    //quick hack job to test landlords
    GoodSeller landLord;
    public Person me;
    ActorNLocation a;
    //For finding the best wage in a search
    double bestWage;
    //for finding commute and goods delivery costs - just calculate distance once and store
    double distance;
    //record of remaining time after work and commute
    double remainingTime;
    //For casting LocMemory actors to firms, to access Firm methods
    Firm f;
    //Current employer
    public Firm currentEmployer;
    //List of all Bundle - used for storing the best options
    //from a particular spot
    //Initial list to check: empty array of Bundles just with locations set
    public ArrayList<Bundle> initialBundles = new ArrayList<Bundle>();
    //And the ones to use. Need to do this because you can't pull Objects out while
    //you're iterating over the array
    public ArrayList<Bundle> bundlesToOptimise = new ArrayList<Bundle>();
    //for calculating prices of goods before storing in spacePriceList
    double price;
    //Spare point for various pointy calcs
    //Set to zero initially, for finding distances to stored LocMemory of actor positions
    //since those are all relative to this Person's position
    //Point2D.Double spp = new Point2D.Double(0, 0);
    //used for comparisons for picking the best bundle
    public Bundle bestBundle = new Bundle();
    //whosInRange searches return ActorNLocations with location relative to my centre.
    //zp can be used to represent this centre point.
    Point2D.Double zp = new Point2D.Double(0, 0);
    //general porpoise points. Doing this to try and free up memory. We'll see.
    Point2D.Double pt = new Point2D.Double(0, 0);
    Point2D.Double pt2 = new Point2D.Double(0, 0);
    //used for drawing optimisations
    ProcessingOptimiserGrapher pog;
    //nearest neighbour: for data output
    //public double nearestNeighbour;
    /**
     * Data output variables
     */
    //Density cost from whatever Bundle was selected
    public double rawDensityCost, adjustedDensityCost, chosenDensityCost;

    public PersonAction(Actor me) {

        //ref to the actor acting this action!
        this.me = (Person) me;

        //set up drawer for drawing optimisations
//        if (me.ID == gl.LOOKATME) {
//            pog = new ProcessingOptimiserGrapher(this, 500, 500);
//        }


    }

    public void heard(ShoutEvent s) {

        //Quite a timehog. Important for data though.
        me.findNearestNeighbour();



        //for checking if it's time to write optimisation data

//        checkWriteOptData();


        //System.out.println("ID: " + me.ID + ", my loc: " + me.getx() + "," + me.gety());

        //reset vars
        initialBundles.clear();
        bundlesToOptimise.clear();

        //If backyard involved -
        //new bestBundle
        bestBundle = new Bundle();
        //System.out.println("initial bestBundle maxutility: " + bestBundle.maxUtility);
        //set max utility for the base best bundle to backyard
        //bestBundle.maxUtility = gl.BY_OPTIMUM;

        //remove time contribution from previous firm, if I had contributed any
        if (currentEmployer != null) {
            currentEmployer.removeTime(me.myCurrentWorkTime);
        }

        //set back to null, in case I don't get a new employer below.
        currentEmployer = null;

        //Create array of new initialBundles with relative locations
        //Get an initial random list (based on space type and whether mobile)
        //This list will shrink if a bundle tested next is no good (e.g. no wage)
        //Number of initialBundles in short range, radius for random location relative to me
        //and number to sample from whole space
        Bundle.addToBundles(initialBundles, 10, 0.2, 10);

        //hacking this in for drawing
        for (Bundle b : initialBundles) {
            b.myAbsoluteLocation.x = me.getx();
            b.myAbsoluteLocation.y = me.gety();
        }


        //test - only difference being, I need to remove bundle, not add
        for (Bundle b : initialBundles) {

            me.lm = me.getSpace().whosInRangeOfArray(gl.goodSellers, (HasLocation) me,
                    SpaceTools.convertRelativeToAbsolutePoint(b.here, me.getPoint()), !gl.excludeMe, 1);
//            
//            me.lm = me.getSpace().firmsInRange(me, notherFeckingPoint, 1);

            findBestWageAndPrices(b);

        }

        //Test
//        for (Bundle bwp : spacePriceList) {
//            bwp.printFullResults();
//        }
        //System.out.println(" ");
        //System.out.print(" Number of firms/goods in each bundle: ");

        //how many firms/goods in each?
//        for (Bundle bwp : initialBundles) {
//
//            System.out.print(bwp.GoodsList.size() + " ");
//
//        }
//System.out.print("number of initialBundles: " + initialBundles.size() + ", opts: ");
        boolean drawPog = false;

        //Autobots, optimise!
        for (Bundle bwp : bundlesToOptimise) {

            //Haaa--cckkerrr-eeee
            gl.writePog = (me.ID == gl.LOOKATME ? true : false);
            //pass bundle to the hillclimber to find the best mix of goods

            if (gl.optimiser == gl.Optimiser.Greedy) {
                OptimiseHillClimb.optimise(bwp);
//                System.out.println("in greedy opt.");
            } else if (gl.optimiser == gl.Optimiser.Constrained) {
                UtilityShell.giveUtility(bwp);
//                System.out.println("In constrained optimisation");
            }

            //density cost test
//            bwp.maxUtility *= (1 - bwp.rawDensityCost);

//            System.out.println("bwp.maxUtility: " + bwp.maxUtility);
            //draw the result if it's me we're drawing
//            System.out.println("ites: " + OptimiseHillClimb.iterations);

            if (bwp.maxUtility > bestBundle.maxUtility) {
                //System.out.println("New best utility found in bundle: " + bwp.maxUtility);
                bestBundle = bwp;
                chosenDensityCost = bwp.adjustedDensityCost;
                //hack
                me.chosenDensityCost = chosenDensityCost;
                //System.out.println("new max: " + bestBundle.maxUtility);
                //keep a record of the optimal data for the POG
                OptimiseHillClimb.setOptimalPog();
                drawPog = true;

            }
        }

        if (me.ID == gl.LOOKATME && pog != null && drawPog) {
            pog.heard();
        }

        //System.out.println(" ");
//        System.out.println("In PersonAction. Optimal chosen: ");
//            for (Good g : bestBundle.GoodsList) {
//
//                System.out.println("OptimalChosenAmount: " + g.optimalChosenAmount + ", Good " + g.id + ", type = " + g.f.goodType + ", spend = " + g.tempBudgetSpend);
//
//            }
//        System.out.println("number of goods bought: " + bestBundle.GoodsList.size());


        //set my current work time (so I can remove it from my chosen firm next time.)
        me.myCurrentWorkTime = bestBundle.optimalWorkTime;
        me.maxUtility = bestBundle.maxUtility;
//        System.out.println("bestBundle optimal work time: " + me.myCurrentWorkTime);

        //for data: how many People did I detect in my density cost radius?
        me.numPeopleInDensityCostRadius = bestBundle.numPeopleInDensityCostRadius;
        //for data: mean distance from me? I suspect often they'll be on top of each other (so mean close to zero)
        me.meanDistanceOfPeopleInDensityCostRadius = bestBundle.meanDistanceOfPeopleInDensityCostRadius;
        me.bestBundle = bestBundle;

//        me.distanceToCentralFirm = gl.firms.get(0).getPoint().distance(me.getPoint());
        
        
        if (bestBundle.employer != null) {

            currentEmployer = bestBundle.employer;
            //agree to work for currentEmployer

            currentEmployer.acceptWageOffer(me.myCurrentWorkTime);
            //hack for recording my distance to central firm in monocentric models
//            me.distanceToCentralFirm = currentEmployer.getPoint().distance(me.getPoint());
            me.distanceToCentralFirm = gl.firms.get(0).getPoint().distance(me.getPoint());

            //hack landLord
            if (gl.landCostsOn > 0) {

                bestBundle.landLord.buyGood(0, bestBundle.landLord.getGoodCost());

            }

        } else {
//            System.out.println("PersonAction: bestBundle employer == null");
        }

        //buy the goods
        //me.myMoney should equal this: which is does... phew.
        //System.out.println("my money: " + me.myMoney + ", money in bundle: " + (bestBundle.optimal_1spend + bestBundle.optimal_2spend));

        //note: the circularity of this - we've already worked out what the wage above could buy
        //So these two payment lines of code should always leave the Person with zero.
        //But worth doing explicitly...
        //
        //double goodsCost = bestBundle.buyGoods();
        //me.myMoney -= bestBundle.buyGoods();
        bestBundle.buyGoods();

//        System.out.println("best bundle size: " + bestBundle.GoodsList.size());
//        
//        System.out.println("Best bundle firm wage: " + bestBundle.employer.wage);
//        System.out.println("Best bundle good cost: " + bestBundle.employer.goodCost);



        //me.myMoney += (wage - goodsCost);

        //and don't forget to actually move
        //System.out.println("BEFORE::: My location: " + me.xy.x + "," + me.xy.y + ", Best Bundle location: " + bestBundle.here.x + "," + bestBundle.here.y);
        if (gl.peopleMobile) {

            //for working out how far I'm moving between iterations
            me.locationYesterday.x = me.getx();
            me.locationYesterday.y = me.gety();

            me.moveTo(bestBundle.here);
        }

        me.distanceMovedToday = me.getPoint().distance(me.locationYesterday);
//        System.out.println("distance moved today: " + me.distanceMovedToday);

        //System.out.println("AFTER:::: My location: " + me.xy.x + "," + me.xy.y + ", Best Bundle location: " + bestBundle.here.x + "," + bestBundle.here.y);

        //Testing: locking agent location to a firm
//        if (bestBundle.employer != null) {
//            me.setx(bestBundle.employer.getx());
//            me.sety(bestBundle.employer.gety());
//        }


        //This is just done to make sure the LocMemory has correct refs so that Torus can draw3D it -
        //it serves no other purpose apart from ensuring accurate drawing.
        //me.lm = me.getSpace().whosInRangeOfArray(me, me.getPoint(), "Firm", 0.5);

        //  }//end if

        //me.lm = new LocMemory();


    }


    /*
     * Finds the best work and trade options based on my current LocMemory
     */
    private void findBestWageAndPrices(Bundle bwp) {

        //Reset variables
        bestWage = 0;

        //for certain spaces, need to randomise firm order
        //since distances won't offer a fine-grained difference between firms
        //or actually if wages are fixed...
//        if (gl.space == gl.SpaceType.Point) {
        me.lm.actors = RandPermuteArray.mix(me.lm.actors);
//            //p.s("Length of me.lm.actors: " + me.lm.actors.size(), this);
//        }

//        System.out.println("size of firms list: " + me.lm.actors.size());
//        for(ActorNLocation a : me.lm.actors) {
//
//            Firm fm = (Firm) a.actor;
//
//            System.out.println("ID: " + a.actor.ID + ", wage: " + fm.wageoffer);
//
//        }
//
//        System.out.println(" ");

        //Two things to find out: what's the best wage from this point?
        //And, what's the best prices? Prices are ranked into two objects
        //One for each type of good
        for (ActorNLocation ac : me.lm.actors) {


            //start with a random firm - works with Point better
            //for (int i = 0; i < me.lm.actors.size(); i++) {
            this.f = (Firm) ac.actor;
            //f = me.lm.actors.get(i).actor;

            //does this firm have a better offer, accounting for commute costs to them?
            //So - wageOffer is price per day's work. I'm proposing to contribute what
            //time I have left over from commute.
            //Keep the firm

            //Distance between proposed location to firm.
            //All should be relative to zero...
            distance = (zp.distance(ac.p));
            //distance = (bwp.here.distance(ac.p));
//            System.out.print("found distance between point and firm: " + distance + ", ");

            //what's my time left over after commute?
            if (gl.peopleWageDistanceCost) {

                if (gl.vonThunenCommute) {

                    if (this.f.ID == me.vonThunenFirmID) {
                        remainingTime = 1 - (distance * f.commuteCost);
                    } else {
                        remainingTime = 0;
                    }
                } else {
                    remainingTime = 1 - (distance * f.commuteCost);
                }
            } else {
                remainingTime = 1;
            }

//            System.out.println("RemainingTime: " + remainingTime);

            //int firms = 0;

            //2*distance = commute cost
            //If there's any time left after commute for actually working
            //and the wage offer is better than others found so far...

            if (!gl.vonThunenWage && (remainingTime > 0 && this.f.getWageOffer() * remainingTime > bestWage)) {

                //at the moment, because I'm only working, no backyard
                //remainingTime is the same thing as my optimalWorktime

                //set remainingTime in bwp
//                bwp.addRemainingTime(remainingTime);

                bwp.optimalWorkTime = remainingTime;

                bestWage = this.f.wage * remainingTime;

                bwp.addPotentialEmployer(this.f, bestWage);

//                System.out.println("Best wage found: " + bwp.bestWage);


            }//end if wage

            if (gl.vonThunenWage && this.f.ID == me.vonThunenFirmID) {
                //set remainingTime in bwp
//                bwp.addRemainingTime(remainingTime);

                bwp.optimalWorkTime = remainingTime;

                //Just putting in brackets to help make clear what's happening:
                //vonThunenFirmIndex is there to make multiples of the base wage offer
                //It starts at 1.
//                bestWage = (this.f.wageOffer * me.vonThunenFirmIndex) * remainingTime;
                bestWage = this.f.wage * remainingTime;

                bwp.addPotentialEmployer(this.f, bestWage);

//                System.out.println("added employer, bestwage:" + bwp.bestWage);

            }

            //Price is raw + (delivery cost * distance)
            //Price is per unit of good delivered

            //Note: the distance I can get goods from will have been limited by
            //the size of the whosInRadius search. As long as these two things are
            //the same, won't need to change it here, or check for it.
            if (this.f.stock > 0) {

                //Also, if this is von Thunen mode, only add the good from 'my' Firm
                if ((!gl.vonThunenMode)
                        || ((gl.vonThunenMode) && this.f.ID == me.vonThunenFirmID)) {


//                    System.out.println("adding Firm good: " + f.ID + ", my vont num: " + me.vonThunenFirm);

                    //make multiple of either good or delivery cost
                    //vonThunenFirmIndex starts at zero and increments for each Firm. 
                    //Is just same as ID, only minus number of people since Firm ID indexing starts after People's
//                    price = (this.f.getGoodCost() * me.vonThunenFirmIndex) + (this.f.getDeliveryCost() * distance);
//                    price = this.f.getGoodCost() + ((this.f.getDeliveryCost() * me.vonThunenFirmIndex) * distance);
                    //
                    price = this.f.getGoodCost() + (this.f.getDeliveryCost() * distance);

                    //System.out.println("total delivery cost calculated: " + price);
                    //1 = index of the good
                    bwp.addGood(this.f, price);
//                    System.out.println("adding good " + bwp.GoodsList.size()+ ", price: " + price + ", base price: " +  this.f.getGoodCost());

                }

            }



        }//end for each ActorNLocation

//        System.out.println("Number of detected goods: " + bwp.GoodsList.size());

        //If landcosts are on, find the nearest landlord agent
//        if (gl.landCostsOn > 0) {
//
//            Point2D.Double newloc = new Point2D.Double(me.xy.x + bwp.here.x, me.xy.y + bwp.here.y);
//
//            GoodSeller gs = (GoodSeller) SpaceTools.whosNearest(newloc, gl.landLords);
////            System.out.println("bwp xy: " + bwp.here.x + "," + bwp.here.y);
//            bwp.addGood(gs, gs.getGoodCost());
//            //Actor rew = (Actor) gs;
//            //System.out.println("PersonAction: added gs: " + rew.xy.x + "," + rew.xy.y + ", cost: " + gs.getGoodCost());
//
//        }


        //don't consider this bundle if there were no goods or no firms
        //if (bwp.employer != null && bwp.GoodsList.size() > 0) {
        if (bwp.GoodsList.size() > 0) {

            //If I'm fixing the wage, fix it here...
            //No, don't! It's fixed in Firm's getWageOffer. That way, it can still
            //Have a spatial element to it.
//            if (gl.peopleFixWage > 0) {
//                bwp.bestWage = gl.peopleFixWage;

            //hacking different wages for half of People.
            //They get ID number first, so -
//                if (me.ID < gl.people.size() / 2) {
//                    bwp.bestWage = gl.peopleFixWage;
//                } else {
//                    bwp.bestWage = gl.peopleFixWage * 2;
//                }



            //different rent approach: take directly off wage. This means people are forced to pay it
            //rather than having the option, depending on utility.
            if (gl.landCostsOn > 0) {

                pt = new Point2D.Double(me.xy.x + bwp.here.x, me.xy.y + bwp.here.y);

                bwp.landLord = (GoodSeller) SpaceTools.whosNearest(pt, gl.landLords);

                bwp.bestWage = (bwp.bestWage - bwp.landLord.getGoodCost() > 0 ? bwp.bestWage - bwp.landLord.getGoodCost() : 0);

                //why else if? Cos these are both land proxies. Won't be using both.
            } else if (gl.DENSITYCOST > 0) {

                //Use density cost finding method in SpaceTools
                pt = new Point2D.Double(me.xy.x + bwp.here.x, me.xy.y + bwp.here.y);
                //includes all actors
//                LocMemory dlm = me.getSpace().whosInRange(me, pt, gl.DENSITYCOSTRADIUS);

                //includes only People
                //making new so each Bundle can have its own ref
                LocMemory dlm = new LocMemory();
                dlm = me.getSpace().peopleInRange(me, pt, gl.DENSITYCOSTRADIUS);
//                LocMemory dlm = me.getSpace().whosInRangeOfArray(gl.people, (HasLocation) me,
//                    pt, gl.excludeMe, gl.DENSITYCOSTRADIUS);

                //for data: how many People did I detect in my density cost radius?
                bwp.numPeopleInDensityCostRadius = dlm.actors.size();
                //for data: mean distance from me? I suspect often they'll be on top of each other (so mean close to zero)
                bwp.meanDistanceOfPeopleInDensityCostRadius = dlm.meanDistanceOfPeopleInDensityCostRadius;


//                LocMemory dlm = me.getSpace().whosInRangeOfArray(gl.people, me, pt, false, gl.DENSITYCOSTRADIUS);
//                System.out.print("Density cost. Points found: " + dlm.actors.size());

//                System.out.println(", density cost: " + SpaceTools.findDensityCost(dlm));

//                System.out.print("Wage before: " + bwp.bestWage + ", ");
//                densityCost = gl.DENSITYCOST * SpaceTools.findDensityCost(dlm) * bwp.bestWage;

                //add ref of LocMemory to Bundle for later data use.


                rawDensityCost = SpaceTools.findDensityCost(dlm);
                adjustedDensityCost = gl.DENSITYCOST * rawDensityCost;
//                System.out.println("raw density cost: " + rawDensityCost);
//                dlm = null;
                bwp.bestWage -= adjustedDensityCost;
//                bwp.bestWage *= (1 - rawDensityCost);
//                System.out.println("wage after: " + bwp.bestWage);
                //store for picking whichever we ended up with
                bwp.rawDensityCost = rawDensityCost;
                bwp.adjustedDensityCost = adjustedDensityCost;
                bwp.densityCostActors = dlm;

            }

            //            System.out.println("bwp xy: " + bwp.here.x + "," + bwp.here.y);
            //bwp.addGood(gs, gs.getGoodCost());
            //Actor rew = (Actor) gs;
            //System.out.println("PersonAction: added gs: " + rew.xy.x + "," + rew.xy.y + ", cost: " + gs.getGoodCost());


            //Add the bwp to the array
            if (bwp.bestWage > 0) {
                bundlesToOptimise.add(bwp);
            }

        }//end if bwp.GoodList size

//        System.out.println("Number of detected goods: " + bwp.GoodsList.size());

    }//end method findBestWorkAndPrices

    public void checkWriteOptData() {

        if (gl.day == 3) {

            //if it's a good time, write out the optimiser data
            if (gl.LOOKATME == me.ID && OptimiseHillClimb.pogData != null) {



                //System.out.println("s.he:" + s.heardValue);

                //make a new array, adding a column for each total of nec and lux goods
                //System.out.println(OptimiseHillClimb.optimalPogData.length+","+OptimiseHillClimb.optimalPogData[0].length);
                double[][] optData = new double[OptimiseHillClimb.optimalPogData.length][OptimiseHillClimb.optimalPogData[0].length + 2];

                //add pogData to optData


                for (int i = 0; i < OptimiseHillClimb.optimalPogData.length; i++) {
                    double luxTot = 0;


                    double necTot = 0;


                    for (int j = 0; j < OptimiseHillClimb.optimalPogData[0].length; j++) {

                        //add to the two extra columns, one for each good type total
                        //at the moment, 0 is lux, 1 is nec etc. Watch that though!
                        //Don't include last column - that's utility
                        if (j < OptimiseHillClimb.optimalPogData[0].length - 1) {
                            if (j % 2 == 0) {
                                //System.out.println("ping:" + j);
                                luxTot += OptimiseHillClimb.optimalPogData[i][j];


                            } else {
                                //System.out.println("pong:" + j);
                                necTot += OptimiseHillClimb.optimalPogData[i][j];


                            }
                        }

                        optData[i][j] = OptimiseHillClimb.optimalPogData[i][j];



                    }//end for j

                    //add the extra total columns
                    optData[i][OptimiseHillClimb.optimalPogData[0].length] = luxTot;
                    optData[i][OptimiseHillClimb.optimalPogData[0].length + 1] = necTot;



                } //Make array for column names



                staticArrayWriter.write2DDoubleArray(optData, "hillclimbdata");



            }

        }

    }

    /*
     * Drawing optimisations
     */
    public void draw3D(PApplet pa) {

        //Lets just test with one colour for now.
        //pa.strokeWeight();
        //pa.noFill();

        //+1 for when I'm trying (so far failing) to access a value to look at utility
        //for (int val = 0; val < OptimiseHillClimb.goods.size()+1; val++) {
        for (int val = 0; val < OptimiseHillClimb.goods.size(); val++) {
            //System.out.println("val: " + val);

            //draw hill climb data, iterations on horizontal axis
//            for (int ite = 0; ite < OptimiseHillClimb.iterations; ite += (OptimiseHillClimb.iterations / 400)) {
            for (int ite = 0; ite < OptimiseHillClimb.iterations; ite++) {

                //System.out.println("check vals. Iterations: " + (float) ite * (pa.width / (float) OptimiseHillClimb.iterations)
                //      + ", Values: " + (float) (pa.height - (OptimiseHillClimb.pogData[ite][val] * (pa.height / 2))));

                //assume max utility of 1.5 for now
                pa.stroke(100);


                //if (val < OptimiseHillClimb.goods.size()) {
                //System.out.println("goods, val: " + val);
                //adjust height to number of goods. Max spend is total quantity on one good.
                pa.rect((float) ite * (pa.width / (float) OptimiseHillClimb.iterations),
                        (float) (pa.height - (OptimiseHillClimb.optimalPogData[ite][val]) * 8 * (pa.height)), 3, 3);
//                } else {
//                System.out.println("utility, val: " + val);
//                    //utility
//                    pa.stroke(0, 100, 0);
//                    pa.ellipse((float) ite * (pa.width / (float) OptimiseHillClimb.iterations),
//                            (float) (pa.height - (OptimiseHillClimb.optimalPogData[ite][val] * (pa.height))), 2, 2);
//
//                }


//
//                //write values occasionally
//                if (ite == OptimiseHillClimb.iterations - 10) {
//                    //System.out.println("In draw3D: good " + val + ", " + OptimiseHillClimb.optimalPogData[ite][val]);
//                }


            }//end for ite

        }//end for val

        //check good totals
//        if (OptimiseHillClimb.goods.size() > 0) {
//            System.out.println("good total: "
//                    + (OptimiseHillClimb.optimalPogData[OptimiseHillClimb.iterations - 10][0] + OptimiseHillClimb.optimalPogData[OptimiseHillClimb.iterations - 10][1]));
//        }


    }//end draw3D

    public void draw2D(PApplet p) {
    }
}
//Cuttinz

