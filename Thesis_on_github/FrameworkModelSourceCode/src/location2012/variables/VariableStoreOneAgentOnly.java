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
 * The `Store one agent only' version targets the agent who's ID matches
 * gl.LOOKATME and looks for the appropriate variable.
 *
 * @author geodo
 */
public class VariableStoreOneAgentOnly extends VariableStore {

    Field chkFld;

    public VariableStoreOneAgentOnly(ArrayList arrayList, String varName) {

        super(arrayList, varName);


    }

    //If label for variable is different from varName - for instance
    //If getting x,y coords for people and firms separately, need way to find them
    public VariableStoreOneAgentOnly(ArrayList arrayList, String varName, String varNameLabel) {

        super(arrayList, varName, varNameLabel);


    }

    public void acquireVariable() {

        //add new ArrayList of Doubles to contain "today's" data from 'objects'
        //Only for one actor, so just the single index
        variableData.add(new Double[1]);
//        variableData.add(new Double[objects.size()]);

//        int index = 0;

        //This is the generic version...
        //for each element of array
//        for (Object o : objects) {
//
//            try {
//
//                cls = o.getClass();
//
//                //check ID
//                chkFld = cls.getField("ID");
//
//
////                if (chkFld.getInt(o) == gl.LOOKATME) {
////                    System.out.println("found");
////                    fld = cls.getField(varName);
////                    variableData.get(variableData.size() - 1)[0] = fld.getDouble(o);
////                }
//
//
//
////                System.out.println(fld.getName().toString() + " = " + fld.getDouble(o));
//
//            } catch (Throwable e) {
//                System.err.println("VariableStore " + varName + " fail: " + e.toString());
//            }//end try catch
//
//        }//end for each

        //... and this is the one I'm hacking to get a single Firm's markUp
        try {

            Object o = (Object) objects.get(0);
            cls = o.getClass();

            fld = cls.getField(varName);
            variableData.get(variableData.size() - 1)[0] = fld.getDouble(o);

//            System.out.println(fld.getName().toString() + " = " + fld.getDouble(o));

        } catch (Throwable e) {
            System.err.println("VariableStore " + varName + " fail: " + e.toString());
        }//end try catch


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
