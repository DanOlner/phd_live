/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.geog;

import java.awt.geom.Point2D;

/**
 * HasLocation: interface for requiring geographical information
 * from a class
 *
 * @author Olner Dan
 */
public interface HasLocation {

    void setSpace(SpaceTools t);

    SpaceTools getSpace();

    double getx();

    double gety();

    void setx(double x);

    void sety(double y);

    void setPoint(Point2D.Double pt);

    Point2D.Double getPoint();

    //for HasLocation objects to store query returns from Torus
    void addLocMemory(LocMemory lm);

    //for moving location
    void moveTo(Point2D.Double pt);
    
}
