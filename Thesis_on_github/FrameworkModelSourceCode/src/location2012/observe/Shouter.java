package location2012.observe;

/**
 * Shouter: used by Listeners to define when they want notifying of a certain
 * type of value change - as yet, rather too inflexible, but a start. <p>Passes
 * ShoutEvents back to the registered Listener
 *
 * Implements Comparable so Shouters can be ordered by weight and so called in a
 * defined order
 *
 * @author Olner Dan
 */
public class Shouter implements Comparable {

    public Listener lstr;
    public double value;
    //for stopping the timeLine from calling it; used for pausing
    public boolean silenceable = false, silenced = false;

    /**
     *
     * @param t
     * @param value
     */
    public Shouter(Listener t, double value) {

        this.lstr = t;
        this.value = value;

    }

    public Shouter(Listener t, double value, boolean silenceable) {

        this.lstr = t;
        this.value = value;
        this.silenceable = silenceable;

    }

    //Method called by an audible class when triggered
    //Audible passes in a ShoutEvent;
    //Tells listener thusly
    /**
     *
     * @param s
     */
    public void shout(ShoutEvent s) {

        if (!silenced) {
            lstr.heard(s);
        }

    }

    /**
     * Returns true if now silenced
     *
     * @return
     */
    public boolean toggleSilenced() {

        if (silenceable) {
            silenced = (silenced ? false : true);            
        }

        return silenced;

    }

    public int compareTo(Object arg0) {

        int weight1 = lstr.getWeight();
        int weight2 = ((Shouter) arg0).lstr.getWeight();

        if (weight1 > weight2) {
            //p.p("returned 1");
            return 1;
        } else if (weight1 < weight2) {
            //p.p("returned -1");
            return -1;
        } else {
            return 0;
        }

    }
}
