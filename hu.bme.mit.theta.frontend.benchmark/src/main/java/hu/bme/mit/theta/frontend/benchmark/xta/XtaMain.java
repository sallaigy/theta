package hu.bme.mit.theta.frontend.benchmark.xta;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.StringJoiner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.analysis.xta.algorithm.lazy.BinItpStrategy;
import hu.bme.mit.theta.analysis.xta.algorithm.lazy.ItpStrategy.ItpOperator;
import hu.bme.mit.theta.analysis.xta.algorithm.lazy.LazyXtaChecker;
import hu.bme.mit.theta.analysis.xta.algorithm.lazy.LazyXtaChecker.AlgorithmStrategy;
import hu.bme.mit.theta.analysis.xta.algorithm.lazy.LazyXtaStatistics;
import hu.bme.mit.theta.analysis.xta.algorithm.lazy.LuStrategy;
import hu.bme.mit.theta.analysis.xta.algorithm.lazy.SeqItpStrategy;
import hu.bme.mit.theta.common.table.TableWriter;
import hu.bme.mit.theta.common.table.impl.SimpleTableWriter;
import hu.bme.mit.theta.formalism.xta.XtaSystem;
import hu.bme.mit.theta.formalism.xta.dsl.XtaDslManager;

public final class XtaMain {

	private static enum Algorithm {

		SEQITP {
			@Override
			public AlgorithmStrategy<?> create(final XtaSystem system) {
				return SeqItpStrategy.create(system, ItpOperator.DEFAULT);
			}
		},

		BINITP {
			@Override
			public AlgorithmStrategy<?> create(final XtaSystem system) {
				return BinItpStrategy.create(system, ItpOperator.DEFAULT);
			}
		},

		WEAKSEQITP {
			@Override
			public AlgorithmStrategy<?> create(final XtaSystem system) {
				return SeqItpStrategy.create(system, ItpOperator.WEAK);
			}
		},

		WEAKBINITP {
			@Override
			public AlgorithmStrategy<?> create(final XtaSystem system) {
				return BinItpStrategy.create(system, ItpOperator.WEAK);
			}
		},

		LU {
			@Override
			public AlgorithmStrategy<?> create(final XtaSystem system) {
				return LuStrategy.create(system);
			}
		};

		public abstract LazyXtaChecker.AlgorithmStrategy<?> create(final XtaSystem system);
	}

	private static enum Search {

		DFS {
			@Override
			public SearchStrategy create() {
				return SearchStrategy.depthFirst();
			}
		},

		BFS {
			@Override
			public SearchStrategy create() {
				return SearchStrategy.breadthFirst();
			}
		},

		RANDOM {
			@Override
			public SearchStrategy create() {
				return SearchStrategy.random();
			}
		};

		public abstract SearchStrategy create();
	}

	private static final Option ALGORITHM = Option.builder("a").longOpt("algorithm").numberOfArgs(1)
			.argName(optionsFor(Algorithm.values())).type(Algorithm.class).desc("Algorithm").required().build();

	private static final Option MODEL = Option.builder("m").longOpt("model").numberOfArgs(1).argName("MODEL")
			.type(String.class).desc("Path of the input model").required().build();

	private static final Option SEARCH = Option.builder("s").longOpt("search").numberOfArgs(1)
			.argName(optionsFor(Search.values())).type(SearchStrategy.class).desc("Search strategy").required().build();

	public static void main(final String[] args) {
		final TableWriter writer = new SimpleTableWriter(System.out, ",", "\"", "\"");

		// Setting up argument parser
		final Options options = new Options();

		options.addOption(MODEL);
		options.addOption(ALGORITHM);
		options.addOption(SEARCH);

		final CommandLineParser parser = new DefaultParser();
		final HelpFormatter helpFormatter = new HelpFormatter();
		final CommandLine cmd;

		// Parse arguments
		try {
			cmd = parser.parse(options, args);
		} catch (final ParseException e) {
			// If called with a single --header argument, print header and exit
			if (args.length == 1 && "--header".equals(args[0])) {
				writer.cell("AlgorithmTimeInMs");
				writer.cell("RefinementTimeInMs");
				writer.cell("InterpolationTimeInMs");
				writer.cell("RefinementSteps");
				writer.cell("ArgDepth");
				writer.cell("ArgNodes");
				writer.cell("ArgNodesFeasible");
				writer.cell("ArgNodesExpanded");
				writer.cell("DiscreteStatesExpanded");
				writer.newRow();
				return;
			} else {
				helpFormatter.printHelp("theta-xta.jar", options, true);
				return;
			}
		}

		final String modelOption = cmd.getOptionValue(MODEL.getOpt());
		final XtaSystem system;
		try {
			final InputStream inputStream = new FileInputStream(modelOption);
			system = XtaDslManager.createSystem(inputStream);
		} catch (final Exception e) {
			System.out.println("Path \"" + modelOption + "\" is invalid");
			return;
		}

		final String algorithmOption = cmd.getOptionValue(ALGORITHM.getOpt());
		final Algorithm algorithm;
		try {
			algorithm = Enum.valueOf(Algorithm.class, algorithmOption);
		} catch (final Exception e) {
			System.out.println("\"" + algorithmOption + "\" is not a valid value for --algorithm");
			return;
		}

		final String searchOption = cmd.getOptionValue(SEARCH.getOpt());
		final Search search;
		try {
			search = Enum.valueOf(Search.class, searchOption);
		} catch (final Exception e) {
			System.out.println("\"" + searchOption + "\" is not a valid value for --strategy");
			return;
		}

		try {
			final LazyXtaChecker.AlgorithmStrategy<?> algorithmStrategy = algorithm.create(system);
			final SearchStrategy searchStrategy = search.create();

			final SafetyChecker<?, ?, UnitPrec> checker =

					LazyXtaChecker.create(system, algorithmStrategy, searchStrategy, l -> false);

			final SafetyResult<?, ?> result = checker.check(UnitPrec.getInstance());
			final LazyXtaStatistics stats = (LazyXtaStatistics) result.getStats().get();

			writer.cell(stats.getAlgorithmTimeInMs());
			writer.cell(stats.getRefinementTimeInMs());
			writer.cell(stats.getInterpolationTimeInMs());
			writer.cell(stats.getRefinementSteps());
			writer.cell(stats.getArgDepth());
			writer.cell(stats.getArgNodes());
			writer.cell(stats.getArgNodesFeasible());
			writer.cell(stats.getArgNodesExpanded());
			writer.cell(stats.getDiscreteStatesExpanded());

		} catch (final Exception e) {
			final String message = e.getMessage() == null ? "" : ": " + e.getMessage();
			writer.cell("[EX] " + e.getClass().getSimpleName() + message);
		}

		writer.newRow();
	}

	private static String optionsFor(final Object[] objs) {
		final StringJoiner sj = new StringJoiner("|");
		for (final Object o : objs) {
			sj.add(o.toString());
		}
		return sj.toString();
	}

}
