/*
 * Take an ArrayList of actions and, when shouted, shout only one per iteration 
 */
package location2012.actiontools;

import location2012.observe.Audible;
import location2012.observe.Listener;
import location2012.observe.ShoutEvent;
import location2012.observe.Shouter;
import java.util.ArrayList;
import location2012.Actor;
import location2012.Firm;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class OnlyOnePerDayActorListener implements Listener {

    ArrayList<Actor> incoming;
    ArrayList<Actor> outGoing;
    //pot to permutate from - a copy so actions can be dropped out one by one
    ArrayList pot = new ArrayList();
    //for pulling out and doing things to!
    //p.s. not currently needed; can call method on removed object
    //Action action;
    int weight;
    //testing permute. Count where actor is in order, use to calculate gap
    int count = 0, pos, yesterdayPos = 0;

    /*
     * Take in: audible, call value, and array of actions to permute
     * each time I'm called
     */
    public OnlyOnePerDayActorListener(Audible a, double val, ArrayList<Actor> act) {

        incoming = act;

        //p.p("size of actions in perm:"+actions.size() );

        //register with timeline
        giveShouterTo(a, val);

    }

    /*
     * Take in: audible, call value, and array of actions to permute
     * each time I'm called
     * This one can also take in a weight
     */
    public OnlyOnePerDayActorListener(Audible a, double val, ArrayList<Actor> act, int weight) {

        incoming = act;

        //p.p("size of actions in perm:"+actions.size() );

        //register with timeline
        giveShouterTo(a, val);
        setWeight(weight);

    }

    public int getWeight() {

        return weight;

    }

    public void giveShouterTo(Audible a, double val) {

        a.registerShouter(new Shouter(this, val));

    }

    /*
     * When I hear a call, permutate all actions randomly
     *
     * Just changed this to permute whole Actor array
     * then call actions in order.
     * Same result, plus randomises array for other parts of the model
     */
    public void heard(ShoutEvent s) {

        outGoing = (ArrayList<Actor>) incoming.clone();

//        outGoing = RandPermuteArray.mix(outGoing);

//        Firm f = (Firm) outGoing.get(count++);

//        for (Actor a : outGoing) {

//            if (a.getClass().getSimpleName().equals("Firm")) {
//                f = (Firm) a;
//
//            }

        outGoing.get(count++).actions.get(0).heard(new ShoutEvent(0));

        System.out.println("Day: " + gl.day + ", shouted firm: " + count);

        //do permute order test. Will be zero if first on list
//            a.yesterdayPermutePos = a.permutePos;
//            a.permutePos = count;
//            count++;

//        }

        count = (count == outGoing.size() ? 0 : count);

    }

    public void setWeight(int weight) {

        this.weight = weight;

    }
}
