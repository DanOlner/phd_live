/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.utils;

import com.google.common.collect.AbstractIterator;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Uses "configs/keyPrintIndex.properties" file to label keyPrints
 * alphabetically
 *
 * Google Guava code copy n pasted from
 * http://stackoverflow.com/questions/8710719/generating-an-alphabetic-sequence-in-java
 *
 *
 */
public class PersistentAlphabeticNumberer extends AbstractIterator<String> {

    private int now;
    private static char[] vs;
    private static int counter;
    private static String nextCounter;
    public static String currentFileName;

    static {
        vs = new char['Z' - 'A' + 1];
        for (char i = 'A'; i <= 'Z'; i++) {
            vs[i - 'A'] = i;
        }
    }

    private StringBuilder alpha(int i) {
        assert i > 0;
        char r = vs[--i % vs.length];
        int n = i / vs.length;
        return n == 0 ? new StringBuilder().append(r) : alpha(n).append(r);
    }

    @Override
    protected String computeNext() {
        return alpha(++now).toString();
    }

    public static String getCurrentAlphabetSequence() {

        if (gl.printingAll) {

            //only need to get new sequence once and re-use
            //I'll have to have set this manually beforehand, annoyingly, cos brain's gone blank
            return currentFileName;

        } else {

            return getNextAlphabetSequence();

        }

    }

    public static synchronized String getNextAlphabetSequence() {

        //Load name of .properties file from .init file
        Properties keyPrintIndexFile = new Properties();

        try {
            keyPrintIndexFile.load(new FileInputStream("configs/keyPrintIndex.properties"));
        } catch (Exception e) {
            System.err.println("file not found");
        }

        Enumeration keys = keyPrintIndexFile.keys();

        //Filename should be the first element in the file. If there's more than one uncommented
        //This will not necessarily pick the first.
        Object key = keys.nextElement();
        Object value = keyPrintIndexFile.get(key);

        counter = Integer.parseInt(value.toString());

        System.out.println("counter from file: " + counter);

        PersistentAlphabeticNumberer a = new PersistentAlphabeticNumberer();

        for (int i = 0; i < counter; i++) {

            nextCounter = a.next();

        }

        //increment counter
        counter++;

        keyPrintIndexFile.setProperty("keyPrintIndex", Integer.toString(counter));

        //write back to file
        try {
            keyPrintIndexFile.store(new FileOutputStream("configs/keyPrintIndex.properties"), null);
        } catch (IOException e) {
            System.err.println("file failed to save: " + e.toString());
        }

        return nextCounter;

    }//end method
}
