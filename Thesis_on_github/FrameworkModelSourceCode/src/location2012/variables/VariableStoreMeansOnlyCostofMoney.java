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
 * Specific for getting 'cost of money' from wage offer
 *
 * @author geodo
 */
public class VariableStoreMeansOnlyCostofMoney extends VariableStore {

    //for summing values for this iteration to find mean
    double total;

    public VariableStoreMeansOnlyCostofMoney(ArrayList arrayList, String varName) {

        super(arrayList, varName);

    }

    //If label for variable is different from varName - for instance
    //If getting x,y coords for people and firms separately, need way to find them
    public VariableStoreMeansOnlyCostofMoney(ArrayList arrayList, String varName, String varNameLabel) {

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
        //1 over answer because we're getting 'cost of money' in time.
        variableData.get(variableData.size() - 1)[0] = 1 / total / objects.size();

//        System.out.println("calculated mean: " + variableData.get(variableData.size() - 1)[0]);

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
