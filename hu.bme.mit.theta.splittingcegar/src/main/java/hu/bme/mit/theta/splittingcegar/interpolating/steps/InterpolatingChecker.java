package hu.bme.mit.theta.splittingcegar.interpolating.steps;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.core.expr.NotExpr;
import hu.bme.mit.theta.core.expr.impl.Exprs;
import hu.bme.mit.theta.core.utils.impl.PathUtils;
import hu.bme.mit.theta.formalism.sts.STS;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.splittingcegar.common.data.AbstractResult;
import hu.bme.mit.theta.splittingcegar.common.data.SolverWrapper;
import hu.bme.mit.theta.splittingcegar.common.data.StopHandler;
import hu.bme.mit.theta.splittingcegar.common.steps.AbstractCEGARStep;
import hu.bme.mit.theta.splittingcegar.common.steps.Checker;
import hu.bme.mit.theta.splittingcegar.common.utils.SolverHelper;
import hu.bme.mit.theta.splittingcegar.common.utils.visualization.Visualizer;
import hu.bme.mit.theta.splittingcegar.interpolating.data.InterpolatedAbstractState;
import hu.bme.mit.theta.splittingcegar.interpolating.data.InterpolatedAbstractSystem;
import hu.bme.mit.theta.splittingcegar.interpolating.utils.VisualizationHelper;

public class InterpolatingChecker extends AbstractCEGARStep
		implements Checker<InterpolatedAbstractSystem, InterpolatedAbstractState> {

	private final boolean isIncremental;
	private int actualInit;
	private final Set<InterpolatedAbstractState> exploredStates;
	// Stacks for backtracking
	private final Stack<InterpolatedAbstractState> stateStack;
	private final Stack<Integer> successorStack;

	public InterpolatingChecker(final SolverWrapper solvers, final StopHandler stopHandler, final Logger logger,
			final Visualizer visualizer, final boolean isIncremental) {
		super(solvers, stopHandler, logger, visualizer);
		this.isIncremental = isIncremental;
		exploredStates = new HashSet<>();
		stateStack = new Stack<>();
		successorStack = new Stack<Integer>();
		reset();
	}

	@Override
	public AbstractResult<InterpolatedAbstractState> check(final InterpolatedAbstractSystem system) {
		// Get the index of the previously splitted state, or -1 at first call
		final int splitIndex = system.getPreviousSplitIndex();

		final NotExpr negProp = Exprs.Not(system.getSTS().getProp());

		final STS sts = system.getSTS();
		Stack<InterpolatedAbstractState> counterExample = null; // Store the
																// counterexample
																// (if found)

		final int stateSpaceInitialSize = exploredStates.size();

		if (!isIncremental)
			reset(); // Clear if not incremental
		else {
			if (splitIndex == -1)
				reset(); // No split state before the first iteration
			if (splitIndex >= 0) {
				assert (actualInit > 0);
				--actualInit; // The actual initial state should be moved back
								// by one
								// since the previous search is continued in
								// this case
								// Remove states from the explored set (only
								// those that are later than the split one)
				assert (splitIndex < stateStack.size());
				final int removeAfter = stateStack.get(splitIndex).getExploredIndex();
				for (final Iterator<InterpolatedAbstractState> it = exploredStates.iterator(); it.hasNext();) {
					final InterpolatedAbstractState ias = it.next();
					if (ias.getExploredIndex() >= removeAfter)
						it.remove();
				}

				// Stacks also have to be cleared above (including) the split
				// state.
				while (stateStack.size() > splitIndex) {
					stateStack.pop();
					successorStack.pop();
				}
				// The current successor for the top state must be moved back,
				// since the split state was
				// removed from its successors (if any state still remains on
				// the stack)
				if (splitIndex > 0)
					successorStack.push(successorStack.pop() - 1);
				logger.writeln("Continuing search with a stack of " + stateStack.size(), 5);
			}
		}

		final int stateSpaceSizeAfterClear = exploredStates.size();

		final Solver solver = solvers.getSolver();

		solver.push();
		solver.add(PathUtils.unfold(negProp, 0)); // Assert the negate of the
		// specification

		// Flag for storing whether the actual search is a continuation from a
		// previous
		// model checking round. It is true for the first search from an initial
		// state,
		// but false after
		boolean isContinuation = true;

		// Loop through each initial state and do a search
		while (actualInit < system.getAbstractKripkeStructure().getInitialStates().size() && counterExample == null) {
			if (stopHandler.isStopped())
				return null;
			final InterpolatedAbstractState init = system.getAbstractKripkeStructure().getInitialState(actualInit);
			++actualInit;

			// The initial state has to be considered if:
			// - The search is not incremental OR
			// - It is the first model checking round OR
			// - It is not a continuation of a previous search OR
			// - It is a continuation, but the initial state was splitted
			if (!isIncremental || splitIndex == -1 || !isContinuation || splitIndex == 0) {
				// Create stacks for backtracking
				stateStack.clear();
				successorStack.clear();

				logger.writeln("Initial state: " + init, 5);
				if (!exploredStates.contains(init)) {
					init.setExploredIndex(exploredStates.size());
					exploredStates.add(init);
					// Push to stack
					stateStack.push(init);
					successorStack.push(-1);
					// Check if the specification holds
					if (checkState(init, solver, sts)) {
						logger.writeln("Counterexample reached!", 5, 1);
						counterExample = stateStack;
					}
				}
			}
			isContinuation = false;

			while (!stateStack.isEmpty() && counterExample == null) {
				if (stopHandler.isStopped())
					return null;
				// Get the actual state
				final InterpolatedAbstractState actual = stateStack.peek();
				// Get the next successor of the actual state (if exists)
				if (successorStack.peek() + 1 < actual.getSuccessors().size()) {
					final InterpolatedAbstractState next = actual.getSuccessors().get(successorStack.peek() + 1);
					successorStack.push(successorStack.pop() + 1);
					// If this state is not yet explored, process it
					if (!exploredStates.contains(next)) {
						logger.write("New state: ", 6, 1);
						logger.writeln(next, 6, 0);
						next.setExploredIndex(exploredStates.size());
						exploredStates.add(next);
						// Push to stack
						stateStack.push(next);
						successorStack.push(-1);
						// Check if the specification holds
						if (checkState(next, solver, sts)) {
							logger.writeln("Counterexample reached!", 5, 1);
							counterExample = stateStack;
						}
					}
				} else { // If the actual state has no more successors, then
								// backtrack
					stateStack.pop();
					successorStack.pop();
				}
			}
		}
		solver.pop();

		logger.writeln("Explored state space statistics:", 2);
		if (isIncremental) {
			logger.writeln("States from the previous iteration: " + stateSpaceInitialSize, 2, 1);
			logger.writeln("States after cleaning stack: " + stateSpaceSizeAfterClear, 2, 1);
		}
		logger.writeln("States: " + exploredStates.size(), 2, 1);

		if (counterExample != null) {
			logger.writeln("Counterexample found (length: " + counterExample.size() + ")", 2);
			// Mark states on the stack and print counterexample
			for (final InterpolatedAbstractState as : counterExample) {
				as.setPartOfCounterexample(true);
				logger.writeln(as, 4, 1); // Print each state in the
											// counterexample
			}
			VisualizationHelper.visualizeAbstractKripkeStructure(system, exploredStates, visualizer, 4);
		} else {
			logger.writeln("Specification holds.", 2);
			VisualizationHelper.visualizeAbstractKripkeStructure(system, exploredStates, visualizer, 6);
		}

		// TODO: optimization: clear data structures if not incremental. Note
		// that the returned counterexample is a reference to the state stack

		return counterExample == null
				? new AbstractResult<InterpolatedAbstractState>(null, exploredStates,
						exploredStates.size() - stateSpaceSizeAfterClear)
				: new AbstractResult<InterpolatedAbstractState>(counterExample, null,
						exploredStates.size() - stateSpaceSizeAfterClear);
	}

	private boolean checkState(final InterpolatedAbstractState s, final Solver solver, final STS sts) {
		solver.push();
		SolverHelper.unrollAndAssert(solver, s.getLabels(), sts, 0);
		final boolean ret = SolverHelper.checkSat(solver);
		solver.pop();
		return ret;
	}

	/**
	 * Reset data structures for completely new model checking iteration
	 */
	private void reset() {
		exploredStates.clear();
		stateStack.clear();
		successorStack.clear();
		actualInit = 0;
	}

	@Override
	public String toString() {
		return isIncremental ? "incremental" : "";
	}
}
