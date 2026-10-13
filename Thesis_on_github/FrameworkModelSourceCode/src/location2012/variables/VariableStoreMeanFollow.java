/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.variables;

import java.util.ArrayList;
import location2012.utils.gl;

/**
 * VariableStoreMeanSDFollow: getting stats from timesteps in the past bandwidth
 * of `follow'. Store them in today's slot Store for each array element
 *
 * @author geodo
 */
public class VariableStoreMeanFollow extends VariableStore {

    //for summing values for this iteration to find mean
    double total;
    //
    public double mean, variance, sd;
    private int followDays;
    VariableStore followData;

    public VariableStoreMeanFollow(ArrayList arrayList, String varName,
            VariableStore followData, int follow) {

        super(arrayList, varName);
        this.followDays = follow;
        this.followData = followData;

    }

    //If label for variable is different from varName - for instance
    //If getting x,y coords for people and firms separately, need way to find them
    public VariableStoreMeanFollow(ArrayList arrayList, String varName, 
            String varNameLabel, VariableStore followData, int follow) {

        super(arrayList, varName, varNameLabel);
        this.followDays = follow;
        this.followData = followData;

    }

    /**
     *
     */
    @Override
    public void acquireVariable() {

        //add new ArrayList of Doubles to contain "today's" data from 'objects'
        variableData.add(new Double[objects.size()]);
//        variableData.add(new ArrayList<Double>(objects.size()));

        int index = 0;

        //for each element of array
        for (Object o : objects) {

            total = 0;
            mean = 0;
            variance = 0;

            try {

//                cls = o.getClass();
//                fld = cls.getField(varName);
                //store today's data temporarily in the slot we'll use for
                //the final mean value
//                variableData.get(variableData.size() - 1)[index] = fld.getDouble(o);

//                System.out.println(fld.getName().toString() + " = " + fld.getDouble(o));

                //calculate mean
                //Get mean from previous timesteps. Either 'follow' bandwidth
                //or available timesteps, whichever is smaller
                int tempFollow = (gl.day < followDays ? gl.day : followDays);

                for (int i = 0; i < tempFollow; i++) {
                    mean += followData.variableData.get(followData.variableData.size() - 1 - i)[index];
//                    System.out.println("val stored in previous slotS: "
//                            + variableData.get(variableData.size() - 1 - i)[index]);
                }

//                System.out.println(" ");

                //average over the number of days we're looking at
                mean /= tempFollow;
                //store in the current slot
                variableData.get(variableData.size() - 1)[index] = mean;

                index++;

            } catch (Throwable e) {
                System.err.println("VariableStore " + varName + " fail. Index: " + index + ", " + e.toString());
            }//end try catch

        }//end for each



        //now we have means, work out variance
//        for (Object o : objects) {
//
//            try {
//
//                cls = o.getClass();
//                fld = cls.getField(varName);
//                variance += Math.pow((mean - fld.getDouble(o)), 2);
//
////                System.out.println(fld.getName().toString() + " = " + fld.getDouble(o));
//
//            } catch (Throwable e) {
//                System.err.println("VariableStore " + varName + " fail: " + e.toString());
//            }//end try catch
//
//        }//end 2nd for objects
//
//        //find final variance by averaging total squared differences
//        variance /= objects.size();
//        //find SD
//        sd = Math.sqrt(variance);
//
////        System.out.println("calculated mean: " + mean + ", variance: " + variance + ", sd: " + sd);
//
//        //hacking what var actually gets stored, for now
//        variableData.get(variableData.size() - 1)[0] = mean;
////        variableData.get(variableData.size() - 1)[0] = sd;



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
