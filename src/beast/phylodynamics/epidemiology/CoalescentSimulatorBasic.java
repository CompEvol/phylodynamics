package beast.phylodynamics.epidemiology;

import beast.core.Description;
import beast.core.Input;
import beast.core.Logger;
import beast.evolution.tree.RandomTree;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.coalescent.PopulationFunction;
import java.util.ArrayList;
import java.util.List;

/**
 * @author remco@cs.waikato.ac.nz
 * @author Alexei Drummond
 */
@Description("Simulate coalescent trees given a population function.")
public class CoalescentSimulatorBasic extends beast.core.Runnable {
    
    public Input<PopulationFunction> populationFunctionInput
            = new Input<PopulationFunction>(
                    "populationFunction",
                    "the population function",
                    Input.Validate.REQUIRED);
    
    public Input<Integer> replicatesInput = new Input<Integer>(
            "replicates",
            "the number of replicates", Input.Validate.REQUIRED);

    public Input<TraitSet> timeTraitInput = new Input<TraitSet>(
            "trait",
            "trait information for initializing traits (like node dates) in the tree",
            Input.Validate.REQUIRED);
    
    public Input<Double> maxHeightInput = new Input<Double>(
            "maxHeight",
            "Used to optionally constrain the height of the generated tree "
                    + "to be below the start of the epidemic.  Trees will "
                    + "be generated until the height is found to be smaller "
                    + "than this value.",
            Double.POSITIVE_INFINITY);
    
        
    public Input<List<Logger>> loggersInput = new Input<List<Logger>>(
            "logger",
            "Logger used to write results to screen or disk.",
            new ArrayList<Logger>());    

    PopulationFunction populationFunction;
    PopulationFunction.Abstract fullPopFunc;

    TraitSet timeTraitSet;
    int replicates;
    String outputFileName;


    @Override
    public void initAndValidate() throws Exception {

        replicates = replicatesInput.get();
        populationFunction = populationFunctionInput.get();
        timeTraitSet = timeTraitInput.get();

        if (!timeTraitSet.isDateTrait())
            throw new IllegalArgumentException("Trait set must be a date "
                    + "(forward or backward) trait set.");
        
        for (int i=0; i<timeTraitSet.taxaInput.get().asStringList().size(); i++) {
            if (Double.isInfinite(populationFunction.getPopSize(timeTraitSet.getValue(i))))
                throw new IllegalStateException("Intensity is non-finite at "
                        + "one or more sample times.");
        }
        
        
        if (populationFunction instanceof PopulationFunction.Abstract)
            fullPopFunc = (PopulationFunction.Abstract)populationFunction;
    }

    @Override
    public void run() throws Exception {

        // Initialise loggers
        for (Logger logger : loggersInput.get()) {
            logger.init();
        }

        RandomTree tree = new RandomTree();
        for (int i = 0; i < replicates; i++) {
            
            // Iterate until we get a tree meeting the requirements.
            do {
                if (fullPopFunc != null)
                    fullPopFunc.prepare();
                tree.initByName(
                        "taxonset", timeTraitSet.taxaInput.get(),
                        "trait", timeTraitSet,
                        "populationModel", populationFunction);
            } while (!(tree.getRoot().getHeight()<maxHeightInput.get()));
                    
            // Log state
            for (Logger logger : loggersInput.get()) {
                logger.log(i);
            }
        }
        
        // Finalize loggers
        for (Logger logger : loggersInput.get()) {
            logger.close();
        }
    }
}

