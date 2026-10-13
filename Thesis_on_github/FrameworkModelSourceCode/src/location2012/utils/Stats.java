/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.utils;

import java.util.ArrayList;
import location2012.SingleActorAction;
import location2012.SingleActorPerson;

/**
 *
 * @author geodo
 */
public class Stats {

    private static ArrayList<Double[]> meanSD = new ArrayList<Double[]>();
    private static double mean, var, sd, uq, lq;//mean, variance, standard dev, upper quartile, lower quartile

    /**
     * Takes in an ArrayList of double[]s. Just finds mean.
     *
     * @param arrayList
     * @return
     */
    public static synchronized ArrayList<Double[]> returnArrayListWithMean(ArrayList<Double[]> arrayList) {

        meanSD.clear();

        for (Double[] d : arrayList) {

            mean = 0;

            for (Double e : d) {
                mean += e;
            }

            Double addD[] = new Double[2];
            addD[0] = mean / arrayList.get(0).length;
//            Double addD[] = {mean / arrayList.get(0).length};

            meanSD.add(addD);

        }

        return (ArrayList<Double[]>) meanSD.clone();

    }

    /**
     * Takes in an ArrayList of double[]s. Finds mean, SD for each ArrayList
     * entry Returns a new ArrayList with double[0] = mean, double[1] = SD.
     * double[2] = upper quartile double[3]=lower quartile.
     * 
     * Except for those last two which currently it doesn't.
     *
     * @param arrayList
     * @return
     */
    public static ArrayList<Double[]> returnArrayListWithMeanAndSD(ArrayList<Double[]> arrayList) {

       meanSD.clear();

        for (Double[] d : arrayList) {

            mean = 0;

            for (Double e : d) {
                mean += e;
            }

            Double addD[] = new Double[2];
            addD[0] = mean / arrayList.get(0).length;
//            Double addD[] = {mean / arrayList.get(0).length};

            meanSD.add(addD);

        }

        

        //now we have means, work out variance. We need the index ref to
        //access the matching meanSD array index at the same time
        for (int i = 0; i < arrayList.size(); i++) {
            
//            System.out.println("size meanSD: " + meanSD.size() + ", arrayList: " + arrayList.size());

            var = 0;

            //For a single iteration, get each Double[] mean value we just set
            //Use that to work out variance
            for (int j = 0; j < arrayList.get(0).length; j++) {

                var += Math.pow((meanSD.get(i)[0] - arrayList.get(i)[j]), 2);

            }//for j

            //number of agents
            var /= arrayList.get(0).length;
            //find SD
            sd = Math.sqrt(var);

            meanSD.get(i)[1] = sd;

        }//for i

       
        return (ArrayList<Double[]>) meanSD.clone();

    }
}
