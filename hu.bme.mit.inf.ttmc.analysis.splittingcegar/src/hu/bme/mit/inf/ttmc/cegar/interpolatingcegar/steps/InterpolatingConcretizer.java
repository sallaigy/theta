package hu.bme.mit.inf.ttmc.cegar.interpolatingcegar.steps;

import java.util.List;

import hu.bme.mit.inf.ttmc.cegar.common.data.ConcreteTrace;
import hu.bme.mit.inf.ttmc.cegar.common.steps.ConcretizerBase;
import hu.bme.mit.inf.ttmc.cegar.common.steps.IConcretizer;
import hu.bme.mit.inf.ttmc.cegar.common.utils.visualization.IVisualizer;
import hu.bme.mit.inf.ttmc.cegar.interpolatingcegar.data.InterpolatedAbstractState;
import hu.bme.mit.inf.ttmc.cegar.interpolatingcegar.data.InterpolatedAbstractSystem;
import hu.bme.mit.inf.ttmc.common.logging.Logger;
import hu.bme.mit.inf.ttmc.constraint.expr.NotExpr;

/**
 * Tries to find a concrete counterexample for an abstract counterexample. If no
 * concrete counterexample exists, then it finds the longest prefix of the
 * abstract counterexample for which a concrete trace exists. Since predicates
 * can be arbitrary, it may occur that a trace exists for the whole abstract
 * counterexample, but the last state of the trace is not a bad state (i.e., the
 * formula holds). In such cases the last state is the failure state.
 *
 * @author Akos
 */
public class InterpolatingConcretizer extends ConcretizerBase implements IConcretizer<InterpolatedAbstractSystem, InterpolatedAbstractState> {

	/**
	 * Initialize the step with a solver, logger and visualizer
	 *
	 * @param solver
	 * @param logger
	 * @param visualizer
	 */
	public InterpolatingConcretizer(final Logger logger, final IVisualizer visualizer) {
		super(logger, visualizer);
	}

	@Override
	public ConcreteTrace concretize(final InterpolatedAbstractSystem system, final List<InterpolatedAbstractState> abstractCounterEx) {
		final NotExpr negSpec = system.getManager().getExprFactory().Not(system.getSystem().getProp());
		return super.concretize(system, abstractCounterEx, negSpec, system.getVariables());
	}

	@Override
	public String toString() {
		return "";
	}
}
