package hu.bme.mit.theta.frontend.benchmark.cfa;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.utils.impl.StmtUtils;
import hu.bme.mit.theta.formalism.cfa.CFA;
import hu.bme.mit.theta.formalism.cfa.CfaEdge;
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
		final TableWriter tableWriter = new SimpleTableWriter(System.out, ",", "\"", "\"");
		// If only called with a single --header argument, print header and exit
		if (args.length == 1 && "--header".equals(args[0])) {
			tableWriter.cell("Safe");
			tableWriter.cell("TimeMs");
			tableWriter.cell("Iterations");
			tableWriter.cell("ArgSize");
			tableWriter.cell("ArgDepth");
			tableWriter.cell("ArgMeanBranchFactor");
			tableWriter.cell("CexLen");
			tableWriter.cell("Vars");
			tableWriter.cell("Size");
			tableWriter.newRow();
			return;
		}

		final Options options = new Options();

		final Option optFile = new Option("f", "file", true, "Path of the input file");
		optFile.setRequired(true);
		optFile.setArgName("FILE");
		options.addOption(optFile);

		final Option optSliceNo = new Option("sn", "sliceno", true, "Number of the slice to be verified");
		optFile.setArgName("INT");
		options.addOption(optSliceNo);

		/* Options for the optimizer */

		final Option optSlice = new Option("l", "slicer", true, "Slicing strategy (default : BACKWARD)");
		optSlice.setRequired(false);
		optSlice.setArgName("BACKWARD|VALUE|THIN");
		options.addOption(optSlice);

		final Option optRefinementSlicer = new Option("e", "refinement-slicer", true,
				"Refinement slicer (default : same as slicer)");
		optRefinementSlicer.setRequired(false);
		optRefinementSlicer.setArgName("BACKWARD|VALUE|THIN");
		options.addOption(optRefinementSlicer);

		final Option optOptimize = new Option("o", "optimization", true, "Optimize CFA (default : true)");
		optOptimize.setRequired(false);
		optOptimize.setArgName("true|false");
		options.addOption(optOptimize);

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
		final Option optLogfile = new Option("lf", "log-file", true, "Logger file (default : none)");
		optLogfile.setRequired(false);
		optLogfile.setArgName("LOGFILE");
		options.addOption(optLogfile);

		final Option optVerbosity = new Option("v", "verbosity", true, "Logging verbosity level (default : 1)");
		optVerbosity.setRequired(false);
		optVerbosity.setArgName("VERBOSITY");
		options.addOption(optVerbosity);

		final Option optBenchmark = Option.builder("bm").longOpt("benchmark")
				.desc("Benchmark mode (only print output values)").build();
		options.addOption(optBenchmark);

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
		final Domain domain = Domain.valueOf(cmd.getOptionValue(optDomain.getOpt()));
		final Refinement refinement = Refinement.valueOf(cmd.getOptionValue(optRefinement.getOpt()));
		final Search search = Search.valueOf(cmd.getOptionValue(optSearch.getOpt()));
		final PrecGranularity pg = PrecGranularity.valueOf(cmd.getOptionValue(optPrecGran.getOpt()));

		final FunctionSlicer slicer = Slicer.valueOf(cmd.getOptionValue(optSlice.getOpt(), "BACKWARD")).createSlicer();
		final FunctionSlicer refinementSlicer = Slicer.valueOf(
				cmd.getOptionValue(optRefinementSlicer.getOpt(), cmd.getOptionValue(optSlice.getOpt(), "BACKWARD")))
				.createSlicer();
		final boolean optimize = Boolean.parseBoolean(cmd.getOptionValue(optOptimize.getOpt(), "true"));

		final int verbosity = Integer.parseInt(cmd.getOptionValue(optVerbosity.getOpt(), "1"));

		final int sliceNo = Integer.parseInt(cmd.getOptionValue(optSliceNo.getOpt(), "-1"));

		final boolean benchmarkMode = cmd.hasOption(optBenchmark.getOpt());

		Logger logger;

		if (benchmarkMode) {
			logger = NullLogger.getInstance();
		} else if (cmd.hasOption(optLogfile.getOpt())) {
			logger = new FileLogger(verbosity, cmd.getOptionValue(optLogfile.getOpt()), true, false);
		} else {
			logger = NullLogger.getInstance();
		}

		final GlobalContext context = Parser.parse(filename);
		final Optimizer opt = new Optimizer(context, slicer);

		opt.setLogger(logger);
		opt.addTransformation(new FunctionInliner());
		if (optimize) {
			opt.addTransformation(new ConstantPropagator());
			opt.addTransformation(new DeadBranchEliminator());
		}

		final Stopwatch sw = Stopwatch.createStarted();
		opt.transform();
		final List<Slice> slices = opt.createSlices();
		sw.stop();
		final long optTime = sw.elapsed(TimeUnit.MILLISECONDS);

		logger.writeln(String.format("Checking '%s' with the following configuration:", filename), 0);
		logger.writeln(String.format("Slicer: %s", slicer.getClass().getSimpleName()), 0, 1);
		logger.writeln(String.format("RefinementSlicer: %s", refinementSlicer.getClass().getSimpleName()), 0, 1);

		if (sliceNo >= 0) {
			checkSlice(domain, refinement, search, pg, logger, sliceNo, slices, refinementSlicer, optTime,
					benchmarkMode, tableWriter);
		} else {
			for (int i = 0; i < slices.size(); i++) {
				checkSlice(domain, refinement, search, pg, logger, i, slices, refinementSlicer, optTime, benchmarkMode,
						tableWriter);
			}
		}
	}

	private static void checkSlice(final Domain domain, final Refinement refinement, final Search search,
			final PrecGranularity pg, final Logger log, final int sliceNo, final List<Slice> slices,
			final FunctionSlicer refinementSlicer, final long optTime, final boolean benchmarkMode,
			final TableWriter tableWriter) throws AssertionError {

		log.writeHeader("Slice #" + sliceNo, 1);

		try {
			final Slice slice = slices.get(sliceNo);
			slice.setRefinementSlicer(refinementSlicer);
			long sliceVerifTime = 0;
			int sliceCegarIterations = 0;
			@SuppressWarnings("unused")
			long sliceRefinementTime = optTime;
			@SuppressWarnings("unused")
			long refinementCnt = 0;

			final Stopwatch sw = Stopwatch.createUnstarted();

			SafetyResult<?, ?> status;
			CFA cfa;
			boolean cont = true;
			do {
				final Function cfg = slice.getSlicedFunction();
				cfa = FunctionToCFATransformer.createLBE(cfg);

				final Configuration<?, ?, ?> configuration = new CfaConfigurationBuilder(domain, refinement)
						.search(search).precGranularity(pg).logger(log).build(cfa);

				status = configuration.check();
				final CegarStatistics stats = (CegarStatistics) status.getStats().get();
				sliceVerifTime += stats.getElapsedMillis();
				sliceCegarIterations += stats.getIterations();

				if (status.isUnsafe()) {
					// final Trace<?, ?> cex = status.asUnsafe().getTrace();
					// The slice may require further refinement
					cont = slice.canRefine();
					if (!cont) {
						log.writeln("No slice refinement is possible. Slice is UNSAFE", 7, 1);
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

			} while (cont);
			if (benchmarkMode) {
				tableWriter.cell(status.isSafe());
				tableWriter.cell(sliceVerifTime);
				tableWriter.cell(sliceCegarIterations);
				tableWriter.cell(status.getArg().size());
				tableWriter.cell(status.getArg().getDepth());
				tableWriter.cell(status.getArg().getMeanBranchingFactor());
				if (status.isUnsafe()) {
					tableWriter.cell(status.asUnsafe().getTrace().length());
				} else {
					tableWriter.cell("");
				}
				tableWriter.cell(getCfaVars(cfa).size());
				tableWriter.cell(cfa.getLocs().size());
			} else {
				System.out.println("----------");
				System.out.println("Slice " + sliceNo);
				System.out.println("----------");
				System.out.println("Safe = " + status.isSafe());
				System.out.println("TimeElapsedInMs = " + sliceVerifTime);
				System.out.println("Iterations = " + sliceCegarIterations);
				System.out.println("ArgSize = " + status.getArg().size());
				System.out.println("ArgDepth = " + status.getArg().getDepth());
				System.out.println();
			}
		} catch (final Throwable ex) {
			final String message = ex.getMessage() == null ? "" : ": " + ex.getMessage();
			if (benchmarkMode) {
				tableWriter.cell("[EX] " + ex.getClass().getSimpleName() + message);
			} else {
				log.writeln("Exception occured: " + ex.getClass().getSimpleName(), 0);
				log.writeln("Message: " + ex.getMessage(), 0, 1);
			}

		}
		if (benchmarkMode) {
			tableWriter.newRow();
		}

	}

	private static Set<VarDecl<? extends Type>> getCfaVars(final CFA cfa) {
		final Set<VarDecl<? extends Type>> vars = new HashSet<>();
		for (final CfaEdge edge : cfa.getEdges()) {
			vars.addAll(StmtUtils.getVars(edge.getStmts()));
		}
		return vars;
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