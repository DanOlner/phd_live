/*
 * Core-model-style economies of scale function, simple straight line
 */
package location2012.econs;

import location2012.utils.gl;

/**
 *
 * @author Dan
 */
public class CoreModelEoS extends Production {

    @Override
    public double giveOutput(double time) {


        //l = a + bx, in terms of x
        return (time - 3) / gl.EoS_Magnitude;

    }

    @Override
    public void testOutput() {
    }
}
