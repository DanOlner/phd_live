package location2012.io;

import au.com.bytecode.opencsv.CSVReader;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import location2012.Actor;
import location2012.Firm;
import location2012.FirmAct;
import location2012.Main;
import location2012.Person;
import location2012.PersonAction;
import location2012.geog.HasLocation;
import location2012.geog.SpaceTools;
import location2012.utils.PullOneClassFromArray;
import location2012.utils.gl;
import location2012.utils.p;

//Uses http://opencsv.sourceforge.net/
public class DataReader {

    Main m;
    SpaceTools sp;
    ArrayList<Actor> actors;
    CSVReader reader;

    public DataReader(ArrayList<Actor> actors, Main m, SpaceTools sp) {

        this.actors = actors;
        this.m = m;
        this.sp = sp;

        try {
            read();
        } catch (IOException ex) {
            p.p("couldn't read file: " + ex);
        }

    }

        public void read() throws IOException {

        int ID = 0;

        //this file's first entry contains the world's width
        reader = new CSVReader(new FileReader("data/people_x.csv"));
        String[] nextLine;
        boolean readWidth = true;
        boolean findNumPeople = true;
        int modelRunDays = 0;

        while ((nextLine = reader.readNext()) != null) {

            //read width if on first line
            if (readWidth) {
                sp.width = Integer.parseInt(nextLine[0]);
                readWidth = false;
            }

            //Find out how many people there are at the same time
            //don't use first line - it's only one slot.
            if (modelRunDays == 2) {
                gl.numPeople = nextLine.length;
                System.out.println("numPeople found: " + gl.numPeople);
                //findNumPeople = false;
            }

            //Next, how many modelRunDays are there?
            modelRunDays++;

        }

        gl.modelRunDays = modelRunDays;

        /*
         * Make agents
         */
        for (int i = 0; i < gl.numPeople; i++) {

            actors.add(new Person(ID++));
            actors.get(i).addAction(new PersonAction(actors.get(i)));

        }

        //Firms - use the record of good1 and 2 to make these
        reader = new CSVReader(new FileReader("data/firms_contributedTime.csv"));

        nextLine = reader.readNext();

        gl.numFirms = nextLine.length - 1;

        //seems nextLine length is one more than the actual data - is the last entry some return character?
        //doesn't seem to be the case for numPeople above - I'm confused.
        //Might be something to do with the line chosen; perhaps the second would be the right number? Odd.
        for (int i = 0; i < nextLine.length-1; i++) {

                actors.add(new Firm(ID++));
            

            //stick firms' action in.
            actors.get(gl.numPeople + i).addAction(new FirmAct(actors.get(gl.numPeople + i)));

        }//end for over nextLine

        //finish up
        for (Actor a : actors) {
        
            //Actors implement HasLocation, so need to set a ref to the torus
            a.setSpace(sp);

            //I'll also be kind and give the torus its own HasLocation refs
            sp.isInSpace.add((HasLocation) a);

        }

            System.out.println("People: " + gl.numPeople + ", firms: " + gl.numFirms);


        /*
         * Array creation: now have to make a bunch of arrays that will be used to populate the actors with data
         *
         * Using DataStore structure it's fairly straightforward - 
         * 
         */

        DataStore ds = new DataStore(gl.time, 1,2);

        ArrayList<Person> people = new ArrayList<Person>();
//        PullOneClassFromArray.PULLCLASS(actors, people, new Person(0));
        PullOneClassFromArray.PULLCLASS(actors, people, Person.class);

        ArrayList<Firm> firms = new ArrayList<Firm>();
//        PullOneClassFromArray.PULLCLASS(actors, firms, new Firm(0));
        PullOneClassFromArray.PULLCLASS(actors, firms, Firm.class);

//        ds.addVariableStore(new AllFirmsContribTime("firms_contributedTime", firms, modelRunDays));
//        ds.addVariableStore(new AllUtilityBucket("AllPeoplesUtility", people, modelRunDays));
//        ds.addVariableStore(new AllPeoplePaidWorkStatus("people_paidworkstatus", people, modelRunDays));
//        ds.addVariableStore(new AllPeopleX("people_x", people, modelRunDays));
//        ds.addVariableStore(new AllPeopleY("people_y", people, modelRunDays));
//        ds.addVariableStore(new AllFirmsX("firms_x", firms, modelRunDays));
//        ds.addVariableStore(new AllFirmsY("firms_y", firms, modelRunDays));


    }//end method read
}


/*
 * CUTTINZ
 *
 *  //test bucket vars
        Bucket b = ds.buckets.get(1);

            for (int i = 0; i < b.vals.length; i++) {

                System.out.println(" ");

                for (int j = 0; j < b.vals[1].length; j++) {

                    System.out.print(b.vals[i][j] + " ");

                }

            }

 */