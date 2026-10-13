/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package location2012.utils;

/**
 *
 * For outputting debug msgs - keeping name and method as short as possible!
 *
 * @author Olner Dan
 */
public class p {
    
    public static boolean print = true;
    public static boolean agent = true;
    //static boolean singleObjAdd = true;

    static Object ob;

    //for general output
    public static void p(String s) {

        if(print) System.out.println(s);

    }

    //For printing agent decisions and actions
    public static void a(String s) {

        if(agent) System.out.println("AGENT:: " + s);

    }

    //For outputting from a selected object
    //Object is designated using singleObjAdd method
    public static void s(String s, Object o) {

        if(o.equals(ob)) {

            System.out.println(" -> " + o.getClass().getSimpleName() + ":: " + s);

        }

    }

    public static void print (boolean b) {

        print = b;

    }

    public static void agent (boolean b) {

        agent = b;

    }

    public static void singleObjAdd (Object o) {

        ob = o;

    }

    //For when I can't be bothererd typing true or false!
    public static void print (int bool) {

        print = (bool == 1) ? true: false;

    }

    public static void agent (int bool) {

        agent = (bool == 1) ? true: false;

    }

    
}
