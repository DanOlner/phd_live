package location2012.observe;

import location2012.observe.Audible;
import location2012.observe.Shouter;
import location2012.observe.ShoutEvent;
import location2012.utils.p;
import java.util.ArrayList;
import java.util.Collections;
import location2012.utils.Timer;
import location2012.utils.gl;

/**
 * Timeline: schedules all events for the model, including 1. When (in model
 * time) to draw to the screen [different from when Processing decides to draw]
 * 2. When calculators will ask for parameters from the model 3. When agent
 * events will need to happen
 *
 * @author Olner Dan
 */
public class TriggerListener implements Audible, Listener {
    
    public ArrayList<Shouter> shouters = new ArrayList<Shouter>();
    int weight;

   
    /**
     *
     */
    public TriggerListener() {

        

    }

 
    //Implemented - to check to see if any shouters need to know
    /**
     *
     */
    public void checkValueChange() {

//        System.out.println("shouter num: " + shouters.size());

        for (Shouter s : shouters) {

            //for now we're just checking for equality
            //later we'll need to ask the shouters to say
            //whether they're interested in a particular change
            if (s.value == 0) {
               
                shout(s);

            }

        }
    }//end method check value change
    private synchronized void test(Shouter s) {

        shout(s);

    }

    /**
     *
     * @param s
     */
    public void shout(Shouter s) {

        
            s.lstr.heard(new ShoutEvent(0));
        

    }

   

    /**
     *
     * @param s
     */
    public void registerShouter(Shouter s) {

        shouters.add(s);

    }

   

    public void sortListenersByWeight() {

        //sort shouters by their weight
        //Collections.sort(shouters, new ListenerCompare());
        Collections.sort(shouters);

    }

//    /*
//     * Currently not an implemented method of Audible; just putting into timeline
//     * to see how that goes...
//     */
//    public void addListenerDirect(Listener l, int val) {
//
//        l.giveShouterTo(this, val);
//
//    }
    //Just for checking the order...
    public void checkShouterOrder() {

        //check order of Listeners
        for (Shouter sh : shouters) {

            System.out.println("ListWeight: " + sh.lstr.getWeight() + ", name: " + sh.lstr.getClass().getName());

        }

    }

    @Override
    public void giveShouterTo(Audible a, double val) {
        
            a.registerShouter(new Shouter(this, val));
        
    }

    @Override
    public void heard(ShoutEvent s) {
        
        //timeLine shouts me. I need to check if the VariableStore I'm monitoring has reached a certain value.
        
        
        
    }

    @Override
    public void setWeight(int weight) {
        
        this.weight = weight;
        
    }

    @Override
    public int getWeight() {
        
        return weight;
        
    }
    
    
}
