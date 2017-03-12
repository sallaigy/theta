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

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.impl.ConsoleLogger;
import hu.bme.mit.theta.formalism.cfa.CFA;
import hu.bme.mit.theta.frontend.benchmark.CfaConfigurationBuilder;
import hu.bme.mit.theta.frontend.benchmark.CfaConfigurationBuilder.PrecGranularity;
import hu.bme.mit.theta.frontend.benchmark.Configuration;
import hu.bme.mit.theta.frontend.benchmark.ConfigurationBuilder.Domain;
import hu.bme.mit.theta.frontend.benchmark.ConfigurationBuilder.Refinement;
import hu.bme.mit.theta.frontend.benchmark.ConfigurationBuilder.Search;
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

    public enum VerificationResult
    {
        SAFE, UNSAFE, ERROR, UNKNOWN
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        Options options = new Options();

        Option optFile = new Option("f", "file", true, "Path of the input file.");
        optFile.setRequired(true);
        optFile.setArgName("FILE");
        options.addOption(optFile);

        Option optIndividual = new Option("i", "individual", false, "Whether to check individual slices (default false)");
        optIndividual.setArgName("INDIVIDUAL");
        options.addOption(optIndividual);
        
        /* Options for the optimizer */
        
        Option optSlice = new Option("l", "slicer", true, "Slicing strategy");
        optSlice.setRequired(true);
        optSlice.setArgName("SLICER");
        options.addOption(optSlice);
        
        Option optOptimize = new Option("o", "optimizations", false, "Optimizations on/off (default off)");
        optOptimize.setArgName("OPTIMIZE");
        options.addOption(optOptimize);
        
        Option optRefinementSlicer = new Option("e", "refinement-slicer", true, "Refinement slicer");
        optRefinementSlicer.setRequired(false);
        optRefinementSlicer.setArgName("REFINEMENT_SLICER");
        options.addOption(optRefinementSlicer);

        /* Options for the CEGAR algorithm */
        
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
        
        Option optPrecGran = new Option("g", "precision-granularity", true, "Precision granularity (CONST/GEN)");
        optPrecGran.setRequired(true);
        optPrecGran.setArgName("GRANULARITY");
        options.addOption(optPrecGran);
        
        /* Other options */
        Option optVerbosity = new Option("v", "verbosity", true, "Logging verbosity level");
        optVerbosity.setRequired(false);
        optVerbosity.setArgName("VERBOSITY");
        options.addOption(optVerbosity);
                
        CommandLineParser cliParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = cliParser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
            helpFormatter.printHelp("theta.jar", options);
            return;
        }

        String filename = cmd.getOptionValue(optFile.getOpt());
        String slicerName = cmd.getOptionValue(optSlice.getOpt());
        Domain domain = Domain.valueOf(cmd.getOptionValue(optDomain.getOpt()));
        Refinement refinement = Refinement.valueOf(cmd.getOptionValue(optRefinement.getOpt()));
        Search search = Search.valueOf(cmd.getOptionValue(optSearch.getOpt()));
        PrecGranularity pg = PrecGranularity.valueOf(cmd.getOptionValue(optPrecGran.getOpt()));
        boolean optimize = cmd.hasOption(optOptimize.getOpt());
        boolean individual = cmd.hasOption(optIndividual.getOpt());
        
        // Optional arguments
        String verbosityVal = cmd.getOptionValue(optVerbosity.getOpt());
        String refinementSlicerVal = cmd.getOptionValue(optRefinementSlicer.getOpt());
        
        int verbosity = verbosityVal != null ? Integer.parseInt(verbosityVal) : 1;
                
        FunctionSlicer slicer = createSlicer(slicerName);
        FunctionSlicer refinementSlicer = refinementSlicerVal != null ? createSlicer(refinementSlicerVal) : new BackwardSlicer();
        
        GlobalContext context = Parser.parse(filename);
        Optimizer opt = new Optimizer(context, slicer);
        
        Logger log = new ConsoleLogger(verbosity);
        
        opt.setLogger(log);
        opt.addTransformation(new FunctionInliner());
        if (optimize) {
            opt.addTransformation(new ConstantPropagator());
            opt.addTransformation(new DeadBranchEliminator());
        }
                
        opt.transform();

        List<Slice> slices = opt.createSlices();
        VerificationResult result = VerificationResult.SAFE;
        
        boolean outCont = true;
        long verifTime = 0;
        
        log.writeln(String.format("Checking '%s' with the following configuration:", filename), 0);
        log.writeln(String.format("Slicer: %s", slicer.getClass().getSimpleName()), 0, 1);
        log.writeln(String.format("RefinementSlicer: %s", refinementSlicer.getClass().getSimpleName()), 0, 1);
        log.writeln(String.format("Individual slices: %s", individual), 0, 1);
        
        int i;
        for (i = 0; i < slices.size() && outCont; i++) {
            Slice slice = slices.get(i);
            slice.setRefinementSlicer(refinementSlicer);

            boolean cont = true;

            log.writeHeader("Slice #" + i, 1);
            while (cont) {
                Function cfg = slice.getSlicedFunction();
                CFA cfa = FunctionToCFATransformer.createLBE(cfg);
                
                Configuration<?,?,?> configuration = new CfaConfigurationBuilder(domain, refinement)
                        .search(search)
                        .precGranularity(pg)
                        .logger(log)
                        .build(cfa);
    
                SafetyResult<?, ?> status = configuration.check();
                                
                if (status.isUnsafe()) {
                    final Trace<?, ?> cex = status.asUnsafe().getTrace();
                    // The slice may require further refinement
                    cont = slice.canRefine();
                    if (!cont) {
                        log.writeln("No slice refinement is possible. Slice is UNSAFE", 7, 1);
                        // If no refinements are possible, this slice (and thus the whole program) is unsafe
                        result = VerificationResult.UNSAFE;
                        // If we do not wish check all slices, this is the time to stop
                        outCont = individual;
                    } else {
                        log.writeln("Slice refinement is possible. Refining...", 7, 1);
                        slice.refine();
                    }
                } else if (status.isSafe()) {
                    @SuppressWarnings("unchecked")
                    final ARG<? extends ExprState, ? extends ExprAction> arg = (ARG<? extends ExprState, ? extends ExprAction>) status.getArg();
                    final ArgChecker checker = ArgChecker.create(Z3SolverFactory.getInstace().createSolver());
                    if (!checker.isWellLabeled(arg) || !arg.isComplete() || !arg.isSafe()) {
                        throw new AssertionError("Arg is not complete/well-labeled");
                    }
                    
                    cont = false;
                } else {
                    throw new AssertionError();
                }
                
                verifTime += status.getStats().get().getElapsedMillis();
            }
        }
        
        log.writeHeader(String.format("Results for '%s'", filename), 0);
        log.writeln(String.format("Slice count: %d", slices.size()), 0);
        log.writeln(String.format("Checked slices: %d", i), 0);
        log.writeln(String.format("Verification time: %d ms", verifTime), 1);
        log.writeln(String.format("Verification result: %s", result.toString()), 0);
    }

    private static FunctionSlicer createSlicer(String slicerName) {
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
                slicer = new IdentitySlicer();
                break;
            default:
                throw new RuntimeException("Unknown slicer algorithm");
        }
        
        return slicer;
    }

}