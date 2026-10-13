/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.econs;

import location2012.utils.p;

/**
 * Static methods for letting actors know how much they can make with their time
 * either by backyard production or collaboration.
 *
 * Note: in the version I'm doing now -if economies of scale are worked out by time input
 * and the actual number of individuals doesn’t matter, then there’s no difference between
 * one person working and two. It’s the same calculation: the amount of time being put in.
 *
 * Initialised with a ratio of time to output. (This keeps day-distance the basic
 * unit of the model.)
 *
 * p.s. was all this wrapping necessary?
 *
 * @author gy06do
 */
public class Productivity {

    //inner class for outputting
    private static Output o;
    //output to return...
    private static double output;

    public Productivity(double ratio) {

        o = new Output(ratio);

    }

    public static double giveOutput(double time) {

        return (o.giveOutput(time));

    }

    //inner class
    public class Output {

        //basic ratio of time to output - holds for backyard, is basis for collaboration
        //Keeps them both in proportion
        private double ratio;
        //output for returning
        private double output;

        public Output(double ratio) {

            this.ratio = ratio;
        }

        /**
         * Return output based on time input - note, this is total output
         * Whoever gets it will need to work out what their share is.
         *
         * @param time
         */
        public double giveOutput(double time) {

//If time <= 1, total output is linear
//If time > 1, total output per unit of time must increase, proportional to time. So.
//Productivity per unit of time = 1 +, say, (0.1 * time)
//So if 10 extra units of time put in, output = 2 per unit of time.
            if (time <= 1) {

                //sometimes time comes in very close to 1, but not quite...
//                if (time > 0.99999 && time < 1) {
//                    time = 1;
//                }

                return time * ratio;
            } else {

                //Why time - 1? Coz we already know time>=1
                //(since we're only here if time > 1)
                //So in theory, transition to EoS should be smooth.
                //Later, we need finer controls for this!

                //wolfram alpha versions:
                //plot y = x* (1 + (0.13 * (x-1))) for x=1 to 10
                //This for how much output per unit of input time changes - currently linear
                //plot y = (x* (1 + (0.13 * (x-1))))/x for x=1 to 10
                
                //Turns out that reduces to something much simpler
                //return time * (1 + (0.15 * (time-1) * ratio));
                //becomes
                //0.85x + 0.15x^2
                //let's check - yup. 
                //System.out.println("prod1: " + (time * (1 + (0.15 * (time-1) * ratio))) + ", prod2: "
                  //      + ((0.85*time) + (0.15*time*time)*ratio));

                //return ((0.85*time) + (0.15*time*time)*ratio);
                return ((time*2)-1)*ratio;


                //return time * ratio;
                //return 0;

            }

        }
    }//end inner class
}
