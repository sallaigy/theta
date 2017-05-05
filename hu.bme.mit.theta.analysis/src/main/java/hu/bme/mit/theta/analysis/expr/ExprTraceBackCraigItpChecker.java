package hu.bme.mit.theta.analysis.expr;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.model.Model;
import hu.bme.mit.theta.core.model.impl.Valuation;
import hu.bme.mit.theta.core.type.BoolType;
import hu.bme.mit.theta.core.utils.impl.PathUtils;
import hu.bme.mit.theta.core.utils.impl.VarIndexing;
import hu.bme.mit.theta.solver.Interpolant;
import hu.bme.mit.theta.solver.ItpMarker;
import hu.bme.mit.theta.solver.ItpPattern;
import hu.bme.mit.theta.solver.ItpSolver;

public class ExprTraceBackCraigItpChecker implements ExprTraceChecker<ItpRefutation> {

	private final ItpSolver solver;
	private final Expr<? extends BoolType> init;
	private final Expr<? extends BoolType> target;

	private ExprTraceBackCraigItpChecker(final Expr<? extends BoolType> init, final Expr<? extends BoolType> target,
			final ItpSolver solver) {
		this.solver = checkNotNull(solver);
		this.init = checkNotNull(init);
		this.target = checkNotNull(target);
	}

	public static ExprTraceBackCraigItpChecker create(final Expr<? extends BoolType> init,
			final Expr<? extends BoolType> target, final ItpSolver solver) {
		return new ExprTraceBackCraigItpChecker(init, target, solver);
	}

	@Override
	public ExprTraceStatus<ItpRefutation> check(final Trace<? extends ExprState, ? extends ExprAction> trace) {
		checkNotNull(trace);
		final Trace<? extends ExprState, ? extends ExprAction> traceRev = trace.reverse();
		final int stateCount = trace.getStates().size();
		checkArgument(stateCount > 0, "Zero length trace");

		final List<VarIndexing> indexings = new ArrayList<>(stateCount);
		indexings.add(VarIndexing.all(10 * stateCount));

		solver.push();

		final ItpMarker A = solver.createMarker();
		final ItpMarker B = solver.createMarker();
		final ItpPattern pattern = solver.createBinPattern(A, B);

		int nPush = 1;
		solver.add(A, PathUtils.unfold(target, indexings.get(0)));
		solver.add(A, PathUtils.unfold(traceRev.getState(0).toExpr(), indexings.get(0)));
		checkState(solver.check().isSat(), "Initial state of the trace is not feasible");
		int satPostfix = 0;

		for (int i = 1; i < stateCount; ++i) {
			solver.push();
			++nPush;
			indexings.add(indexings.get(i - 1).sub(traceRev.getAction(i - 1).nextIndexing()));
			solver.add(A, PathUtils.unfold(traceRev.getState(i).toExpr(), indexings.get(i)));
			solver.add(A, PathUtils.unfold(traceRev.getAction(i - 1).toExpr(), indexings.get(i)));

			if (solver.check().isSat()) {
				satPostfix = i;
			} else {
				solver.pop();
				--nPush;
				break;
			}
		}

		final boolean concretizable;

		if (satPostfix == stateCount - 1) {
			solver.add(B, PathUtils.unfold(init, indexings.get(stateCount - 1)));
			concretizable = solver.check().isSat();
		} else {
			solver.add(B, PathUtils.unfold(traceRev.getState(satPostfix + 1).toExpr(), indexings.get(satPostfix + 1)));
			solver.add(B, PathUtils.unfold(traceRev.getAction(satPostfix).toExpr(), indexings.get(satPostfix + 1)));
			checkState(!solver.check().isSat(), "Trying to interpolate a feasible trace");
			concretizable = false;
		}

		ExprTraceStatus<ItpRefutation> status = null;
		if (concretizable) {
			final Model model = solver.getModel();
			final ImmutableList.Builder<Valuation> builder = ImmutableList.builder();
			for (final VarIndexing indexing : indexings) {
				builder.add(PathUtils.extractValuation(model, indexing));
			}
			status = ExprTraceStatus.feasible(Trace.of(builder.build().reverse(), trace.getActions()));
		} else {
			final Interpolant interpolant = solver.getInterpolant(pattern);
			final Expr<BoolType> itpFolded = PathUtils.foldin(interpolant.eval(A), indexings.get(satPostfix));
			status = ExprTraceStatus
					.infeasible(ItpRefutation.craig(itpFolded, stateCount - 1 - satPostfix, stateCount));
		}

		solver.pop(nPush);

		return status;
	}
}
