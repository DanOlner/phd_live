/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.variables;

import java.lang.reflect.Field;
import java.util.ArrayList;
import location2012.utils.gl;

/**
 * VariableStore stores a single variable's data - either a direct variable from
 * a model object or a calculated derivation of one
 *
 * @author geodo
 */
public class VariableStore {

    //Used to sort direct from derived variables. Direct ones straight from objects 
    //will need fetching first. DataStore sorts them the first time it's shouted.
    //Turning this off for now - just add in the correct order please!
//    public int weight;
    //Array to get variables from
    public ArrayList objects;
    //Field name of variable 
    public String varName, varNameLabel;
    //Expandable store of variable data
    //Inner ArrayList contains "today's" data from 'objects'
    //Outer ArrayList is record of each day recorded 
    //Using ArrayList for inner since object numbers may change in future
//    public ArrayList<ArrayList<Double>> variableData = new ArrayList<ArrayList<Double>>();
    public ArrayList<Double[]> variableData = new ArrayList<Double[]>();
    //
    Class cls;
    Field fld;

    public VariableStore(ArrayList arrayList, String varName) {

//        this.weight = weight;
        this.objects = arrayList;
        this.varName = varName;
        this.varNameLabel = varName;

    }

    //If label for variable is different from varName - for instance
    //If getting x,y coords for people and firms separately, need way to find them
    public VariableStore(ArrayList arrayList, String varName, String varNameLabel) {

//        this.weight = weight;
        this.objects = arrayList;
        this.varName = varName;
        this.varNameLabel = varNameLabel;


    }

    public void acquireVariable() {

        //add new ArrayList of Doubles to contain "today's" data from 'objects'
        variableData.add(new Double[objects.size()]);
//        variableData.add(new ArrayList<Double>(objects.size()));
        
        //if global follow is on and enough days have passed
//        System.out.println("size b4: " + variableData.size()); 
       
//        if(gl.globalFollowWidth !=-1 && gl.day > gl.globalFollowWidth) {
//            
//            variableData.remove(0);
//            
//        }
        
//        System.out.println("size after: " + variableData.size());

        int index = 0;

        //for each element of array
        for (Object o : objects) {

            try {

                cls = o.getClass();
                fld = cls.getField(varName);
//                valFromCurrentObject = fld.getDouble(o);
                //add variable data to last entry in variableData list
//                variableData.get(variableData.size() - 1).add(fld.getDouble(o));
//                variableData.get(variableData.size() - 1).set(index++, fld.getDouble(o));
                variableData.get(variableData.size() - 1)[index++] = fld.getDouble(o);

//                System.out.println(fld.getName().toString() + " = " + fld.getDouble(o));

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
