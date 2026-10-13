package location2012.econs;

import java.util.ArrayList;

/**
 * Static shell for accessing utility
 *
 * @author Dan
 */
public class UtilityShell {

    public static Utility u;

    public UtilityShell(Utility u) {

        this.u = u;

    }

    /**
     * ArrayList of goods coming in means greedy algorithm produced good amount guesses
     */
    public static double giveUtility(ArrayList<Good> goods) {

        return (u.giveUtility(goods));

    }

    /**
     * Bundle coming in means constrained optimisation being used
     */
    public static void giveUtility(Bundle b) {

        u.giveUtility(b);

    }

}
