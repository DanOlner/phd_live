/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.variables;

import java.util.ArrayList;
import location2012.Actor;

/**
 * VariableStore stores a single variable's data - either a direct variable from
 * a model object or a calculated derivation of one
 *
 * @author geodo
 */
public class VariableStoreCoords extends VariableStore {

    public VariableStoreCoords(ArrayList arrayList, String varName) {

        super(arrayList, varName);

    }

    //If label for variable is different from varName - for instance
    //If getting x,y coords for people and firms separately, need way to find them
    public VariableStoreCoords(ArrayList arrayList, String varName, String varNameLabel) {

        super(arrayList, varName, varNameLabel);

    }

    public void acquireVariable() {

        //for coords, need some horrible hacking as we want to access the Point2D

        //add new ArrayList of Doubles to contain "today's" data from 'objects'
        variableData.add(new Double[objects.size()]);
//        variableData.add(new ArrayList<Double>(objects.size()));

        int index = 0;
        Actor a;

        //HAAAAAAACKKKKKKKKK!!!!!!!!!!
        for (Object o : objects) {

            try {

                a = (Actor) o;
                if (varName.equals("x")) {
                    variableData.get(variableData.size() - 1)[index++] = a.getx();
                } else {
                    variableData.get(variableData.size() - 1)[index++] = a.gety();                    
                }

            } catch (Throwable e) {
                System.err.println("VariableStore " + varName + " fail: " + e.toString());
            }//end try catch

        }//end for each

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
