/*
 *
 */
package location2012.actiontools;

import location2012.utils.Randoms;
import java.util.ArrayList;

/**
 *
 * @author Olner Dan
 */
public class RandPermuteArray {

    //ArrayList<Actor> actors;
    //pot to permutate from - a copy so actions can be dropped out one by one
    static ArrayList pot = new ArrayList();
    
    
    public static ArrayList mix(ArrayList array) {

        //pot.addAll(actions);
        pot = new ArrayList();

        //While there's still stuff in the array...
        while (!array.isEmpty()) {

            //Pick a random entry from the array
            pot.add(array.remove(Randoms.rangeInt(array.size())));

        }

        return pot;

    }    
}
