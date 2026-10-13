package location2012.econs;

import location2012.utils.gl.GoodType;

/**
 * Needs to be implemented by any class that's selling a good that needs to enter into a utility function. (Firm, Landlord)
 * @author Dan
 */
public interface GoodSeller {

    public void buyGood(double units, double totalPayment);

    public double getGoodCost();

    public double getGoodStock();

    public void setGoodCost(double cost);

    public double getDeliveryCost();

    public GoodType getGoodType();

    public void setGoodType(GoodType gt);

    public boolean removeTime(double amount);

}
