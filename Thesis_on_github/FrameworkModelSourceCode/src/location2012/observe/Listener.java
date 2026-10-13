/**
 * Interface: listener - used by anything that wants to be informed about
 * parameter value changes. Uses Shouter classes to do this - these are
 * passed to the timeline, which does the job of informing.
 */
package location2012.observe;

//import java.util.ArrayList;


/**
 *
 * @author Olner Dan
 */
public interface Listener {
    
    
    /*
     * Which audible(s) will this listener give Shouters to?
     * In e.g. Calculator this is called from the constructor
     * so the audible and value (along with a Container and Colourmap)
     * can be passed in when instantiating 
     */
    /**
     *
     * @param a
     * @param val
     */
    public void giveShouterTo(Audible a, double val);


    /*
     * Method called from an Audible from a Shouter it holds - shouter has 
     * reference to the listener that registered it
     */
    /**
     *
     * @param s
     */
    public void heard(ShoutEvent s);


    /**
     * When listeners register with audibles they may want to compare weight
     * to others that have registered. This will allow e.g. causal structure
     * when many listeners are registered with the timeline.
     * Audibles will check and sort every time a new listener is added.
     * @param weight
     */
    public void setWeight(int weight);

    /**
     *
     * @return
     */
    public int getWeight();

}
