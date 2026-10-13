package location2012.econs;

import java.awt.geom.Point2D;
import location2012.Firm;

/**
 * For storing the best wage and a ranking of the best prices
 * worked out from a particular point relative to a person
 * 
 * Details worked out in the FindBestWageAndPrices method of PersonAction
 *
 * @author Dan Olner
 */
public class BundleSA extends Bundle {

    /*
     * Dummy constructor for setting up comparisons e.g. picking the best bundle
     */
    public BundleSA() {
    }

    public BundleSA(Point2D.Double here) {

        //Should be passing in a new point, so re-assign
        this.here = here;
        //this.here.x = here.x;
        //this.here.y = here.y;
        //System.out.println("New bundle, location: " + here.x + "," + here.y);

    }

    /*
     * For letting People pass in any extra money they have spare
     */
    public BundleSA(Point2D.Double here, double money) {

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

    public double buyGoods() {

        //for checking prices and costs are tallying
        double cost = 0;

        for (Good gd : GoodsList) {
            if (gd.optimalChosenAmount > 0) {
                //Buy the optimal number of units. Price is delivery cost per unit * num of units
                gd.gs.buyGood(gd.optimalChosenAmount, gd.price * gd.optimalChosenAmount);
                //cost += gdd.deliveryCostPerUnit * gdd.optimalChosenAmount;
                //optimalWorkgood1 += gdd.optimalChosenAmount;
            }
        }



        return cost;

    }

    /**
     * Called at start of single actor `heard' action: pull time previously added
     * to other actors
     */
    public void removeTimeFromOthers() {

        for (Good gd : GoodsList) {
            if (gd.optimalChosenAmount > 0) {

                gd.gs.removeTime(gd.basePrice);

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

    }
}
