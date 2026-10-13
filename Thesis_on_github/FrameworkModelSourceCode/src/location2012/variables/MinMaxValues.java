/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.variables;

import java.lang.reflect.Field;
import java.util.ArrayList;
import location2012.observe.Audible;
import location2012.observe.Listener;
import location2012.observe.ShoutEvent;
import location2012.observe.Shouter;

/**
 * Goal of class: finds min and max values for an array of objects' variables
 * between turns
 *
 * @author geodo
 */
public class MinMaxValues implements Listener {

    public int weight;
    //Array to find min and max values from
    ArrayList arrayList;
    String varName;
    public double valFromCurrentObject, lowVal, highVal;
    //flag for keeping a record of min max over all days
    boolean allVals = false;
    //if inputting values to follow
    int followWidth;

    /**
     * For inputting a range for the minMax to use. Combined with method for
     * adding values
     *
     * @param follow
     */
    public MinMaxValues() {
    }

    /**
     * Empty constructor if calling methods directly for finding min and max
     */
    public MinMaxValues(int followWidth) {

        this.followWidth = followWidth;
        arrayList = new ArrayList<Double[]>();
    }

    public MinMaxValues(Audible time, double val, int weight, ArrayList arrayList, String varName) {

        giveShouterTo(time, val);

        setWeight(weight);

        this.arrayList = arrayList;
        this.varName = varName;


    }

    /**
     * Boolean allVals: flag for keeping a record of min max over all days
     *
     * @param time
     * @param val
     * @param weight
     * @param arrayList
     * @param varName
     * @param allVals
     */
    public MinMaxValues(Audible time, double val, int weight, ArrayList arrayList,
            String varName, boolean allVals) {

        giveShouterTo(time, val);

        setWeight(weight);

        this.arrayList = arrayList;
        this.varName = varName;
        this.allVals = allVals;


    }

    public void heard(ShoutEvent s) {

        //reset lowest, highest if only keeping a daily record
        if (!allVals) {
            lowVal = 999999;
            highVal = -999999;
        }

        //for each element of array
        for (Object o : arrayList) {

            try {

                Class cls = o.getClass();
                Field fld = cls.getField(varName);
                valFromCurrentObject = fld.getDouble(o);
//                System.out.println(fld.getName().toString() + " = " + fld.getDouble(o));

            } catch (Throwable e) {
                System.err.println("minmaxvargrab fail: " + e.toString());
            }//end try catch

            lowVal = (valFromCurrentObject < lowVal ? valFromCurrentObject : lowVal);
            highVal = (valFromCurrentObject > highVal ? valFromCurrentObject : highVal);

        }//end for each

//        System.out.println("In MinMax: low, high: " + lowVal + "," + highVal);

    }//end method heard

    public void findMinMaxFromArrayListOfArrayDoubles(ArrayList<Double[]> array) {

        //reset lowest, highest if only keeping a daily record
        if (!allVals) {
            lowVal = 999999;
            highVal = -999999;
        }

        for (Double[] da : array) {

            for (Double d : da) {
                lowVal = (d < lowVal ? d : lowVal);
                highVal = (d > highVal ? d : highVal);
            }

        }

    }

    /**
     * If adding values to a range to 'follow' to find mix and max
     *
     * @param val
     */
    public void addValuesToFollow(Double[] vals) {

        arrayList.add(vals);

        if (arrayList.size() > followWidth) {
            
            System.out.println("removing!");            
            arrayList.remove(0);
            
        }

        findMinMaxFromArrayListOfArrayDoubles(arrayList);

    }

    public void giveShouterTo(Audible a, double val) {

        if (a != null) {
            a.registerShouter(new Shouter(this, val));
        }

    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    /**
     * Reset vals: used if minMaXVals is keeping overall record, so it can be
     * reset
     */
    public void resetVals() {

        lowVal = 999999;
        highVal = -999999;

    }
}
