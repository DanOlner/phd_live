/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package location2012.econs;

import location2012.Person;

/**
 * ContributedTime used by actors to store the amount of time they contributed
 * to someone else last time. Need to do this so that others can kick you out
 * as well as you deciding to move that time elsewhere. 
 *
 * @author Olner Dan
 */
public class ContributedTime {

    //from: who's contributed the time
    //to: who's taking the time
    public Person from, to;

    double time;

    public ContributedTime(Person from, Person to) {

        this.from = from;
        this.to = to;
        
    }

    public ContributedTime(Person from, Person to, double time) {

        this.from = from;
        this.to = to;
        
        this.time = time;

    }

    public double getTime() {

        return time;

    }

    public void setTime(double t) {

        time = t;

    }

    /*
     * For setting all contributed time details at once
     */
    public void setFromToTime(Person from, Person to, double time) {

        this.from = from;
        this.to = to;

        this.time = time;

    }

}
