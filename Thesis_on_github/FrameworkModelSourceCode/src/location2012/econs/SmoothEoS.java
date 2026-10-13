/*
 * Smooth economies of scale function, able to smoothly increase from zero
 */
package location2012.econs;

import location2012.utils.gl;

/**
 *
 * @author Dan
 */
public class SmoothEoS extends Production {

    @Override
    public double giveOutput(double time) {


//        return(((time*time)*(gl.EoS))+time);

//        return(((time*time)*(gl.EoS*5))+time);
//        return(((time*time)/5)+time)*gl.EoS;
//        return(((time*time))+time)*gl.EoS;
        //Using EoS to only control the extra-efficiency part. 
        //So if EoS = 0, output will be linear.
//        System.out.println("Eos. Incoming time: " + time + ", outgoing stuff: "
//                + (((time * time * gl.EoS)) + time));

//        return ((((time * time * gl.EoS)) + time)) * 0.05;
//        return ((((time * time)) + time)) * gl.EoS * 0.1;
        return (time * time * gl.EoS_Curve) + (time * gl.EoS_Magnitude);

//        return(((time*time)/5)+time);
//        return(time);

    }

    @Override
    public void testOutput() {
    }
}
