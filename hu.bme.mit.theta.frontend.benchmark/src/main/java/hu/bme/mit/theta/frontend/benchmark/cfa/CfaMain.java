package hu.bme.mit.theta.frontend.benchmark.cfa;

import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.base.Stopwatch;

import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarStatistics;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.impl.FileLogger;
import hu.bme.mit.theta.common.logging.impl.NullLogger;
import hu.bme.mit.theta.common.table.TableWriter;
import hu.bme.mit.theta.common.table.impl.SimpleTableWriter;
import hu.bme.mit.theta.formalism.cfa.CFA;
import hu.bme.mit.theta.frontend.benchmark.Configuration;
import hu.bme.mit.theta.frontend.benchmark.ConfigurationBuilder.Domain;
import hu.bme.mit.theta.frontend.benchmark.ConfigurationBuilder.Refinement;
import hu.bme.mit.theta.frontend.benchmark.ConfigurationBuilder.Search;
import hu.bme.mit.theta.frontend.benchmark.cfa.CfaConfigurationBuilder.PrecGranularity;
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

public class CfaMain {

	public enum VerificationResult {
		SAFE, UNSAFE, ERROR, UNKNOWN
	}

	public static void main(final String[] args) throws IOException, InterruptedException {
		final TableWriter tw = new SimpleTableWriter(System.out, ",", "\"", "\"");

		// If only called with a single --header argument, print header and exit
		if (args.length == 1 && "--header".equals(args[0])) {
			/*
			 * File,Slice
			 * No.,Slicer,Optimizations,Domain,Search,Refinement,InitLocs,
			 * InitEdges,Iterations,ArgSize,ArgDepth,EndLocs,EndEdges,
			 * Optimization Time,Verification Time,Refinements,Status
			 */
			/*
			 * tw.cell("File"); tw.cell("Slice No."); tw.cell("Slicer");
			 * //tw.cell("RefinementSlicer"); tw.cell("Optimizations");
			 * tw.cell("Domain"); tw.cell("Search"); tw.cell("Refinement");
			 */
			// tw.cell("PrecGranuality");
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

		final Options options = new Options();

		final Option optFile = new Option("f", "file", true, "Path of the input file.");
		optFile.setRequired(true);
		optFile.setArgName("FILE");
		options.addOption(optFile);

		// final Option optIndividual = new Option("i", "individual", false,
		// "Whether to check individual slices (default false)");
		// optIndividual.setArgName("INDIVIDUAL");
		// options.addOption(optIndividual);

		final Option optSliceNumber = new Option("n", "slice-num", true, "Slice number");
		optSliceNumber.setRequired(false);
		optSliceNumber.setArgName("SLICE_NO");
		options.addOption(optSliceNumber);

		/* Options for the optimizer */

		final Option optSlice = new Option("l", "slicer", true, "Slicing strategy");
		optSlice.setRequired(true);
		optSlice.setArgName(options(Slicer.values()));
		options.addOption(optSlice);

		final Option optOptimize = new Option("o", "optimizations", true, "Optimization level");
		optOptimize.setRequired(true);
		optOptimize.setArgName("OPTIMIZE");
		options.addOption(optOptimize);

		final Option optRefinementSlicer = new Option("e", "refinement-slicer", true, "Refinement slicer");
		optRefinementSlicer.setRequired(false);
		optRefinementSlicer.setArgName(options(Slicer.values()));
		options.addOption(optRefinementSlicer);

		/* Options for the CEGAR algorithm */

		final Option optDomain = new Option("d", "domain", true, "Abstract domain");
		optDomain.setRequired(true);
		optDomain.setArgName(options(Domain.values()));
		options.addOption(optDomain);

		final Option optRefinement = new Option("r", "refinement", true, "Refinement strategy");
		optRefinement.setRequired(true);
		optRefinement.setArgName(options(Refinement.values()));
		options.addOption(optRefinement);

		final Option optSearch = new Option("s", "search", true, "Search strategy");
		optSearch.setRequired(true);
		optSearch.setArgName(options(Search.values()));
		options.addOption(optSearch);

		final Option optPrecGran = new Option("g", "precision-granularity", true, "Precision granularity");
		optPrecGran.setRequired(true);
		optPrecGran.setArgName(options(PrecGranularity.values()));
		options.addOption(optPrecGran);

		/* Other options */
		final Option optLogfile = new Option("lf", "log-file", true, "Logger file");
		optLogfile.setRequired(false);
		optLogfile.setArgName("LOGFILE");
		options.addOption(optLogfile);

		final Option optVerbosity = new Option("v", "verbosity", true, "Logging verbosity level");
		optVerbosity.setRequired(false);
		optVerbosity.setArgName("VERBOSITY");
		options.addOption(optVerbosity);

		final CommandLineParser cliParser = new DefaultParser();
		final HelpFormatter helpFormatter = new HelpFormatter();
		CommandLine cmd;

		try {
			cmd = cliParser.parse(options, args);
		} catch (final ParseException e) {
			System.out.println(e.getMessage());
			helpFormatter.printHelp("theta-cfa.jar", options);
			return;
		}

		final String filename = cmd.getOptionValue(optFile.getOpt());
		final FunctionSlicer slicer = Slicer.valueOf(cmd.getOptionValue(optSlice.getOpt())).createSlicer();
		final Domain domain = Domain.valueOf(cmd.getOptionValue(optDomain.getOpt()));
		final Refinement refinement = Refinement.valueOf(cmd.getOptionValue(optRefinement.getOpt()));
		final Search search = Search.valueOf(cmd.getOptionValue(optSearch.getOpt()));
		final PrecGranularity pg = PrecGranularity.valueOf(cmd.getOptionValue(optPrecGran.getOpt()));
		final Integer optimizeLvl = Integer.parseInt(cmd.getOptionValue(optOptimize.getOpt()));
		// final boolean individual = cmd.hasOption(optIndividual.getOpt());
		final boolean individual = true;
		final boolean optimize = optimizeLvl == 1;

		// Optional arguments
		final String verbosityVal = cmd.getOptionValue(optVerbosity.getOpt());
		final String refinementSlicerVal = cmd.getOptionValue(optRefinementSlicer.getOpt());

		final int verbosity = verbosityVal != null ? Integer.parseInt(verbosityVal) : 1;
		final int initialSliceN = individual ? Integer.parseInt(cmd.getOptionValue(optSliceNumber.getOpt())) : 0;

		Logger log;

		if (cmd.hasOption(optLogfile.getOpt())) {
			log = new FileLogger(verbosity, filename, true, false);
		} else {
			log = NullLogger.getInstance();
		}

		final FunctionSlicer refinementSlicer = (refinementSlicerVal == null)
				? Slicer.valueOf(refinementSlicerVal).createSlicer() : Slicer.BACKWARD.createSlicer();

		final GlobalContext context = Parser.parse(filename);
		final Optimizer opt = new Optimizer(context, slicer);

		opt.setLogger(log);
		opt.addTransformation(new FunctionInliner());
		if (optimize) {
			opt.addTransformation(new ConstantPropagator());
			opt.addTransformation(new DeadBranchEliminator());
		}

		final Stopwatch sw = Stopwatch.createUnstarted();

		sw.start();

		opt.transform();
		final List<Slice> slices = opt.createSlices();

		sw.stop();

		final long optTime = sw.elapsed(TimeUnit.MILLISECONDS);

		VerificationResult result = VerificationResult.SAFE;

		log.writeln(String.format("Checking '%s' with the following configuration:", filename), 0);
		log.writeln(String.format("Slicer: %s", slicer.getClass().getSimpleName()), 0, 1);
		log.writeln(String.format("RefinementSlicer: %s", refinementSlicer.getClass().getSimpleName()), 0, 1);
		log.writeln(String.format("Individual slices: %s", individual), 0, 1);

		final int sliceMax = individual ? initialSliceN + 1 : slices.size();

		for (int i = initialSliceN; i < sliceMax; i++) {
			final Slice slice = slices.get(i);
			slice.setRefinementSlicer(refinementSlicer);

			// Get initial sizes
			final CFA initCfa = FunctionToCFATransformer.createLBE(slice.getSlicedFunction());

			tw.cell(initCfa.getLocs().size());
			tw.cell(initCfa.getEdges().size());

			result = checkSlice(tw, domain, refinement, search, pg, log, result, i, slice, optTime);
			tw.newRow();
		}
	}

	private static VerificationResult checkSlice(final TableWriter tw, final Domain domain, final Refinement refinement,
			final Search search, final PrecGranularity pg, final Logger log, VerificationResult result, final int i,
			final Slice slice, final long optTime) throws AssertionError {
		boolean cont = true;

		long sliceVerifTime = 0;
		long sliceRefinementTime = optTime;
		long refinementCnt = 0;

		final Stopwatch sw = Stopwatch.createUnstarted();

		log.writeHeader("Slice #" + i, 1);
		while (cont) {
			final Function cfg = slice.getSlicedFunction();
			final CFA cfa = FunctionToCFATransformer.createLBE(cfg);

			final Configuration<?, ?, ?> configuration = new CfaConfigurationBuilder(domain, refinement).search(search)
					.precGranularity(pg).logger(log).build(cfa);

			final SafetyResult<?, ?> status = configuration.check();
			final CegarStatistics stats = (CegarStatistics) status.getStats().get();

			if (status.isUnsafe()) {
				// final Trace<?, ?> cex = status.asUnsafe().getTrace();
				// The slice may require further refinement
				cont = slice.canRefine();
				if (!cont) {
					log.writeln("No slice refinement is possible. Slice is UNSAFE", 7, 1);
					// If no refinements are possible, this slice (and thus the
					// whole program) is unsafe
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
				// verifTime += status.getStats().get().getElapsedMillis();
			}
		}
		return result;
	}

	private static enum Slicer {
		NONE {
			@Override
			public FunctionSlicer createSlicer() {
				return new IdentitySlicer();
			}
		},

		BACKWARD {
			@Override
			public FunctionSlicer createSlicer() {
				return new BackwardSlicer();
			}
		},

		VALUE {
			@Override
			public FunctionSlicer createSlicer() {
				return new ValueSlicer();
			}
		},

		THIN {
			@Override
			public FunctionSlicer createSlicer() {
				return new ThinSlicer();
			}
		};

		public abstract FunctionSlicer createSlicer();
	}

	private static String options(final Object[] objs) {
		final StringJoiner sj = new StringJoiner("|");
		for (final Object o : objs) {
			sj.add(o.toString());
		}
		return sj.toString();
	}

}