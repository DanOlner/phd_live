package location2012.io;

import au.com.bytecode.opencsv.CSVReader;
import java.io.FileReader;
import java.io.IOException;
import location2012.utils.p;

/**
 *
 * @author User
 */
public class DataLoader {

    private static CSVReader reader;

    /*
     * Load data into array, to be used by bucket.
     */
    public static void LoadData(String fileName, double[][] vals) {

        try {
            read(fileName, vals);
        } catch (IOException ex) {
            p.p("couldn't read file: " + ex);
        }

    }

    private static void read(String fileName, double[][] vals) throws IOException {

        //System.out.println("fileName: " + fileName);
        //System.out.println("vals size: "+ vals.length + "," + vals[1].length);

        reader = new CSVReader(new FileReader(fileName));
        String[] nextLine;

        //get past the first line, which is just column headings
        reader.readNext();

        int samplePoint = 0;

        
        while ((nextLine = reader.readNext()) != null) {


            for (int i = 0; i < nextLine.length - 1; i++) {

                //System.out.println(fileName + ":: samplePoint: " + samplePoint + ", i: " + i);

                vals[samplePoint][i] = Double.parseDouble(nextLine[i]);

            }

            samplePoint++;

        }//end while nextLine

    }
}
