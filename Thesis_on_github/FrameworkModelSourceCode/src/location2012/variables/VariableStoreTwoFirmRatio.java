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
 * The `Two Firm price ratio' version finds the ratio between two Firms' prices.
 * Er.
 *
 * @author geodo
 */
public class VariableStoreTwoFirmRatio extends VariableStore {

    Field chkFld;
    double ratioFirm0overFirm1;
    //if true, always make the highest of the two values the numerator
    //so the ratio is always > 1
    boolean highestIsNumerator;

    public VariableStoreTwoFirmRatio(ArrayList arrayList, String varName) {

        super(arrayList, varName);


    }

    //If label for variable is different from varName - for instance
    //If getting x,y coords for people and firms separately, need way to find them
    public VariableStoreTwoFirmRatio(ArrayList arrayList,
            String varName, String varNameLabel, boolean highestIsNumerator) {

        super(arrayList, varName, varNameLabel);

        this.highestIsNumerator = highestIsNumerator;


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

            //firm 0
            Object o = (Object) objects.get(0);
            cls = o.getClass();

            fld = cls.getField(varName);
            ratioFirm0overFirm1 = fld.getDouble(o);

            //firm 1
            o = (Object) objects.get(1);
            cls = o.getClass();

            fld = cls.getField(varName);
            //find actual ratio

            if (highestIsNumerator) {

                if (ratioFirm0overFirm1 > fld.getDouble(o)) {
                    variableData.get(variableData.size() - 1)[0] =
                            ratioFirm0overFirm1 / fld.getDouble(o);
                } else {
                    variableData.get(variableData.size() - 1)[0] =
                            fld.getDouble(o) / ratioFirm0overFirm1;
                }

                //if not forcing highest to be numerator, it's not the opposite way around
                //it's just however it comes.
            } else {
//                ratioFirm0overFirm1 /= fld.getDouble(o);
                variableData.get(variableData.size() - 1)[0] = ratioFirm0overFirm1/fld.getDouble(o);
            }

            //add ratio to variableStore

//            System.out.println("ratio = " + ratioFirm0overFirm1);

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
