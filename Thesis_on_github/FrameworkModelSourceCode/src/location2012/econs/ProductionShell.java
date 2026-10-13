package location2012.econs;

import java.util.ArrayList;

/**
 * For firm's productivity
 *
 * @author Dan
 */
public class ProductionShell {

    public static Production p;

    public ProductionShell(Production p) {

        this.p = p;

    }

   
    public static double giveOutput(double time) {

        return (p.giveOutput(time));
        
    }
//    public static double giveUtility(ArrayList<Good> goods) {
//
//        return (u.giveUtility(goods));
//
//    }
//    //inner class for outputting
//    //private static Output o;
//    //output to return...
//    //private static double output;
//    //love of variety
//    //0<rho<1
//    //As rho -> 0, consumer is better off with a mix of goods.
//    static double rho;
//    static double CEStot;
//
//    public UtilityShell(double rho) {
//
//        //o = new Output(rho);
//        this.rho = rho;
//
//    }
//
//    public static double giveUtility(ArrayList<Good> goods) {
//
//        //return (o.giveUtility(goods));
//        CEStot = 0;
//        //add the quantities, with exponent
//        for (Good g : goods) {
//
//            CEStot += Math.pow(g.chosenAmount, rho);
//
//        }
//
//        //raise the lot to 1/rho
//        return Math.pow(CEStot, (1 / rho));
//
//
//    }
}
