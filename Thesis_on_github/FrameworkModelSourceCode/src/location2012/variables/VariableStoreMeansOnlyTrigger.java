/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.variables;

import java.util.ArrayList;
import java.util.Collections;
import location2012.observe.Audible;
import location2012.observe.ShoutEvent;
import location2012.observe.Shouter;
import location2012.utils.gl;

/**
 * VariableStoreMeansOnly stores only the mean from a single variable's data
 *
 * @author geodo
 */
public class VariableStoreMeansOnlyTrigger extends VariableStore implements Audible {

    public ArrayList<Shouter> shouters = new ArrayList<Shouter>();
    //for summing values for this iteration to find mean
    double total;
    //Hacking to get average over 10 iterations and then shout Listeners
    int daysToAverageOver = 10;
    double avValOverDays = 0;
    //have 'daysToAverageOver' many days to pass after shouting everything before starting to re-check again.
    int daysPast = 0;

    public VariableStoreMeansOnlyTrigger(ArrayList arrayList, String varName) {

        super(arrayList, varName);

    }

    //If label for variable is different from varName - for instance
    //If getting x,y coords for people and firms separately, need way to find them
    public VariableStoreMeansOnlyTrigger(ArrayList arrayList, String varName, String varNameLabel) {

        super(arrayList, varName, varNameLabel);

    }

    /**
     *
     */
    @Override
    public void acquireVariable() {

        //add new ArrayList of Doubles to contain "today's" data from 'objects'
        variableData.add(new Double[1]);
//        variableData.add(new ArrayList<Double>(objects.size()));

        total = 0;

        //for each element of array
        for (Object o : objects) {

            try {

                cls = o.getClass();
                fld = cls.getField(varName);
                total += fld.getDouble(o);

//                System.out.println(fld.getName().toString() + " = " + fld.getDouble(o));

            } catch (Throwable e) {
                System.err.println("VariableStore " + varName + " fail: " + e.toString());
            }//end try catch

        }//end for each

        //calculate mean
        variableData.get(variableData.size() - 1)[0] = total / objects.size();

//        System.out.println("calculated mean: " + variableData.get(variableData.size() - 1)[0]);

        //do check to see about Shouting listeners
        avValOverDays = 0;
        daysPast++;
        
        if (gl.day > daysToAverageOver) {

            for (int i = 0; i < daysToAverageOver; i++) {
                avValOverDays += variableData.get(variableData.size() - i - 1)[0];
            }

            avValOverDays /= daysToAverageOver;

        }

//        System.out.println("Average over " + daysToAverageOver + " days: " + avValOverDays);

//        if (avValOverDays < 10 && daysPast > 10) {
//        if (avValOverDays < 0.0001 && daysPast > 10) {
        if (avValOverDays < 0.00005 && daysPast > 10) {
            
            //reset daysPast so this can only get called 'daysToAverageOver' number of times
            daysPast = 0;
            
            System.out.println("shouting on day: " + gl.day + ", average over " + daysToAverageOver + " days: " + avValOverDays);
            
            checkValueChange();
        } 
//        else {
//            
//            System.out.println("Not shouting, day: " + gl.day);
//            
//        }

    }//end method acquireVariable

    /**
     * Return most recent ArrayList addition to the VariableStore
     *
     * @return
     */
    public Double[] getLatestArrayListOfData() {

//        System.out.println("variableData: " + varName + " " + variableData.size());

//        if (variableData.size() > 0) {
        return variableData.get(variableData.size() - 1);
//        } else {
//            return new ArrayList<Double>();
//        }

    }//end method getLatestArrayListOfData

    @Override
    public void registerShouter(Shouter n) {
        shouters.add(n);
    }

    @Override
    public void sortListenersByWeight() {
        Collections.sort(shouters);
    }

    @Override
    public void checkValueChange() {

//        System.out.println("SHOUTING!");

        for (Shouter s : shouters) {

            shout(s);

        }

    }

    @Override
    public void shout(Shouter s) {
        s.lstr.heard(new ShoutEvent(0));
    }

    public void checkShouterOrder() {

        //check order of Listeners
        for (Shouter sh : shouters) {

            System.out.println("ListWeight: " + sh.lstr.getWeight() + ", name: " + sh.lstr.getClass().getName());

        }

    }
}
