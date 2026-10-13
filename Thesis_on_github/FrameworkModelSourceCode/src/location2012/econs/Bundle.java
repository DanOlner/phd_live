package location2012.econs;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import location2012.Firm;
import location2012.Person;
import location2012.geog.LocMemory;
import location2012.utils.Randoms;
import location2012.utils.Trig;
import location2012.utils.gl;

/**
 * For storing the best wage and a ranking of the best prices worked out from a
 * particular point relative to a person
 *
 * Details worked out in the FindBestWageAndPrices method of PersonAction
 *
 * @author Dan Olner
 */
public class Bundle {

    //hack test for landLords
    public GoodSeller landLord;

    /*
     * Class vars
     */
    //Where's "here?" Relative to location of person
    public Point2D.Double here = new Point2D.Double();
    //reference within Bundle to make sure it's accurate for drawing porpoises
    public Point2D.Double myAbsoluteLocation = new Point2D.Double();
    public Firm employer;
    public double bestWage;
    //time left over if I commute to this job
    public double remainingTime;
    //list of goods, firms selling those goods
    public ArrayList<Good> GoodsList = new ArrayList<Good>();
    //
    double moneyFromBefore;
    //Maximum utility, used for selecting best bundle
    public double maxUtility = 0;
    //optimal work time - for whatever firm I worked for
    public double optimalWorkTime;
    //for giving goods unique ID separate from firm's
    public int ID = 0;
    //local ref to LocMemory data bits. This is for data, Bundle doesn't use it
    //record of discrete number of People I detected while working out density cost
    public int numPeopleInDensityCostRadius;
    //record of mean distance from me. I suspect often they'll be on top of each other
    public double meanDistanceOfPeopleInDensityCostRadius;
    //record of LocMemory used for density cost
    public LocMemory densityCostActors;

    /*
     * Data variables
     */
    public double rawDensityCost, adjustedDensityCost;


    /*
     * Dummy constructor for setting up comparisons e.g. picking the best bundle
     */
    public Bundle() {
    }

    public Bundle(Point2D.Double here) {

        //Should be passing in a new point, so re-assign
        this.here = here;
        //this.here.x = here.x;
        //this.here.y = here.y;
        //System.out.println("New bundle, location: " + here.x + "," + here.y);

    }

    /*
     * For letting People pass in any extra money they have spare
     */
    public Bundle(Point2D.Double here, double money) {

        //Should be passing in a new point, so re-assign
        this.here = here;
        //this.here.x = here.x;
        //this.here.y = here.y;
        moneyFromBefore = money;

    }

    public void addPotentialEmployer(Firm f, double bestWage) {

        employer = f;
        this.bestWage = bestWage;

        //if(remainingTime < 0) {
        //System.out.println("added potential Employer, remainingTime = " + remainingTime);
        //}

    }

    public void addRemainingTime(double time) {
        remainingTime = time;
    }

//    
    public void addGood(GoodSeller f, double cost) {

        //id, firm, total unit cost
        //uses ID from firm
        GoodsList.add(new Good(ID, f, cost));
        ID++;

    }

    public void addGood(GoodSeller f, double cost, double basePrice) {

        //id, firm, total unit cost
        //uses ID from firm
        GoodsList.add(new Good(ID, f, cost, basePrice));
        ID++;

    }

    public double buyGoods() {

        //for checking prices and costs are tallying
        double cost = 0;
//        System.out.println("goodslist size: " + GoodsList.size());

        for (Good gd : GoodsList) {

            if (gd.optimalChosenAmount > 0) {

                //This is the actual amount the goodseller gets. Need to remember for some models
                gd.contributedInput = gd.optimalChosenAmount * gd.gs.getGoodCost();
                //Buy the optimal number of units. Price is delivery cost per unit * num of units
                gd.gs.buyGood(gd.optimalChosenAmount, gd.price * gd.optimalChosenAmount);
                //cost += gdd.deliveryCostPerUnit * gdd.optimalChosenAmount;
                //optimalWorkgood1 += gdd.optimalChosenAmount;
            }
        }



        return cost;

    }

    /**
     * Called at start of single actor `heard' action: pull time previously
     * added to other actors
     */
    public void removeTimeFromOthers() {

        for (Good gd : GoodsList) {
            if (gd.optimalChosenAmount > 0) {

                gd.gs.removeTime(gd.contributedInput);

            }
        }


    }

    /*
     * For testing: outputs all results to screen
     */
    public void printFullResults() {

        System.out.println("Bundle details: ");

        System.out.println("Here: " + here.x + "," + here.y);

        if (employer == null) {
            System.out.println("No employer found");
        } else {
            System.out.println("Best work option pays: " + bestWage);
        }

        System.out.println(" ");

        System.out.println("List of good1s:");

//        for (GoodDeliveryDetails dg : CESList) {
//
//            System.out.println("Deliverycost: " + dg.deliveryCostPerUnit + ", raw good cost: " + dg.f.goodCost + ", stock left: " + dg.f.getGoodStock());
//            System.out.println("Firm location: " + dg.f.getx() + "," + dg.f.gety());
//            System.out.println("Optimal quantity: " + dg.optimalAmount + ", temp quantity: " + dg.tempAmount);
//            System.out.println("Opt q times delivery cost: " + (dg.deliveryCostPerUnit * dg.optimalAmount));
//
//        }

        System.out.println("==");


        System.out.println(" ");

    }//end method printfullresults

    /**
     * Static method for adding to a Person's bundle array. Supply the bundles,
     * the number of extra ones to add and the width radius for randomising new
     * bundle locations
     *
     * numberOfShortRange: in given radius/width
     *
     * numberModelWide: number to choose from random location anywhere in model
     * space
     */
    public static void addToBundles(ArrayList<Bundle> bundles,
            int numberOfShortRange, double radius, int numberOfModelWide) {

        //One for current location
        bundles.add(new Bundle(new Point2D.Double(0, 0)));

        //if no-one's moving, they only get a Bundle for their current location
        if (!gl.peopleMobile) {
            return;
        }

        //if it's a two-region model run, and People are mobile
        //they only get one for their current location and one for the other region
        if (gl.space == gl.SpaceType.TwoRegion) {

            //add bundle for the other region using some terrible hack
            //I'm actually going to add 3 Bundles because that's easier
            //than hacking the code to make Bundle know which region to pick. 
            //All that will happen is the redundant Bundle will never be optimal
            bundles.add(new Bundle(new Point2D.Double(-gl.width, 0)));
            bundles.add(new Bundle(new Point2D.Double(gl.width, 0)));
            return;
        }

        //otherwise, add bundles with random location determined by spacetype
        for (int n = 0; n < numberOfShortRange; n++) {

            //quite different random sampling method for torus vs line, so 
            //no neat way to do this that I can currently think of
            switch (gl.space) {

                case Line:
                    bundles.add(new Bundle(new Point2D.Double(Randoms.lowHighDouble(-radius, radius), 0)));

                case Torus:
                    bundles.add(new Bundle(Trig.getRandPointInCircleOfRadius(radius)));

            }//end switch

        }//end for int n

        //add model-wide sampled Bundles
        for (int n = 0; n < numberOfModelWide; n++) {

            //quite different random sampling method for torus vs line, so 
            //no neat way to do this that I can currently think of
            switch (gl.space) {

                case Line:
                    bundles.add(new Bundle(new Point2D.Double(gl.width / 2 - (Randoms.nextDouble() * gl.width), 0)));

                case Torus:
                    bundles.add(new Bundle(new Point2D.Double(gl.width / 2 - (Randoms.nextDouble() * gl.width),
                            gl.width / 2 - (Randoms.nextDouble() * gl.width))));

            }//end switch

        }//end for int n

    }//end method addToBundles

    public void cloneLocMemory(LocMemory dlm) {
//        densityCostActors = 
    }
}
