/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package location2012.io;

import java.util.ArrayList;

/**
 * For storing columns in datastore - lots of horrible methods needed
 * It needs to return a name and a value
 * @author Olner Dan
 */
public class Bucket {

    public String name;

    //2D: 1 for time, the other for entities e.g. Persons
    public double[][] vals;
    //Ooo, many array refs
    ArrayList entities;

    String[] colnames;

    int samplePoint = 0;
    int samplelength;

    public Bucket (String str, ArrayList entities, int samplelength) {

        this.samplelength = samplelength;

        //assign name for the column
        name = str;

        vals = new double[samplelength][entities.size()];

        this.entities = entities;

    }

    //Used for producing data from in-run full buckets
    //The Bucket used will need to be added at the time
    public Bucket(String str, int samplelength) {

        name = str;

    }


    //Instruction from datastore to grab the data this column stores
    public void grabData() {};

    //used by metabuckets to get data from in-run buckets
    public void grabData(Bucket b) {};


    public String[] getColNames() {
     
        return colnames;

    }

}
