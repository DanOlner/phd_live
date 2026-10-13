/*
 * Gets a range of elements from an ArrayList, returns new ArrayList
 */
package location2012.utils;

import java.util.ArrayList;

/**
 * Gets a range of elements from an ArrayList, returns new ArrayList.
 * Adapted from http://www.ehow.com/how_6882624_do-values-sorted-list-java_.html
 * 
 * @author Dan
 */
public class getArrayListSubset {

    public static ArrayList GETRANGE(ArrayList list, int start, int last) {

        ArrayList temp = new ArrayList();

        for (int x = start; x <= last; x++) {

            temp.add(list.get(x));

        }

        return temp;

    }//end getRange method


    /**
     * get a particular fraction of the list. 0 < fr < 1
     */
    public static ArrayList GETFRACTION(ArrayList list, double fr) {

        ArrayList temp = new ArrayList();

        for (int x = 0; x <= list.size() * fr; x++) {

            temp.add(list.get(x));

        }

        return temp;

    }//end getfraction method
    
    
    /**
     * Return whole ArrayList minus one element
     */
    public static ArrayList GETALLBUTTHIS(ArrayList list, Object thiis) {

        ArrayList temp = new ArrayList();

        for(Object o : list) {
            
            if(!o.equals(thiis)) {
                temp.add(o);                
            }
            
        }
        
        return temp;

    }//end getfraction method


}