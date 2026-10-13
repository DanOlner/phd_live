/*
 * Static tools for extracting actions from actors into an ArrayList of
 * Actions
 */

package location2012.actiontools;

import location2012.Person;
import java.util.ArrayList;
import location2012.Actor;

/**
 *
 * @author Olner Dan
 */
public class ACTIONEXTRACT {

    public static ArrayList<Action> pullFirstActionsOut(ArrayList<Actor> actors){

        ArrayList<Action> a = new ArrayList<Action>();

        //Only extracting the first action from each actor
        for(Actor ac : actors) {

            a.add(ac.actions.get(0));

        }

        return a;

    }


}
