package location2012;

import location2012.econs.GoodSeller;
import location2012.utils.gl.GoodType;

/**
 * Landlord: a fixed-location agent designed to give location a cost. The agent will try and maximise revenue for particular locations in space.
 * They have no other economic role in the model (so far, just dealing with set wages...)
 * @author User
 */
public class LandLord extends Actor implements GoodSeller {

    public GoodType goodType = GoodType.Necessity;
    public double landCost;
    public double startStock = 2, stock;
    public final double deliveryCost = 0;
    //money
    public double money = 0;
    public double income = 0, yesterdayIncome = 0;

    //Set up dummy firm's values
    public LandLord(int ID) {

        super(ID);

        setVars();

    }

    private void setVars() {

        landCost = 0.1;
        stock = startStock;

    }

    public double getGoodCost() {
        return landCost;
    }

    public void setGoodCost(double cost) {

        landCost = cost;

    }

    public double getGoodStock() {
        return stock;
    }

    //for gettin an amount of stock, which one indicated by int
    //plus units bought
    //I should double check, but the prices should work out, since this
    //firm has just been queried about the same
    //Will use actual price of goods to buy the stuff - should work out the same
    public void buyGood(double units, double totalPayment) {

        System.out.print("Landlord buyGood. Stock before buy = " + stock + ", after = ");
        stock -= units;
        System.out.println(stock);
        income += totalPayment;

    }//end method buyGoods

    public double getDeliveryCost() {
        return 0;
    }

    public GoodType getGoodType() {
        return goodType;
    }

    public void setGoodType(GoodType gt) {
        goodType = gt;
    }

    public boolean removeTime(double amount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


}
