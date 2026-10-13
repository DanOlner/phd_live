/*
 * Generic interface to allow easy swapping of utility functions when optimising
 */

package location2012.econs;

/**
 *
 * @author Dan
 */
public abstract class Production {

    public abstract double giveOutput(double time);

    public abstract void testOutput();

}
