package location2012.observe;

/**
 * Shoutevents - passed to Listeners that wanted to be shouted, so they can
 * find out some more details. Have to think this one through rather a lot more...
 *
 * @author Olner Dan
 */
public class ShoutEvent {

    public double heardValue;

    /**
     *
     * @param heardValue
     */
    public ShoutEvent(double heardValue) {

        this.heardValue = heardValue;

    }

    /**
     *
     * @return
     */
    public double getHeardValue() {

        return heardValue;

    }

}
