package hu.bme.mit.theta.frontend.c.benchmark;

import java.io.IOException;
import java.util.List;

import javax.management.RuntimeErrorException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.Precision;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyStatus;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.utils.ArgVisualizer;
import hu.bme.mit.theta.analysis.utils.TraceVisualizer;
import hu.bme.mit.theta.common.logging.impl.ConsoleLogger;
import hu.bme.mit.theta.common.visualization.GraphvizWriter;
import hu.bme.mit.theta.formalism.cfa.CFA;
import hu.bme.mit.theta.formalism.cfa.utils.CfaVisualizer;
import hu.bme.mit.theta.frontend.benchmark.CfaConfigurationBuilder;
import hu.bme.mit.theta.frontend.benchmark.StsConfigurationBuilder;
import hu.bme.mit.theta.frontend.benchmark.CfaConfigurationBuilder.Domain;
import hu.bme.mit.theta.frontend.benchmark.CfaConfigurationBuilder.Refinement;
import hu.bme.mit.theta.frontend.benchmark.CfaConfigurationBuilder.Search;
import hu.bme.mit.theta.frontend.benchmark.Configuration;
import hu.bme.mit.theta.frontend.c.Optimizer;
import hu.bme.mit.theta.frontend.c.cfa.FunctionToCFATransformer;
import hu.bme.mit.theta.frontend.c.ir.Function;
import hu.bme.mit.theta.frontend.c.ir.GlobalContext;
import hu.bme.mit.theta.frontend.c.parser.Parser;
import hu.bme.mit.theta.frontend.c.transform.ConstantPropagator;
import hu.bme.mit.theta.frontend.c.transform.DeadBranchEliminator;
import hu.bme.mit.theta.frontend.c.transform.FunctionInliner;
import hu.bme.mit.theta.frontend.c.transform.slicer.BackwardSlicer;
import hu.bme.mit.theta.frontend.c.transform.slicer.FunctionSlicer;
import hu.bme.mit.theta.frontend.c.transform.slicer.IdentitySlicer;
import hu.bme.mit.theta.frontend.c.transform.slicer.Slice;
import hu.bme.mit.theta.frontend.c.transform.slicer.ThinSlicer;
import hu.bme.mit.theta.frontend.c.transform.slicer.ValueSlicer;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;

public class Application {

    public static void main(String[] args) throws IOException, InterruptedException {
        Options options = new Options();

        Option optFile = new Option("f", "file", true, "Path of the input file.");
        optFile.setRequired(true);
        optFile.setArgName("FILE");
        options.addOption(optFile);

        Option optSlice = new Option("s", "slicer", true, "Slicing strategy");
        optSlice.setRequired(true);
        optSlice.setArgName("SLICER");
        options.addOption(optSlice);

        Option optDomain = new Option("d", "domain", true, "Abstract domain");
        optDomain.setRequired(true);
        optDomain.setArgName("DOMAIN");
        options.addOption(optDomain);

        Option optRefinement = new Option("r", "refinement", true, "Refinement strategy");
        optRefinement.setRequired(true);
        optRefinement.setArgName("REFINEMENT");
        options.addOption(optRefinement);
        
        Option optSearch = new Option("s", "search", true, "Search strategy");
        optSearch.setRequired(true);
        optSearch.setArgName("SEARCH");
        options.addOption(optSearch);
        
        Option optOptimize = new Option("o", "optimizations", false, "Optimizations on/off");
        optOptimize.setArgName("OPTIMIZE");
        options.addOption(optOptimize);
        
        CommandLineParser cliParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = cliParser.parse(options, args);
        } catch (ParseException e) {
            helpFormatter.printHelp("theta.jar", options);
            return;
        }

        String filename = cmd.getOptionValue(optFile.getOpt());
        String slicerName = cmd.getOptionValue(optSlice.getOpt());
        Domain domain = Domain.valueOf(cmd.getOptionValue(optDomain.getOpt()));
        Refinement refinement = Refinement.valueOf(cmd.getOptionValue(optRefinement.getOpt()));
        Search search = Search.valueOf(cmd.getOptionValue(optSearch.getOpt()));
        
        boolean optimize = cmd.hasOption(optOptimize.getOpt());

        FunctionSlicer slicer;
        
        switch (slicerName) {
            case "backward":
                slicer = new BackwardSlicer();
                break;
            case "value":
                slicer = new ValueSlicer();
                break;
            case "thin":
                slicer = new ThinSlicer();
                break;
            case "none":
            default:
                slicer = new IdentitySlicer();
                break;
        }
        
        GlobalContext context = Parser.parse(filename);
        Optimizer opt = new Optimizer(context, slicer);
        
        opt.setLogger(new ConsoleLogger(100));
        opt.addTransformation(new FunctionInliner());
        if (optimize) {
            opt.addTransformation(new ConstantPropagator());
            opt.addTransformation(new DeadBranchEliminator());
        }
                
        opt.transform();

        List<Slice> slices = opt.createSlices();
        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            
            Function cfg = slice.getSlicedFunction();
            CFA cfa = FunctionToCFATransformer.createLBE(cfg);
            
            Configuration<?,?,?> configuration = new CfaConfigurationBuilder(domain, refinement).search(search).logger(new ConsoleLogger(3)).build(cfa);
                        
            SafetyStatus<?, ?> status = configuration.check();
                                    
            if(status.isUnsafe()) {
                final Trace<?, ?> cex = status.asUnsafe().getTrace();
                System.out.println("CEX length: " + cex.length());
            } else if (status.isSafe()) {
                @SuppressWarnings("unchecked")
                final ARG<? extends ExprState, ? extends ExprAction> arg = (ARG<? extends ExprState, ? extends ExprAction>) status.getArg();
                final ArgChecker checker = ArgChecker.create(Z3SolverFactory.getInstace().createSolver());
                if (!checker.isWellLabeled(arg) || !arg.isComplete() || !arg.isSafe()) {
                    throw new AssertionError("Arg is not complete/well-labeled");
                }
            } else {
                throw new AssertionError();
            }
        }
    }

}