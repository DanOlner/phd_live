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
 * MarkupCrashTrigger stores where one of two Firms ends up in a positive
 * feedback loop and `crashes'.
 *
 * @author geodo
 */
public class VariableStoreMarkupCrashTrigger extends VariableStore implements Audible {

    public ArrayList<Shouter> shouters = new ArrayList<Shouter>();
    
    //number of times firm zero's good cost has increased
    int countPositiveGoodCostChange;
    //have 'daysToAverageOver' many days to pass after shouting everything before starting to re-check again.
    int daysPast = 0;

    public VariableStoreMarkupCrashTrigger(ArrayList arrayList, String varName) {

        super(arrayList, varName);

    }

    //If label for variable is different from varName - for instance
    //If getting x,y coords for people and firms separately, need way to find them
    public VariableStoreMarkupCrashTrigger(ArrayList arrayList, String varName, String varNameLabel) {

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

        try {
            
            //just get first Firm
            Object o = (Object) objects.get(0);

            cls = o.getClass();
            fld = cls.getField(varName);
            countPositiveGoodCostChange = fld.getInt(o);

//                System.out.println(fld.getName().toString() + " = " + fld.getDouble(o));

        } catch (Throwable e) {
            System.err.println("VariableStore " + varName + " fail: " + e.toString());
        }//end try catch

        //store firm zero's days with positive good cost change
        //I don't actually need it, it turns out. Overkill, but HACKEROO!
        variableData.get(variableData.size() - 1)[0] = (double) countPositiveGoodCostChange;
        
        
        if (countPositiveGoodCostChange > 210) {
            
            System.out.println("shouting on day: " + gl.day);

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
