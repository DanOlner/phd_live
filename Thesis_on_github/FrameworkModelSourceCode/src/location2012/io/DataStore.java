/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.io;

import location2012.observe.Audible;
import location2012.observe.Listener;
import location2012.observe.Shouter;
import location2012.observe.ShoutEvent;
import java.util.ArrayList;
import location2012.utils.gl;
import location2012.variables.VariableStore;

/**
 * For getting data outa the model and into an sqlite database For now, I'm just
 * gonna make it concrete and not waste any abstracting time
 *
 * @author Olner Dan
 */
public class DataStore implements Listener {

    //Probably want it to store data at the end of the day when everyone else 
    //has done their thing
    int weight;
    public ArrayList<VariableStore> variableStores = new ArrayList<VariableStore>();
    public ArrayList<Bucket> buckets = new ArrayList<Bucket>();
    //flag: if running for the first time, check the VariableStore's order
    //And sort so that direct variable grabs from objects are called
    //before derived VariableStores.
    //Actually, I think I might just ask myself to add them in the correct order!
    //Is that OK? Some others may rely on deriving from derivations (e.g. SD using average)
    //Just get the order right yourself! No-one else is using this code!
    boolean sort = true;

    //Empty constructor for using manually
    public DataStore() {
    }

    public DataStore(Audible a, double val, int weight) {

        giveShouterTo(a, val);

        setWeight(weight);

    }

    public void addVariableStore(VariableStore v) {

        variableStores.add(v);


    }

    public int getWeight() {

        return weight;

    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    //Probably want two types of shouters - ones to get the data we want
    //and one to trigger storing the data.
    public void giveShouterTo(Audible a, double val) {

        //true: means silenceable
        a.registerShouter(new Shouter(this, val, true));

    }

    public void heard(ShoutEvent s) {

//        System.out.println("In datastore, day: " + s.heardValue);

        try {
            doData();
        } catch (InterruptedException e) {
        }


    }

    private void doData() throws InterruptedException {



        for (VariableStore v : variableStores) {
            v.acquireVariable();
            
//            System.out.println("v: " + v.varNameLabel);
            
        }


        //go through each Bucket and get data
//        for (Bucket cn : buckets) {
//
//            cn.grabData();
//
//        }

        //if doing visualisations, need to wait to give Processing a chance to draw
        if (gl.mode == gl.Mode.DataRead) {
            //System.out.println("ping!");
            Thread.sleep(400);

        }


    }

    /**
     * Get bucket with a particular name
     */
    public Bucket getBucket(String name) {

        for (Bucket b : buckets) {

            if (b.name.equals(name)) {
                return b;
            }

        }

        return new Bucket("null", 0);

    }//end getBucket

    /**
     * Get VariableStore with a particular label (might not be actual variable
     * name). To be used sparingly to get class ref to add to other objects that
     * want to know the variableStore's values
     *
     * @param label
     * @return
     */
    public VariableStore getVariableStoreByVariableLabel(String label) {

        for (VariableStore v : variableStores) {

            if (v.varNameLabel.equals(label)) {
                System.err.println("returning variable store, with label: " + label);
                return v;
            }

        }

        System.err.println("returning a null variable store, tried label: " + label);

        return new VariableStore(null, "null");

    }//end method getVariableStoreByVariableLabel

    /**
     * Get VariableStore by actual variable name (not label name). To be used
     * sparingly to get class ref to add to other objects that want to know the
     * variableStore's values
     *
     * @param name
     * @return
     */
    public VariableStore getVariableStoreByVariableName(String name) {

        for (VariableStore v : variableStores) {

            if (v.varName.equals(name)) {
                System.err.println("returning variable store, with name: " + name);
                return v;
            }

        }

        System.err.println("returning a null variable store, tried name: " + name);

        return new VariableStore(null, "null");

    }//end method getVariableStoreByVariableLabel
}
