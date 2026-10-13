/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.control;

import location2012.observe.Audible;
import location2012.observe.Listener;
import location2012.observe.ShoutEvent;
import location2012.observe.Shouter;

/**
 * Abstract listener: used e.g. by mousebutton listener, wrapper for VarChangers
 *
 * @author Dan
 */
public class ListenerWrapper implements Listener {

    Listener l;

    public ListenerWrapper(Audible a, double val) {

        giveShouterTo(a, val);

    }

    public ListenerWrapper(Audible a, double val, Listener l) {

        this.l = l;

        giveShouterTo(a, val);

    }

    public void setListener(Listener l) {
        this.l = l;
    }

    public Listener getListener() {

        return l;

    }

    public int getWeight() {
        return 0;
    }

    public void giveShouterTo(Audible a, double val) {

        if (a != null) {
            a.registerShouter(new Shouter(this, val));
        }

    }

    public void heard(ShoutEvent s) {
        if (l != null) {
            l.heard(s);
        }
    }

    public void setWeight(int weight) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
