/*
 * Good - used by CESoptimise... was inner class, but need it for e.g. testing backyard production elsewhere
 */

package location2012.econs;

import location2012.Firm;

/**
 *
 * @author Dan
 */
 //inner class, good
    public class Good {

        //initial slice sizes for budget split
        public double slice;
        //randomly jiggled position of slices; will keep if finds better optima,
        //or will revert to previous if not
        public double jiggledSlice;
        //
        public double amount, price;
        //for temporarily recording amounts left
        public double tempAmountLeft;
        //Temporary var: proposed amount of budget spend
        public double tempBudgetSpend;
        //ID of good. Makes code tidier elsewhere, e.g. allows foreaches
        public int id;
        //chosen amount, temporary
        public double chosenAmount;
        //The amount the final optimisation finds
        public double optimalChosenAmount;

        //for single actor model. Base price is the equivalent of time given to other
        //actors. Can store, to be removed later.
        public double basePrice;
        //For when I need to remove the record of the input from another actor
        public double contributedInput;

        //The firm producing this, so I can buy from them
        public GoodSeller gs;

        //Constructor for test version
        public Good(int id, double amount, double price) {

            this.id = id;
            this.amount = amount;
            tempAmountLeft = amount;
            this.price = price;

        }

        //constructor for live version
        public Good(int id, GoodSeller f, double price) {

            this.gs = f;
            this.id = id;
            //dont need stock for single actor...
            //but do for Firms. 
            this.amount = f.getGoodStock();
            tempAmountLeft = amount;
            this.price = price;

        }
        //constructor for single actor version.
        //BasePrice used to remove time from other actors.
        public Good(int id, GoodSeller f, double price, double basePrice) {

            this.gs = f;
            this.id = id;
            this.amount = f.getGoodStock();//singleActorPerson returns -1 here.
            tempAmountLeft = amount;
            this.price = price;
            this.basePrice = basePrice;

        }

        //constructor for constrained optimisers to use
        public Good() {
            
        }

    }//end inner class
