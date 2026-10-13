package location2012.utils;

import java.util.ArrayList;

/**
 *
 * @author User
 */
public class PullOneClassFromArray {

    public static void PULLCLASS(ArrayList from, ArrayList to, Object ob) {

        for(Object o : from) {
            if(o.getClass().isInstance(ob)) {
//            if(o instanceof ob)) {
                to.add(o);
            }
        }

    }
    
    public static void PULLCLASS(ArrayList from, ArrayList to, Class ob) {

        for(Object o : from) {
            if(o.getClass().isInstance(ob)) {
//            if(o.getClass() instanceof ob) {
            
                to.add(o);
            }
        }

    }

}
