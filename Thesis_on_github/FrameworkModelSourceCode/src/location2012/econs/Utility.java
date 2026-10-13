/*
 * Generic interface to allow easy swapping of utility functions when optimising
 */
package location2012.econs;

import java.util.ArrayList;

/**
 *
 * @author Dan
 */
public abstract class Utility {

    /**
     * ArrayList of goods coming in means greedy algorithm produced good amount guesses
     */
    public abstract double giveUtility(ArrayList<Good> goods);

    /**
     * Bundle coming in means constrained optimisation being used.
     * Sets best utility directly in the Bundle so no need to return value
     */
    public abstract void giveUtility(Bundle b);

    public abstract void testUtility();
}
