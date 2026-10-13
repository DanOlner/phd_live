package location2012.econs;

/**
 * Utility function: returns a value from a mix of two goods
 *
 * @author User
 */
 
public class XXXEngelTwoGoodUtilityxxx {

    //inner class for outputting
    private static Output o;
    //output to return...
    private static double output;

    public XXXEngelTwoGoodUtilityxxx(double ratio) {

        o = new Output(ratio);

    }

    //one-type CES
    /*
    public static double giveUtility(ArrayList<GoodDeliveryDetails> CESgoods) {

        return 0;

    }*/

    public static double giveUtility(double good1, double good2) {

        return (o.giveUtility(good1, good2));

    }

    //inner class
    public class Output {

        //basic ratio of time to output - holds for backyard, is basis for collaboration
        //Keeps them both in proportion
        private double ratio;
        //output for returning
        private double utility;

        public Output(double ratio) {

            this.ratio = ratio;

        }

        /**
         * Return utility based on two good quantities
         *
         * @param time
         */
        public double giveUtility(double good1, double good2) {

            double delta = 0.5; double gamma = 1;
            //Engel's law-type function from Murata
            //u = x^a * (x+y)^(1-a)
            //Note: setting a and y here. They're usually called delta and gamma.

            return (Math.pow(good1,delta) * Math.pow((good2 + gamma),1-delta)) * ratio;

        }
    }//end inner class

}
