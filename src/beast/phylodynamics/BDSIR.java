package beast.phylodynamics;

import beast.core.Input;
import beast.core.Description;
import beast.core.Valuable;
import beast.core.parameter.IntegerParameter;
import beast.evolution.tree.Tree;
import beast.evolution.speciation.BirthDeathSkylineModel;

import java.util.Arrays;

/**
 * @author Denise K�hnert
 *
 */

@Description("Adaptation of Tanja Stadler's BirthDeathSamplingModel, to allow for birth and death rates to change at times t_i")
public class BDSIR extends BirthDeathSkylineModel {


    public Input<Valuable> S0_input =
            new Input<Valuable>("S0", "The numbers of susceptible individuals");

    public Input<IntegerParameter> m_dS =
            new Input<IntegerParameter>("dS", "dS vector containing the changes in numbers of susceptibles per location", Input.Validate.REQUIRED);
    public Input<IntegerParameter> m_dE =
            new Input<IntegerParameter>("dE", "dE vector containing the changes in numbers of susceptibles per location");
    public Input<IntegerParameter> m_dR =
            new Input<IntegerParameter>("dR", "dR vector containing the changes in numbers of susceptibles per location", Input.Validate.REQUIRED);

    Integer S0;
    Double bS0;
    Integer[] dS;
    Integer[] dE;
    Integer[] dR;

    Boolean recomputeSIR;
    double T;
    int ntaxa;

    Double[] birth_stored;
    Double[]  death_stored;
    Double[]  psi_stored;


    @Override
    public void initAndValidate() throws Exception {

        S0 = (int) (S0_input.get().getArrayValue());

        dS = m_dS.get().getValues();

        birthChanges = intervalNumber.get() - 1; //true; // must be true now since it is multiplied with S_i in every interval
        super.initAndValidate();

        if (transform){
            if (R0.get().getDimension() != 1)// || becomeUninfectiousRate.get().getDimension() != 1 || samplingProportion.get().getDimension() != 1)
                throw new RuntimeException("R0, becomeUninfectiousRate and samplingProportion have to be 1-dimensional!");
        } else {
            if (birthRate.get().getDimension() != 1)//  || death.length != 1 || psi.length != 1)
                throw new RuntimeException("Birth, death and sampling rate have to be 1-dimensional!");
        }

        T = m_tree.get().getRoot().getHeight() + orig_root.get().getValue();
        ntaxa = m_tree.get().getLeafNodeCount();

    }


    @Override
    public Double updateRatesAndTimes(Tree tree){

        super.updateRatesAndTimes(tree);

        T = tree.getRoot().getHeight() + orig_root.get().getValue();
        ntaxa = tree.getLeafNodeCount();

        S0 = (int) (S0_input.get().getArrayValue());

        dS = m_dS.get().getValues();

        dE = (m_dE.get() !=null)? m_dE.get().getValues(): (new Integer[dS.length]);
        if (dE[0]==null) Arrays.fill(dE,0);
        
        dR = m_dR.get().getValues();

        
        int cumS = S0 - 1 ;
        int dim = dS.length;
        double b = birth[0]/S0 ;
        double time; 

        birth = new Double[dim];
        int I = 1;
        int R = 0;

        birth[0] = b * cumS;
        for (int i = 0; i < dim-1; i++){

            cumS -= dS[i];
            birth[i+1] = b * cumS;

            I += dS[i] - dE[i] - dR[i];
            R += dR[i];
            time = (i+1)*T/(dim-1);

            if ( I<=0 || I < lineageCountAtTime(T-time, tree))
                return Double.NEGATIVE_INFINITY;

        }

        if (cumS < 0 || S0 - cumS < m_tree.get().getLeafNodeCount() || ( (cumS+I+R) != S0))
            return Double.NEGATIVE_INFINITY;

        return 0.;

    }


    @Override
    public Boolean isBDSIR(){
        return true;
    }


}
