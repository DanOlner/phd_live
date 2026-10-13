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

/**
 * VariableStoreMeansOnly stores only the mean from a single variable's data
 *
 * @author geodo
 */
public class VariableStoreCrossesZero extends VariableStore {

    //for summing values for this iteration to find mean
    double total;
    //
    public double mean, variance, sd;

    public VariableStoreCrossesZero(ArrayList arrayList, String varName) {

        super(arrayList, varName);

    }

    //If label for variable is different from varName - for instance
    //If getting x,y coords for people and firms separately, need way to find them
    public VariableStoreCrossesZero(ArrayList arrayList, String varName, String varNameLabel) {

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

        total = 0; mean = 0; variance = 0;
        
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
        mean = total / objects.size();//overkill but just so I can tell what's going on

        //now we have means, work out variance
        for (Object o : objects) {

            try {

                cls = o.getClass();
                fld = cls.getField(varName);
                variance += Math.pow((mean - fld.getDouble(o)), 2);

//                System.out.println(fld.getName().toString() + " = " + fld.getDouble(o));

            } catch (Throwable e) {
                System.err.println("VariableStore " + varName + " fail: " + e.toString());
            }//end try catch

        }//end 2nd for objects

        //find final variance by averaging total squared differences
        variance /= objects.size();
        //find SD
        sd = Math.sqrt(variance);

//        System.out.println("calculated mean: " + mean + ", variance: " + variance + ", sd: " + sd);

        //hacking what var actually gets stored, for now
        variableData.get(variableData.size() - 1)[0] = mean;
//        variableData.get(variableData.size() - 1)[0] = sd;
        
        
        
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
}
