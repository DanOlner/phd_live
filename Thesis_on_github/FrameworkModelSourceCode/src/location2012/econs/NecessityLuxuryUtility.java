/*
 * One necessity good type, one luxury good type.
 */
package location2012.econs;

import java.util.ArrayList;
import location2012.Firm;
import location2012.utils.gl;

/**
 *
 * @author Dan
 */
public class NecessityLuxuryUtility extends Utility {

    //love of variety
    //0<rho<1
    //As rho -> 0, consumer is better off with a mix of goods.
    //delta = split between two goods
    //gamma = entry-point for luxury goods
    double delta, gamma;
    double LuxuryTot, necessityTot;
    //for constrained optimisation
    Good LuxCheapest, NecCheapest;
    //constrained optima amounts
    double F, M;
    //number of good
    int index;

    public NecessityLuxuryUtility(double delta, double gamma) {

        this.delta = delta;
        this.gamma = gamma;

        LuxCheapest = new Good();
        NecCheapest = new Good();

    }

    @Override
    /**
     * Greedy version
     */
    public double giveUtility(ArrayList<Good> goods) {

        LuxuryTot = 0;
        necessityTot = 0;
        //add the quantities, with exponent
        for (Good g : goods) {

            switch (g.gs.getGoodType()) {

                case Necessity:

                    necessityTot += g.chosenAmount;

                    break;

                case Luxury:

                    LuxuryTot += g.chosenAmount;

            }

        }


        //p.p("in utility: necessityTot = " + necessityTot + ", LuxuryTot" + LuxuryTot);
        //p.p("Cobb D values: " + (Math.pow(necessityTot, delta)) + "," + Math.pow(LuxuryTot + gamma, (1 - delta)));

        //return the Engel function for both
        return (Math.pow(necessityTot, delta) * Math.pow(LuxuryTot + gamma, (1 - delta)));


    }

    /**
     * Constrained version, set best utility directly in Bundle
     *
     * @param b
     * @return
     */
    public void giveUtility(Bundle b) {

        //More than one of each good may be in the Bundle, so we need to pick the cheapest of each
        //If there isn't one of each, this function cannot work
        LuxCheapest.price = 999999999;
        NecCheapest.price = 999999999;

        for (Good g : b.GoodsList) {

            switch (g.gs.getGoodType()) {

                case Necessity:

                    if (g.price < NecCheapest.price) {
                        NecCheapest = g;
                    }

                    break;

                case Luxury:

                    if (g.price < LuxCheapest.price) {
                        LuxCheapest = g;
                    }

            }//end switch

        }//end for Good g

        //Now we can work out the optimal quantities of each, then plug back into utility
        //constrained optima are:
        //F = (delta(Y + Pm*gamma))/Pf
        //M = (Y(1-delta)/Pm) - delta*gamma
        //See modelmaker2011 section 'optimising optimising'
        F = (delta * (b.bestWage + (LuxCheapest.price*gamma))) / NecCheapest.price;
        M = ((b.bestWage * (1 - delta)) / LuxCheapest.price) - (delta * gamma);


        if (M < 0) {
//            System.out.print("**** M IS NEGATIVE ****: " + M);
            //reassign M's budget spend.
            double Mspend = M * LuxCheapest.price;
            //This will be a negative spend: F will be reduced
//            System.out.println(", F before change: " + F);
            F += (Mspend * NecCheapest.price);
            //set M to zero
            M = 0;
//            System.out.println(",reset to zero. Changed F: " + F);
        }

        
        //set chosenAmount...
        NecCheapest.optimalChosenAmount = F;
        LuxCheapest.optimalChosenAmount = M;

        //set best utility directly in Bundle
        //U=F^delta * (M + gamma)^1-delta
        b.maxUtility = (Math.pow(F, delta) * Math.pow(M + gamma, (1 - delta)));


    }

    /*
     * Test
     */
    public void testUtility() {

        //values to sweep
        //budget here just means total goods quantity to sweep
        double maxBudget = 1;

        double best = 0, best1 = 0;
        ArrayList<Good> goodsTest = new ArrayList<Good>();
        //goodsTest.add(new Good(1, 1, 1));
        goodsTest.add(new Good(0, new Firm(0), best1));
        goodsTest.add(new Good(1, new Firm(1), best1));
        goodsTest.add(new Good(2, new Firm(2), best1));
        goodsTest.add(new Good(3, new Firm(3), best1));
        //goodsTest.add(new Good(2, 1, 1));

        goodsTest.get(0).gs.setGoodType(gl.GoodType.Necessity);
        goodsTest.get(1).gs.setGoodType(gl.GoodType.Luxury);
        goodsTest.get(2).gs.setGoodType(gl.GoodType.Necessity);
        goodsTest.get(3).gs.setGoodType(gl.GoodType.Luxury);


        for (double i = 0; i < maxBudget; i += 0.000001) {

            goodsTest.get(0).chosenAmount = i / 2;
            goodsTest.get(1).chosenAmount = (maxBudget - i) / 2;
            goodsTest.get(2).chosenAmount = i / 2;
            goodsTest.get(3).chosenAmount = (maxBudget - i) / 2;

            if (UtilityShell.u.giveUtility(goodsTest) > best) {
                best = UtilityShell.u.giveUtility(goodsTest);

                //System.out.println("current best: " + best);
                best1 = i;

            }//end if

        }//end for i

        //best1 = Math.round(best1);

        System.out.println("Utility test. Utility was " + best + ", opt 1 = " + best1 + ", opt2 = " + (maxBudget - best1));

    }
}
