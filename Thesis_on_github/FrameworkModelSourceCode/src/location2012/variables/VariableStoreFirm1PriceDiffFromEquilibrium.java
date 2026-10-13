/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.variables;

import java.lang.reflect.Field;
import java.util.ArrayList;
import location2012.Firm;
import location2012.utils.gl;

/**
 * VariableStore stores a single variable's data - either a direct variable from
 * a model object or a calculated derivation of one
 *
 * @author geodo
 */
public class VariableStoreFirm1PriceDiffFromEquilibrium extends VariableStore {

    Firm f;

    public VariableStoreFirm1PriceDiffFromEquilibrium(ArrayList arrayList, String varName) {

        super(arrayList, varName);

//        this.weight = weight;
        this.objects = arrayList;
        this.varName = varName;
        this.varNameLabel = varName;

    }

    //If label for variable is different from varName - for instance
    //If getting x,y coords for people and firms separately, need way to find them
    public VariableStoreFirm1PriceDiffFromEquilibrium(ArrayList arrayList, String varName, String varNameLabel) {

        super(arrayList, varName, varNameLabel);

//        this.weight = weight;
        this.objects = arrayList;
        this.varName = varName;
        this.varNameLabel = varNameLabel;


    }

    public void acquireVariable() {


        //Just storing a value for Firm1's difference between 
        //equilibrium price when markup is zero and their actual price
        variableData.add(new Double[1]);

//        int index = 0;
        
        //equilibrium value deduced from assuming demand split from one person evenly
        
        
        double eqPrice = 0.5/((gl.EoS_Curve * 0.5 * 0.5) + 0.5);
//        System.out.println("eqPrce:"+eqPrice);
        
        f = (Firm) gl.firms.get(1);
//        System.out.println("goodc: " + f.goodCost);
        
        variableData.get(variableData.size() - 1)[0] = f.goodCost - eqPrice;
        
//        System.out.println("val: " + variableData.get(variableData.size() - 1)[0]);

    }//end method acquireVariable

    /**
     * Return most recent ArrayList addition to the VariableStore
     *
     * @return
     */
    public Double[] getLatestArrayListOfData() {

//        System.out.println("variableData: " + varName + " " + variableData.size());

//        if (variableData.size() > 0) {
        return variableData.get(variableData.size() - 1);
//        } else {
//            return new ArrayList<Double>();
//        }

    }//end method getLatestArrayListOfData
}
