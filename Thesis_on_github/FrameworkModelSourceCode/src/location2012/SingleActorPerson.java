/*
 * Extension of person, specific for single actor model
 */
package location2012;

import java.util.ArrayList;
import location2012.econs.ContributedTime;
import location2012.econs.GoodSeller;
import location2012.econs.ProductionShell;
import location2012.utils.Randoms;
import location2012.utils.gl;
import location2012.utils.gl.GoodType;

/**
 *
 * @author Dan
 */
public class SingleActorPerson extends Person implements GoodSeller {

    //Total amount of time being contributed at any point
    //Other actors are responsible for adding and removing their own time
    //Set to 1 initially: this 'seeds' everyone with their own backyard production
    //level so that if others collaborate, there's already time being put in.
    public double totalTimeInput;
    //For storing who is contributing time, and how much
    //Update: that's all done through Bundles plus the GoodSeller removeTime method now
//    public ArrayList<ContributedTime> timeContribToMe = new ArrayList<ContributedTime>();
    //A record of who I have contributed time to, so I remove it on my next turn each go.
//    public ArrayList<ContributedTime> timeContribByMe = new ArrayList<ContributedTime>();
    //Used for temporarily storing arrived-at productivity level for when
    //other actors are considering collaboration
    double tempProductivity;
    //used for doing the same with contributed time
    double tempContribTime;
    //Used in various places, including for drawing and data output - my final productivity
    public double productivity;
    //a record of the density cost for the spot I ended up on
    public double chosenDensityCost;
    //production time stuff
    public double currentContributedTime = 0;
    //we want this to stay as close to zero as possible
    public double stock = 0;
//    public double stock = (Randoms.nextDouble() * 2);
    //This is the base goodCost
    public double goodCost = (Randoms.nextDouble() * 2) + 1;
//    public double goodCost = 1;
    //if I'm setting my own price differently, for buying from myself
    public double myGoodCost = (Randoms.nextDouble() * 2) + 1;
//    public double goodCost = 1;
    public double deliverycost;
    //for data
    public double Output_per_unitTime = 0;
    //For checking how much time I'm contributing to myself
    public double timeContributedToMyself = 0;

    public SingleActorPerson(int ID) {
        super(ID);

        deliverycost = gl.deliveryCost;

    }

    //For SingleActorPerson, 'buyGood' is another buying with time. So
    //the time goes into production.
    //I also do the stock adjustment method, changing price, here.
    public void buyGood(double units, double totalPayment) {

        //If this is true, they get the delivery cost paid by consumers
        //Otherwise, their income from a sale is just their own good price times units bought
        if (gl.goodSellersGetDeliveryCost) {

            stock -= units;

            //the input the actor actually receives. I think. This is scrambling my head right now.
            //In this case, totalPayment of input, cos actor gets all space costs too
            currentContributedTime += totalPayment;

            stock += (ProductionShell.giveOutput(currentContributedTime) * ((totalPayment) / currentContributedTime));

        } else {
            stock -= units;
            //System.out.println("units * goods cost: " + (units * goodCost) + ", total Payment: " + totalPayment);
            //the input the actor actually receives. I think. This is scrambling my head right now.
            //units * goodCost is the actual amount of input this actor gets
            currentContributedTime += (units * goodCost);

            stock += (ProductionShell.giveOutput(currentContributedTime) * ((units * goodCost) / currentContributedTime));

//            EoS_per_person = ProductionShell.giveOutput(currentContributedTime) / gl.people.size();
            Output_per_unitTime = ProductionShell.giveOutput(currentContributedTime) / currentContributedTime;

//            System.out.println("Current EoS, output per actor: " + EoS_per_person + ", stock: " + stock + ", currentContributedTime: " + currentContributedTime);
//            if(currentContributedTime < 0) {
//                System.out.println("time < 0: " + currentContributedTime);
//            }

        }

        //If stock is +, price is reduced.
        //If stock is - , price increased.
        //If stock is close to zero, change is very small.

        double costChangeAmount;

        //if targeting a stock level
        if (gl.stockTarget) {
            //Note: stock*stock is positive. So doing this, then test
//            double costChangeAmount = (stock * stock) / 100;

//            costChangeAmount = ((stock) * (stock));
            costChangeAmount = Math.abs(stock);

//            double costChangeAmount = ((stock) * (stock)) / (50 + ((Randoms.nextDouble()*100)));
            //add noise
//            costChangeAmount *= (Randoms.nextDouble() * 10);
//        double costChangeAmount = ((stock-1)*(stock-1))/100;
//        System.out.println("stock = " + stock + ", costchangeamount: " + costChangeAmount);

            //testing price change elasticity impact
            if (ID != gl.LOOKATME) {

                //Don't allow negative prices
                if (stock < 0) {
//                    if (true) {
                    if (goodCost + costChangeAmount > 0) {
                        goodCost += costChangeAmount;
                    } else {
                        System.out.println("attempt to set negative price failed. Would have been: "
                                + (goodCost + costChangeAmount));
                    }
                } else {

//                    if (true) {
                    if (goodCost - costChangeAmount > 0) {
                        goodCost -= costChangeAmount;
                    } else {
                        System.out.println("attempt to set negative price failed. Would have been: "
                                + (goodCost - costChangeAmount));
                    }
                }
            } else {
                System.out.println("ID " + gl.LOOKATME + " goodcost: " + goodCost + ", stock level: " + stock);
            }


//        goodCost -= (stock) / 100;
            //end if stocktarget

//        if (goodCost < 0) {
//            goodCost = (Randoms.nextDouble() * 2) + 1;
//        }

        }



    }//end method buyGood

    public double getDeliveryCost() {
        return deliverycost;
    }

    public double getGoodCost() {
        return goodCost;
    }

    public double getGoodStock() {
        //not needed, but Good needs a number
        return -1;
    }

    public GoodType getGoodType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setGoodCost(double cost) {
    }

    public void setGoodType(GoodType gt) {
    }

    public boolean removeTime(double time) {

        currentContributedTime -= time;
//        inout--;
//        if (currentContributedTime < 0) {
//            System.out.println("Firm's time contributions: less than zero: " + currentContributedTime
//                    + ", inout: " + inout);
//
//            if (currentContributedTime < -0.01) {
//                System.out.println("And it's more than a tiny amount.");
//            }
//        }

//        System.out.println(ID + " contributed Time: " + currentContributedTime);

        //in cases where it's very close to zero, addition and subtraction problems
        //mean it misses. Rather than implement a more complex method to correct that
        //(like the ContributedTime objects in the Backyard model)
        //A simpler way: if the value is just less than zero, make it zero.
        if (currentContributedTime < 0 && currentContributedTime > -0.001) {
            System.out.println("correcting currentContributedTime");
            currentContributedTime = 0;

        }

        if (currentContributedTime < 0) {
            return false;
        } else {
            return true;
        }

    }
}
