/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.utils;

import java.util.Date;

/**
 *
 * @author geodo
 */
public class FileName {

    public static String getFileName() {

        Date d = new Date();

        String a = PersistentAlphabeticNumberer.getNextAlphabetSequence();
//        String a = PersistentAlphabeticNumberer.getCurrentAlphabetSequence();

//                System.out.println("today = " + d);
//        String fileName = "C£//saveframes/keyprints/" + a + "_"
        String fileName = "C£//saveframes/" + a + "_modelday_"
                + gl.day + "_"
                + gl.configFileName + "_" + d + "_";
        fileName = fileName.replaceAll(" ", "-");
        fileName = fileName.replaceAll(":", "_");
        fileName = fileName.replaceAll("£", ":");

        return fileName;

    }
}
