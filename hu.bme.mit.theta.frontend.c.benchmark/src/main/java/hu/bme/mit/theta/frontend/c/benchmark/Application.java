package hu.bme.mit.theta.frontend.c.benchmark;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.management.RuntimeErrorException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.base.Stopwatch;

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.Statistics;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.impl.ConsoleLogger;
import hu.bme.mit.theta.common.logging.impl.FileLogger;
import hu.bme.mit.theta.common.logging.impl.NullLogger;
import hu.bme.mit.theta.common.table.TableWriter;
import hu.bme.mit.theta.common.table.impl.SimpleTableWriter;
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
        TableWriter tw = new SimpleTableWriter(System.out, ",", "\"", "\"");

        // If only called with a single --header argument, print header and exit
        if (args.length == 1 && "--header".equals(args[0])) {
            /*File,Slice No.,Slicer,Optimizations,Domain,Search,Refinement,InitLocs,InitEdges,Iterations,ArgSize,ArgDepth,EndLocs,EndEdges,Optimization Time,Verification Time,Refinements,Status*/
/*            tw.cell("File");
            tw.cell("Slice No.");
            tw.cell("Slicer");
            //tw.cell("RefinementSlicer");
            tw.cell("Optimizations");
            tw.cell("Domain");
            tw.cell("Search");
            tw.cell("Refinement"); */
            //tw.cell("PrecGranuality");
            tw.cell("InitLocs");
            tw.cell("InitEdges");
            tw.cell("Iterations");
            tw.cell("ArgSize");
            tw.cell("ArgDepth");
            tw.cell("EndLocs");
            tw.cell("EndEdges");
            tw.cell("Optimization Time");
            tw.cell("Verification Time");
            tw.cell("Refinements");
            tw.cell("Status");
            tw.newRow();

            return;
        }


        Options options = new Options();

        Option optFile = new Option("f", "file", true, "Path of the input file.");
        optFile.setRequired(true);
        optFile.setArgName("FILE");
        options.addOption(optFile);

        Option optIndividual = new Option("i", "individual", false, "Whether to check individual slices (default false)");
        optIndividual.setArgName("INDIVIDUAL");
        options.addOption(optIndividual);

        Option optSliceNumber = new Option("n", "slice-num", true, "Slice number (only meaningful with -i)");
        optSliceNumber.setRequired(false);
        optSliceNumber.setArgName("SLICE_NO");
        options.addOption(optSliceNumber);

        /* Options for the optimizer */

        Option optSlice = new Option("l", "slicer", true, "Slicing strategy");
        optSlice.setRequired(true);
        optSlice.setArgName("SLICER");
        options.addOption(optSlice);

        Option optOptimize = new Option("o", "optimizations", true, "Optimization level");
        optOptimize.setRequired(true);
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
        Option optLogfile = new Option("lf", "log-file", true, "Logger file");
        optLogfile.setRequired(false);
        optLogfile.setArgName("LOGFILE");
        options.addOption(optLogfile);

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
        Integer optimizeLvl = Integer.parseInt(cmd.getOptionValue(optOptimize.getOpt()));
      //  boolean individual = cmd.hasOption(optIndividual.getOpt());
        boolean individual = true;
        boolean optimize = optimizeLvl == 1;

        // Optional arguments
        String verbosityVal = cmd.getOptionValue(optVerbosity.getOpt());
        String refinementSlicerVal = cmd.getOptionValue(optRefinementSlicer.getOpt());

        int verbosity = verbosityVal != null ? Integer.parseInt(verbosityVal) : 1;
        int initialSliceN = individual ? Integer.parseInt(cmd.getOptionValue(optSliceNumber.getOpt())) : 0;

        Logger log;

        if (cmd.hasOption(optLogfile.getOpt())) {
            log = new FileLogger(verbosity, filename, true, false);
        } else {
            log = NullLogger.getInstance();
        }


        FunctionSlicer slicer = createSlicer(slicerName);
        FunctionSlicer refinementSlicer = refinementSlicerVal != null ? createSlicer(refinementSlicerVal) : new BackwardSlicer();

        GlobalContext context = Parser.parse(filename);
        Optimizer opt = new Optimizer(context, slicer);


        opt.setLogger(log);
        opt.addTransformation(new FunctionInliner());
        if (optimize) {
            opt.addTransformation(new ConstantPropagator());
            opt.addTransformation(new DeadBranchEliminator());
        }

        Stopwatch sw = Stopwatch.createUnstarted();

        sw.start();

        opt.transform();
        List<Slice> slices = opt.createSlices();

        sw.stop();

        long optTime = sw.elapsed(TimeUnit.MILLISECONDS);

        VerificationResult result = VerificationResult.SAFE;

        log.writeln(String.format("Checking '%s' with the following configuration:", filename), 0);
        log.writeln(String.format("Slicer: %s", slicer.getClass().getSimpleName()), 0, 1);
        log.writeln(String.format("RefinementSlicer: %s", refinementSlicer.getClass().getSimpleName()), 0, 1);
        log.writeln(String.format("Individual slices: %s", individual), 0, 1);

        int sliceMax = individual ? initialSliceN + 1 : slices.size();

        for (int i = initialSliceN; i < sliceMax; i++) {
            Slice slice = slices.get(i);
            slice.setRefinementSlicer(refinementSlicer);
/*
            tw.cell(filename);
            tw.cell(i);
            tw.cell(slicer.getClass().getSimpleName());
         //   tw.cell(refinementSlicer.getClass().getSimpleName());
            tw.cell(optimize);
            tw.cell(domain.toString());
            tw.cell(search.toString());
            tw.cell(refinement.toString()); */
        //    tw.cell(pg.toString());

            // Get initial sizes
            CFA initCfa = FunctionToCFATransformer.createLBE(slice.getSlicedFunction());

            tw.cell(initCfa.getLocs().size());
            tw.cell(initCfa.getEdges().size());

            result = checkSlice(tw, domain, refinement, search, pg, log, result, i, slice, optTime);
            tw.newRow();

            if (!individual && result == VerificationResult.UNSAFE) {
                // If we are not checking individual slices and this slice was unsafe, it is time to stop.
                log.writeln(String.format("Slice %d status is unsafe.", i), 0);
                log.writeln(String.format("Program is UNSAFE."), 0);
                break;
            }
        }
    }

    private static VerificationResult checkSlice(TableWriter tw, Domain domain, Refinement refinement, Search search,
            PrecGranularity pg, Logger log, VerificationResult result, int i, Slice slice, long optTime) throws AssertionError {
        boolean cont = true;

        long sliceVerifTime = 0;
        long sliceRefinementTime = optTime;
        long refinementCnt = 0;

        Stopwatch sw = Stopwatch.createUnstarted();

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
            Statistics stats = status.getStats().get();


            if (status.isUnsafe()) {
                // final Trace<?, ?> cex = status.asUnsafe().getTrace();
                // The slice may require further refinement
                cont = slice.canRefine();
                if (!cont) {
                    log.writeln("No slice refinement is possible. Slice is UNSAFE", 7, 1);
                    // If no refinements are possible, this slice (and thus the whole program) is unsafe
                    result = VerificationResult.UNSAFE;
                } else {
                    log.writeln("Slice refinement is possible. Refining...", 7, 1);
                    sw.reset();
                    sw.start();

                    slice.refine();

                    sw.stop();
                    refinementCnt++;
                    sliceRefinementTime += sw.elapsed(TimeUnit.MILLISECONDS);
                }
            } else if (status.isSafe()) {
                // The slice is safe, no refinements needed
                cont = false;
            } else {
                throw new AssertionError();
            }

            sliceVerifTime += stats.getElapsedMillis();

            if (!cont) {
                tw.cell(stats.getIterations());
                tw.cell(status.getArg().size());
                tw.cell(status.getArg().getDepth());
                tw.cell(cfa.getLocs().size());
                tw.cell(cfa.getEdges().size());
                tw.cell(sliceRefinementTime);
                tw.cell(sliceVerifTime);
                tw.cell(refinementCnt);
                tw.cell(result.toString());
            } else {
              //  verifTime += status.getStats().get().getElapsedMillis();
            }
        }
        return result;
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