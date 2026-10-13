package location2012.observe;

import location2012.observe.Shouter;
import java.util.ArrayList;


/**
 * Interface: Audible - guarantees methods that allow all parameters to be
 * used in the same way, e.g. other classes can ask for notification of changes
 *
 * @author Olner Dan
 */
public interface Audible {

    //Arraylist of all listeners
    //Listeners may have more than one time period they want to be
    //notified about
    /**
     *
     */
    

    /**
     *
     * @param n
     */
    public void registerShouter(Shouter n);

    public void sortListenersByWeight();

    //Audibles must call this method whenever a parameter's value changes. 
    /**
     *
     */
    public void checkValueChange();

    //Called if a value tells a shouter it should shout
    /**
     *
     * @param s
     */
    public void shout(Shouter s);

}
