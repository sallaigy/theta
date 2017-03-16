package hu.bme.mit.theta.frontend.c.benchmark;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.Statistics;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.impl.ConsoleLogger;
import hu.bme.mit.theta.formalism.cfa.CFA;
import hu.bme.mit.theta.formalism.sts.STS;
import hu.bme.mit.theta.frontend.aiger.impl.AigerParserSimple;
import hu.bme.mit.theta.frontend.benchmark.CfaConfigurationBuilder;
import hu.bme.mit.theta.frontend.benchmark.Configuration;
import hu.bme.mit.theta.frontend.benchmark.StsConfigurationBuilder;
import hu.bme.mit.theta.frontend.benchmark.ConfigurationBuilder.Domain;
import hu.bme.mit.theta.frontend.benchmark.ConfigurationBuilder.Refinement;
import hu.bme.mit.theta.frontend.benchmark.ConfigurationBuilder.Search;
import hu.bme.mit.theta.frontend.benchmark.StsConfigurationBuilder.InitPrec;
import hu.bme.mit.theta.frontend.c.Optimizer;
import hu.bme.mit.theta.frontend.c.cfa.FunctionToCFATransformer;
import hu.bme.mit.theta.frontend.c.ir.GlobalContext;
import hu.bme.mit.theta.frontend.c.parser.Parser;
import hu.bme.mit.theta.frontend.c.transform.ConstantPropagator;
import hu.bme.mit.theta.frontend.c.transform.DeadBranchEliminator;
import hu.bme.mit.theta.frontend.c.transform.FunctionInliner;
import hu.bme.mit.theta.frontend.c.transform.slicer.FunctionSlicer;
import hu.bme.mit.theta.frontend.c.transform.slicer.IdentitySlicer;
import hu.bme.mit.theta.frontend.c.transform.slicer.Slice;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;

public class Sandbox {

    
    public static void main(final String[] args) throws FileNotFoundException, IOException {
        System.out.println("Press a key to start");
        System.in.read();

        /* Input parameters */        
        String file = "";
        FunctionSlicer slicer = new IdentitySlicer();

        GlobalContext context = Parser.parse(file);
        Optimizer opt = new Optimizer(context, slicer);
        
        /* Add arbitrary transformations here */
        opt.addTransformation(new FunctionInliner());
        opt.addTransformation(new ConstantPropagator());
        opt.addTransformation(new DeadBranchEliminator());
        
        opt.transform();
        
        List<Slice> slices = opt.createSlices();
        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            
            /* Use createSBE for single-block encoding, createLBE for large-block encoding */
            CFA cfa = FunctionToCFATransformer.createLBE(slice.getSlicedFunction());
        }

        
        
        
    }
    
}
