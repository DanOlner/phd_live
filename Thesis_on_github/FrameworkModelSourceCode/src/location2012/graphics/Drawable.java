package location2012.graphics;

import processing.core.*;
/**
 * INTERFACE: Drawable
 * Defines anything that may be drawn in the Processing window
 * Defines methods for giving the Processing window what it needs
 * Can draw3D other things, of course, but this helps keep them all together
 *
 * @author Olner Dan
 */
public interface Drawable {

    /**
     *
     * @param p
     */
    public void draw3D(PApplet p);
    
//    public void draw2D(PApplet p);
    public void draw2D(PGraphics p);
    
    
}
