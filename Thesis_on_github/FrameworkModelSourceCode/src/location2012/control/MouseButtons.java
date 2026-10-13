/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.control;

/**
 *  For VarChangers to indicate what mousebuttons they want to be accessible to
 *
 * @author Dan
 */
public interface MouseButtons {

    public void setLeft(boolean l);

    public void setRight(boolean r);

    public boolean useLeft();

    public boolean useRight();

    public void setName(String name);

    public String getName();

    public void setID(int id);

    public int getID();
}
