/*
 * Holds gene data for the OptimiseGenetic class. Each gene is just a random number
 * between zero and 1, one for each good under consideration.
 */

package location2012.econs;

import location2012.utils.Randoms;

/**
 *
 * @author Dan
 */
public class Chromosome {
    
    public double[] genes;
    
    public Chromosome(int geneNumber) {

        //set up one gene for each good
        genes = new double[geneNumber];

        //randomise values when chromosome first created
        for(double d : genes) {

            d = Randoms.nextDouble();

        }

    }

}
