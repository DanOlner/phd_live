/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012;

import location2012.econs.GoodSeller;
import location2012.econs.ProductionShell;
import location2012.utils.Randoms;
import location2012.utils.gl;
import location2012.utils.gl.GoodType;

/**
 *
 * @author User
 */
public class Firm extends Actor implements GoodSeller {

    //index of type of good being made
    //public int goodType;
    public GoodType goodType;
    //true if I make good, false otherwise
    public boolean makingGood1;
    //money
    public double money;
    //prices
    public double goodCost, wage;
    //quantities in stock
    public double stock, stockTarget;
    //variables for keeping track of change, for firm's decisions also
    public double yesterdayWageOffer, yesterdayGoodCost, goodCostDiffBetweenDays;
    public double yesterdayMoney;
    public double yesterdayStock, stockDiffBetweenDays, yesterdayContributedTime;
    //Equivalent of 2nd derivative of stock (stockDiffBetweenDays being first
    public double yesterDayStockDiffBetweenDays, stockSecondDeriv;
    public double stockMinusStockDifference;
    //record of income difference between days, for data ouput
    public double dailyIncomeBalance;
    //delivery cost of goods; just one number for all at the moment
    //number is per per unit distance
    public double deliverycost;
    public double commuteCost;
    //total value of current contributed labour time
    public double currentContributedTime = 0;
    //Production level - raw level, should equilibriate to show how much is being produced
    //Per day
    public double productionLevel = 0;
    //counting in and out workers - should equal zero when they're removed
    int inout = 0;
    //data markers: have I shifted any stock or hired any labour?
    //if not, I don't want to include this firm's info in data averages
    public boolean shiftedStock = false, hiredLabour = false;
    //counts
    public int goodBought = 0;
    //for tracking how much money actually comes into the firm
    //Stored each day. Reset in the bucket - encapsulation enschmapsulation.
    public double revenue = 0, yesterdayRevenue = 0;
    public double revenueDiff, costDiff;
    //for recording total outgoings between turns
    public double costs = 0, yesterdayCosts = 0;
    //Just used for some mean income difference finding actions.
    public double meanIncomeDifference, avMID;
    //Hacks
    //If transmission...
    public double Output_per_unitTime;
    //Misc
    Firm f;
    double meanPriceOfOtherFirms;
    int counter = 0;
    //markUp is the amount the Firm wants to attempt to get
    //movingMarkUpTarget is the actual stock level they'll be aiming for based on this
    public double markUp, movingMarkUpTarget = 0;
    //Sum of all price fluctuations
    //For detecting when oscillations have died down
    //Used by action as a trigger to act so it can minimise interference to action signal
    //Will be reset by the action
    public double priceFlux = 0, lastGoodCost, goodCostDiff, lastGoodCostDiff, lastGoodCostAtMarkupChange;
    int waitNumber, waitCounter;//minimum wait before reverting back to priceFlux liveband test
    int periodicityCounter = 0, periodicity = -1;
    double goodCostAtLastKnownPeriod;//store value for cost at the last point stock crossed the target line
    double markUpAtLastKnownPeriod;
    //
    double adjustCostAmount = 5;//for testing adjustment of cost change based on periodicity
    public int countPositiveGoodCostChange = 0;//checking for runaway prices, indicating failure to hit stock target
    //Demand for my good today; hacked just for single-Person CES test
    public double demandFromOnePersonForCESTest;

    //Set up dummy firm's values
    public Firm(int ID) {

        super(ID);

        setVars();

    }

    private void setVars() {

        waitNumber = gl.people.size() * 2;

        //starting quantity of money in whole economy is thus the same as the
        //number of firms
        money = 1000;

        double costRandFactor = 5;
        goodCost = (Randoms.nextDouble() * costRandFactor) + 1;
//        goodCost = (Randoms.nextDouble() * costRandFactor) + 0.1;
//        goodCost = (Randoms.nextDouble() * 10);

        goodCost = 3;

        //Don't confuse with 'delivery cost per unit' which is the total cost
        //per unit, including space costs
        //This is how it's used:
        //price = f.getGoodCost() + (f.getGoodCost() * f.getDeliveryCost() * distance)
        //note deliverycost in proportion to good cost; could be in proportion to other things like weight
        deliverycost = gl.deliveryCost;
        commuteCost = gl.commuteCost;
//        goodCost = gl.good;
        wage = gl.wage;

        if (gl.markUpSingleFirmOnly) {
            //cos firm ID indexing starts after people indexing
            if (ID == gl.people.size()) {
                markUp = gl.markUp;
            }
        } else {
            markUp = gl.markUp;
        }

//        goodStock = 100;
        if (gl.transmissionFirms) {
            stock = 0;
        } else {
            stock = 100000;
        }


        //set 'yesterdays' so there's no zero-starting comparison
        yesterdayStock = stock;

        yesterdayContributedTime = currentContributedTime;
//        yesterdayMoney = money;
//        yesterdayGood1Price = good1cost;
//        yesterdayGood2Price = good2cost;
//        yesterdayWageOffer = wageoffer;

        //hacking adjustcostamount randomise val



//        double adjustCostRandFactor = 100;
//        adjustCostAmount = (Randoms.nextDouble() * costRandFactor) + 10;
        
        
        

    }

    /*
     * Later, firms will use this to keep track of how the wage offer is doing
     */
    public double getWageOffer() {

        //make sure I actually have the capital to cover the wage offer
        if (wage <= money) {
            //check if wages have been fixed
            if (gl.peopleFixWage == 0) {
                return wage;
            } else {
                return gl.peopleFixWage;
            }

        } else {

            //System.out.println("returning zero for wageoffer");
            if (gl.peopleFixWage == 0) {
                return 0;
            } else {
                return gl.peopleFixWage;
            }
        }

        //testing allowing firms to go into negative capital
        //return wageoffer;

    }

    public double getGoodCost() {

        return goodCost;

    }

    public void setGoodCost(double cost) {
        goodCost = cost;
    }

    public double getGoodStock() {
        return stock;
    }

    //For getting stock - which good indicated by passed int
    /*
     public double getGoodStock(int num) {

     switch (num) {
     case 1:
     return good1stock;
     case 2:
     return good2stock;
     }

     return 0;
     }
     */
    public double getDeliveryCost() {
        return deliverycost;
    }

    public void setDeliveryCost(double d) {
        deliverycost = d;
    }

    //for gettin an amount of stock, which one indicated by int
    //plus units bought
    //I should double check, but the prices should work out, since this
    //firm has just been queried about the same
    //Will use actual price of goods to buy the stuff - should work out the same
    @Override
    public void buyGood(double units, double totalPayment) {

        //This won't work - doesn't take account of delivery cost. Not problem of firm getting delivery cost
        //System.out.println("In firm, buying goods. Payment given: " + totalPayment + ", cost according to firm: " + (units * goodCost));

        //for single firm test, store amount of stock sold
        demandFromOnePersonForCESTest = units;

        if (gl.firmsGoodsFlowOn) {
            stock -= units;
        }

        if (gl.firmsMoneyFlowOn) {
            if (gl.goodSellersGetDeliveryCost) {
//                System.out.println("in firm buyGood, firm gets deliverycost. Money now: " + money + ", payment: " + totalPayment);
                money += totalPayment;
            } else {
//                System.out.println("in firm buyGood, firm gets deliverycost. Money now: " + money + ", payment: " + (units * goodCost));
                money += (units * goodCost);
                //System.out.println("units * goods cost: " + (units * goodCost) + ", total Payment: " + totalPayment);
            }
        }

        goodBought++;
        //System.out.println("in Firm: buying good " + index + ", amount: " + units + ", stock left: " + good1stock);
        //for tracking income: since incomings and outgoings can cancel, we won't know what this is without separately recording it

        //If this is true, they get the delivery cost paid by consumers
        //Otherwise, their income from a sale is just their own good price times units bought
        if (gl.goodSellersGetDeliveryCost) {
            revenue += totalPayment;
        } else {
            revenue += (units * goodCost);
            //System.out.println("units * goods cost: " + (units * goodCost) + ", total Payment: " + totalPayment);
        }

        //Quick hacks to get Firms working with the transmission model

        if (gl.setStockToZero) {
            stock = 0;
        }

        if (gl.transmissionFirms) {
            transmissionFirmsAction(units, totalPayment);
        }






    }//end method buyGoods

    private void transmissionFirmsAction(double units, double totalPayment) {

        //MARKUP PARAMETER SWEEP HACK
        //ID = people size because firm IDs start after that and we want the 1st firm
//        if (gl.day % 200 == 0 && ID == gl.people.size()) {
//
//            lastGoodCostAtMarkupChange = goodCost;
//            markUp += 0.002;
//
//        } else if (gl.day % 200 == 0) {
//            lastGoodCostAtMarkupChange = goodCost;
//        }

        //hack single firm markup change
//        if(gl.day < 5 && ID - gl.people.size() == gl.LOOKATFIRM) {
//            markUp = 0.1;
//        }

        yesterdayStock = stock;

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

            productionLevel = ProductionShell.giveOutput(currentContributedTime);
//            System.out.println("prod level in Firm: " + productionLevel);

//            EoS_per_person = ProductionShell.giveOutput(currentContributedTime) / gl.people.size();
            Output_per_unitTime = ProductionShell.giveOutput(currentContributedTime) / currentContributedTime;

//            System.out.println("Current EoS, output per actor: " + EoS_per_person + ", stock: " + stock + ", currentContributedTime: " + currentContributedTime);
//            if(currentContributedTime < 0) {
//                System.out.println("time < 0: " + currentContributedTime);
//            }

        }

        //1st derivative, sorta. Not exactly sure I can call it that.
        //Used below in the damping equation
        yesterDayStockDiffBetweenDays = stockDiffBetweenDays;
        stockDiffBetweenDays = stock - yesterdayStock;

        stockSecondDeriv = stockDiffBetweenDays - yesterDayStockDiffBetweenDays;

//        stockMinusStockDifference = stock - stockDiffBetweenDays;

        //If stock is +, price is reduced.
        //If stock is - , price increased.
        //If stock is close to zero, change is very small.

        double costChangeAmount;

        //if targeting a stock level
        if (gl.stockTarget && ID != gl.LOOKATME) {
            //Note: stock*stock is positive. So doing this, then test
//            double costChangeAmount = (stock * stock) / 100;


            //Aiming for this to be zero: so both stock level itself
            //And its first derivative, so it's equilibriating production
            //and keeping stable            
//            double stockAv = (stock + stockDiffBetweenDays);
//            stockTarget = stock;
//            double stockAv = stock + stockDiffBetweenDays + stockSecondDeriv;

            movingMarkUpTarget += markUp;

            //test gently increasing markUp
//            markUp += Randoms.nextDouble() * 0.00001;
//            stockTarget = stock + stockDiffBetweenDays + stockSecondDeriv;
            stockTarget = stock + stockDiffBetweenDays + stockSecondDeriv - movingMarkUpTarget;
//            stockTarget = stock + stockDiffBetweenDays - movingMarkUpTarget;

//            stockTarget = stock;

//            int periodicityTarget = 50;
//
//            //adjust based on periodicity
//            if (periodicity != -1) {
//                if (periodicity < periodicityTarget) {
//                    adjustCostAmount *= 1.1;
////                    System.out.println("adjustCostAmount: " + adjustCostAmount);
//
//                } else if (periodicity > periodicityTarget + 1) {
////                    System.out.println("ping?");
//                    adjustCostAmount *= 0.1;
////                    System.out.println("adjustCostAmount: " + adjustCostAmount);
//                }
//            }


//            adjustCostAmount = (adjustCostAmount < 0.5 ? 0.5 : adjustCostAmount);
//            System.out.println("adj: " + adjustCostAmount);


            adjustCostAmount = 100;
//            adjustCostAmount = 0.5 + (100 * gl.EoS_Curve);
//            costChangeAmount = ((stockAv) * (stockAv)) / 10;
//            costChangeAmount = ((stock) * (stock)) / 100;
//            costChangeAmount = (Math.abs(stockTarget) / 5);
            costChangeAmount = (Math.abs(stockTarget) / adjustCostAmount);
//            costChangeAmount = Math.abs(stock)/100;
//            costChangeAmount = stockTarget/5;

            //Correct polarity to change stock in right direction
            //Easiest way to do this since the change amount may end up
            //positive from squaring
            costChangeAmount = (stock < movingMarkUpTarget ? costChangeAmount : -costChangeAmount);


            //for keeping record of price fluctuations between actions
            lastGoodCost = goodCost;
            goodCost += costChangeAmount;
//            goodCost += 0.001;

//            if(ID == gl.LOOKATFIRM + gl.people.size()) {
//                goodCost = 1;
//            }

//            System.out.println("good Cost: " + goodCost);

            if (goodCost < 0) {
                System.out.println("Good cost < 0");
                goodCost = goodCostAtLastKnownPeriod;
//                markUp = 0;
            }


            lastGoodCostDiff = goodCostDiff;
            goodCostDiff = goodCost - lastGoodCost;

            //test pos only for now
            if (goodCostDiff > 0) {
                countPositiveGoodCostChange++;
                //reset if the other way and keep price
            } else {
                goodCostAtLastKnownPeriod = goodCost;//keep a record, a rough target to return to
                markUpAtLastKnownPeriod = markUp;
                countPositiveGoodCostChange = 0;
            }

            //arbitrary check for runaway prices
            if (false) {
//            if (countPositiveGoodCostChange > 200) {
                countPositiveGoodCostChange = 0;
                goodCost = goodCostAtLastKnownPeriod;

                movingMarkUpTarget -= markUp;
                markUp = markUpAtLastKnownPeriod;
//                markUp = 0;
                System.out.println("correcting to: " + goodCost);
            }

            priceFlux = Math.abs(goodCost - lastGoodCost);

            try {
                //Testing using liveband trigger where price flux is within a bound
                if (priceFlux < 0.00000001 && waitCounter == 0) {

                    //reset wait counter: use as boolean also:
                    //"waiting period has passed? y/n"
                    waitCounter = waitNumber;

                    actions.get(1).heard(null);

                    //Wait before allowing markup action to be called again.
                    //Will also test price flux again but need to wait for a time
                    //to allow People's spending to respond
                } else if (waitCounter > 0) {
                    waitCounter--;
//                    System.out.println("wait: " + waitCounter);
                }
            } catch (Exception e) {
                //System.out.println("This Firm has no markup action");
            }

        }



    }

    private double findMeanPriceOfOtherFirms() {

        meanPriceOfOtherFirms = 0;

        for (Actor a : gl.firms) {

            f = (Firm) a;

            if (f.ID != ID) {
                meanPriceOfOtherFirms += f.goodCost;
            }

        }

        //minus one, don't include me
        return meanPriceOfOtherFirms / (gl.firms.size() - 1);

    }

    //Person accepts wage offer, contributes time
    //returns money earned
    public double acceptWageOffer(double time) {

        //set to false at the end of FirmAct
        //If one person accepts work, I have hired since my last action.
        hiredLabour = true;

        //System.out.println("wageOffersAccepted: " + wageOffersAccepted);

        //add the contribution
//        System.out.print(ID + ", Contribtime from " + currentContributedTime);
        currentContributedTime += time;
//        System.out.println(" to " + currentContributedTime);
        inout++;


        //goodStock += (Productivity.giveOutput(currentContributedTime) * (time / currentContributedTime));
        if (gl.firmsGoodsFlowOn) {
            stock += (ProductionShell.giveOutput(currentContributedTime) * (time / currentContributedTime));
        }
//        System.out.println("In acceptWork. currentContributedTime: " + currentContributedTime + ", goodStock: " + goodStock
//                + ", time input: " + time + ", incd by: " + (ProductionShell.giveOutput(currentContributedTime) * (time / currentContributedTime)));
        //costs += (wageoffer * time);
        // System.out.println(", after: " + good1stock);

        if (gl.firmsMoneyFlowOn) {
//            System.out.println("in Firm acceptWork. Money now = " + money + ", wage being paid is = " + (wageoffer * time));
            money -= (wage * time);
        }

        //record costs going out
//        System.out.print("Firm costs changing: " + costs);
        costs += (wage * time);
//        System.out.println(", " + costs);

        return wage * time;


    }

    public GoodType getGoodType() {
        return goodType;
    }

    public void setGoodType(GoodType gt) {
        goodType = gt;
    }

    /*
     * For People to remove their previously contributed time
     */
    public boolean removeTime(double time) {

        currentContributedTime -= time;
        inout--;
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

            return false;

        } else {
            return true;
        }

    }
}
/**
 * Cuttinz
 */
//            while (goodCost + costChangeAmount < 0) {
//                System.out.println("Count: " + counter++ + ", ID: " + ID
//                        + ": attempt to set negative price failed. Would have been: "
//                        + (goodCost + costChangeAmount));
//
//                costChangeAmount *= 0.9;
//            }
//            if (goodCost + costChangeAmount < 0) {
//                                System.out.println("Count: " + counter++ + ", ID: " + ID
//                        + ": attempt to set negative price failed. Would have been: "
//                        + (goodCost + costChangeAmount));
//                goodCost = (Randoms.nextDouble() * 2) + 1;
//            } else {
//            }
//check for periodicity: if goodCost moved from pos to neg, that's one marker
//        if (lastGoodCostDiff * goodCostDiff < 0) {
//
//            System.out.println(periodicityCounter);
//            periodicity = periodicityCounter;
//            periodicityCounter = 0;
//            goodCostAtLastKnownPeriod = goodCost;//keep a record, a rough target to return to
//
//        } else {
//            periodicityCounter++;
//        }
//
//        //test for correcting price if we haven't reached stock target recently
//        if (periodicity != -1 && periodicityCounter > periodicity * 2) {
//
//            System.out.println("Price gone wrong? Periodicity: " + periodicity
//                    + ", periodicityCounter: " + periodicityCounter);
//
//        }