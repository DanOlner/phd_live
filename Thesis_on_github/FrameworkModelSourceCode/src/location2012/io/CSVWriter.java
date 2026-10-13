/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.io;

import location2012.observe.Audible;
import location2012.observe.Listener;
import location2012.observe.Shouter;
import location2012.observe.ShoutEvent;
import location2012.observe.Timeline;
import location2012.utils.p;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import location2012.utils.gl;

/**
 * CSV Writer: takes a DataStore, which itself contains 'Columns' of data
 * for a bunch of ArrayList entities, and writes them to files named via
 * Bucket.name.
 *
 * @author Olner Dan
 */
public class CSVWriter implements Listener {

    int weight = 5;
    DataStore ds;

    //Take in timeline for shouting and simpleworld for accessing the data
    public CSVWriter(DataStore ds, Audible a, double val) {

        giveShouterTo(a, val);

        this.ds = ds;

    }

    //non shouty version
    public CSVWriter(DataStore ds) {

        this.ds = ds;

    }

    public int getWeight() {

        return weight;



    }

    public void setWeight(int weight) {
    }

    //Probably want two types of shouters - ones to get the data we want
    //and one to trigger storing the data.
    public void giveShouterTo(Audible a, double val) {

        //cast to the timeline for now
        Timeline t;

        t = (Timeline) a;

        t.registerEndOfWorldShouter(new Shouter(this, 0));

    }

    public void heard(ShoutEvent s) {

        System.out.println("writing");


        BufferedWriter writer;

        //For each column in DataStore, write an appropriate file
        for (Bucket c : ds.buckets) {


            try {

                //p.p(filenames[str]);

                writer = new BufferedWriter(new FileWriter("data/" + c.name + gl.currentRun + ".csv"));


                //dataWritePointNumber - usually the number of model run days divided by dataWriteGap, but less if
                //the model was killed early
                for (int d = 0; d < gl.dataWritePointNumber + 1; d++) {

                    //write columns if we're at the start...
                    if (d == 0) {
                        //add one for days
                        writer.write("day,");
                        //if it's set
                        if (c.colnames != null) {


                            for (String a : c.colnames) {

                                writer.write(a + ",");

                            }

                        }
                    } else {

                        //c.vals[1].length - number of entities
                        //one extra for column names
                        for (int f = 0; f < c.vals[1].length + 1; f++) {

                            //System.out.println("f=" + f);

                            //if we've reached the end of the line - when f == entity number - don't use a comma after the value
                            //add column name if there is one, for first column

                            //System.out.println("Size of array: " + c.vals.length + "," + c.vals[0].length + "f=" + f + ", d="+d);


                            if (f == 0) {
                                //Add the day number in the first column
                                writer.write(d * gl.dataWriteGap + ",");

                            } else if (f == c.vals[1].length + 1) {
                                writer.write(Double.toString(c.vals[d - 1][f - 1]));
                            } else {
//                                System.out.println("vals size: " + c.vals.length + "," + c.vals[0].length);
                                writer.write(Double.toString(c.vals[d - 1][f - 1]) + ",");
                            }

                        }

                    }

                    //next line
                    writer.write("\n");

                }

                writer.close();

            } catch (IOException ex) {
                System.out.println("couldn't write file: " + ex);
            }


        } //end of String foreach loop...


    }
}
