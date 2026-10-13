/*
 * CES, set with rho.
 */
package location2012.econs;

import java.util.ArrayList;
import location2012.utils.Randoms;
import location2012.utils.gl;

/**
 *
 * @author Dan
 */
public class CESOneTypeUtility extends Utility {

    //love of variety
    //0<rho<1
    //As rho -> 0, consumer is better off with a mix of goods.
    public static double rho;
    double CEStot;
    double top, bottom;
    double[] goods;

    public CESOneTypeUtility(double rho) {

        this.rho = rho;

    }

    @Override
    /**
     * Greedy version: good amount guesses coming in from HillClimb
     */
    public double giveUtility(ArrayList<Good> goods) {

        //return (o.giveUtility(goods));
        CEStot = 0;
        //add the quantities, with exponent
        for (Good g : goods) {

            CEStot += Math.pow(g.chosenAmount, rho);

        }

        //raise the lot to 1/rho
        return Math.pow(CEStot, (1 / rho));

    }

    @Override
    /**
     * Constrained version: utility is worked out here using analytic constrained optima.
     * The with-good-price lagrangian is worked out in Brakman et al, New Introduction to GE, p.94-5.
     * I've adapted that, just re-arranging to use rho rather than epsilon.
     *
     * @param b
     * @return
     */
    public void giveUtility(Bundle b) {
        
        //Need to calculate demand amount for each detected good:
        goods = new double[b.GoodsList.size()];

        if (rho < 1) {

            //Got a numerator and denominator to calculate. Denominator will require iterating and summing over all goods
            for (int i = 0; i < goods.length; i++) {

                bottom = 0;
                top = Math.pow(b.GoodsList.get(i).price, (1 / (rho - 1))) * b.bestWage;

                //sum for prices of all other goods. Not sure if I need to leave out the one we're working out for... Can't see how you could analytically, so I guess not
                for (Good g : b.GoodsList) {
                    bottom += Math.pow(g.price, (rho / (rho - 1)));
                    //this is a 'noisy' version
//                bottom += Math.pow(g.price + (g.price * ((Randoms.nextDouble()-0.5)*gl.noiseUtil)), (rho / (rho - 1)));
                }

                goods[i] = top / bottom;

//            b.GoodsList.get(i).optimalChosenAmount = goods[i];
                //noisy version
                b.GoodsList.get(i).optimalChosenAmount = goods[i] + (goods[i] * ((Randoms.nextDouble() - 0.5) * gl.noiseUtil));

//            System.out.println("goodsi: " + goods[i] + ", with noise: " + goods[i] + (goods[i] * ((Randoms.nextDouble()-0.5)*gl.noiseUtil)));

                //can add this now...
                b.maxUtility += Math.pow(goods[i], rho);

            }//end for i


            //take final power for utility
            b.maxUtility = Math.pow(b.maxUtility, (1 / rho));
//        System.out.println("b.maxUtility: "+ b.maxUtility);

        } else {
            //assume rho is one if it is more than one.
            double cheapest = 999999;
            int cheapestID = -1;

            for (Good gd : b.GoodsList) {
                if (gd.price < cheapest) {
//                    gd.optimalChosenAmount = cheapest * b.bestWage;
                    cheapestID = gd.id;
                    cheapest = gd.price;
                }
            }

//            System.out.println("cheapest: " + cheapestID);
            //got cheapest. Set optimal wotsits etc.
            for (Good gd : b.GoodsList) {
                if (gd.id == cheapestID) {
                    gd.optimalChosenAmount = gd.price * b.bestWage;
                    b.maxUtility = gd.optimalChosenAmount;
                }
            }

        }//end if else

    }

    @Override
    public void testUtility() {
    }
}
