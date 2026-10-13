/*
 * One necessity good, homogenous; other type = CES, set with rho. Delta and gamma
 * used for whole function.
 */
package location2012.econs;

import java.util.ArrayList;

/**
 *
 * @author Dan
 */
public class CESPlusNecessityGoodUtility extends Utility {

    //love of variety
    //0<rho<1
    //As rho -> 0, consumer is better off with a mix of goods.
    //delta = split between two goods
    //gamma = entry-point for luxury goods
    double rho, delta, gamma;
    double CEStot, necessityTot;
    //number of good
    int index;

    public CESPlusNecessityGoodUtility(double rho, double delta, double gamma) {

        this.rho = rho;
        this.delta = delta;
        this.gamma = gamma;

    }

    @Override
    public double giveUtility(ArrayList<Good> goods) {

        CEStot = 0;
        necessityTot = 0;
        //add the quantities, with exponent
        for (Good g : goods) {

            switch (g.gs.getGoodType()) {

                case Necessity:

                    necessityTot += g.chosenAmount;

                    break;

                case Luxury:

                    CEStot += Math.pow(g.chosenAmount, rho);

            }


        }

        //raise the CES to 1/rho
        CEStot = Math.pow(CEStot, (1 / rho));

        System.out.println("in utility: necessityTot = " + necessityTot + ", CEStot" + CEStot);
        System.out.println("Cobb D values: " + (Math.pow(necessityTot, delta)) + "," + Math.pow(CEStot + gamma, (1 - delta)));

        //return the Engel function for both
        return (Math.pow(necessityTot, delta) * Math.pow(CEStot + gamma, (1 - delta)));


    }

    @Override
    /**
     * Constrained version: utility is worked out here using analytic constrained optima
     *
     * @param b
     * @return
     */
    public void giveUtility(Bundle b) {
    }

    @Override
    public void testUtility() {
    }
}
