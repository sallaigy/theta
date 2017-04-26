package hu.bme.mit.theta.frontend.c.benchmark;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.StringJoiner;

import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.utils.ArgVisualizer;
import hu.bme.mit.theta.analysis.utils.TraceVisualizer;
import hu.bme.mit.theta.common.logging.impl.NullLogger;
import hu.bme.mit.theta.common.visualization.GraphvizWriter;
import hu.bme.mit.theta.formalism.cfa.CFA;
import hu.bme.mit.theta.formalism.cfa.utils.CfaVisualizer;
import hu.bme.mit.theta.frontend.benchmark.CfaConfigurationBuilder;
import hu.bme.mit.theta.frontend.benchmark.Configuration;
import hu.bme.mit.theta.frontend.benchmark.CfaConfigurationBuilder.PrecGranularity;
import hu.bme.mit.theta.frontend.benchmark.ConfigurationBuilder.Domain;
import hu.bme.mit.theta.frontend.benchmark.ConfigurationBuilder.Refinement;
import hu.bme.mit.theta.frontend.benchmark.ConfigurationBuilder.Search;
import hu.bme.mit.theta.frontend.c.Optimizer;
import hu.bme.mit.theta.frontend.c.cfa.FunctionToCFATransformer;
import hu.bme.mit.theta.frontend.c.ir.Function;
import hu.bme.mit.theta.frontend.c.ir.GlobalContext;
import hu.bme.mit.theta.frontend.c.parser.Parser;
import hu.bme.mit.theta.frontend.c.transform.FunctionInliner;
import hu.bme.mit.theta.frontend.c.transform.slicer.BackwardSlicer;
import hu.bme.mit.theta.frontend.c.transform.slicer.Slice;

public class Demo {

    public static void main(String[] args) throws FileNotFoundException {
        if (args.length != 1) {
            System.err.println("USAGE: theta-c <path>");
            return;
        }
        
        String filename = args[0];
        
        GlobalContext context = Parser.parse(filename);
        
        Optimizer opt = new Optimizer(context, new BackwardSlicer());
        opt.addTransformation(new FunctionInliner());
        opt.transform();
        
        GraphvizWriter writer = new GraphvizWriter();
        
        List<Slice> slices = opt.createSlices(); 
        
        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            
            Function function = slice.getSlicedFunction();
            
            System.out.println(String.format("Checking function '%s' #%d...", function.getName(), i));
            
            CFA cfa = FunctionToCFATransformer.createLBE(function);
            writer.writeFile(CfaVisualizer.visualize(cfa), "cfa_" + function.getName() + "_" + i + ".dot");
            
            Configuration<?,?,?> configuration = new CfaConfigurationBuilder(Domain.PRED, Refinement.SEQ_ITP)
                .search(Search.DFS)
                .precGranularity(PrecGranularity.CONST)
                .logger(NullLogger.getInstance())
                .build(cfa);
            
            SafetyResult<?, ?> result = configuration.check();
            
            writer.writeFile(ArgVisualizer.visualize(result.getArg()), "arg_"+ function.getName() + "_" + i + ".dot");
            
            if (result.isSafe()) {
                System.out.println(String.format("> Check finished. Result is SAFE."));
            } else if (result.isUnsafe()) {
                System.out.println(String.format("> Check finished. Result is UNSAFE."));
                System.out.println(String.format("> Written trace dump into '%s'.", "trace_"+ function.getName() + "_" + i + ".dot"));
                
                writer.writeFile(TraceVisualizer.visualize(result.asUnsafe().getTrace()), "trace_"+ function.getName() + "_" + i + ".dot");

            } else {
                System.out.println("> Check finished. Result is UNKNOWN.");
            }
        }
        
    }

}
