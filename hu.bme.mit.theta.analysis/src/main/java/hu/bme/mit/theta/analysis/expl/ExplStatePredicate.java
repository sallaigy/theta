package hu.bme.mit.theta.analysis.expl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Predicate;

import hu.bme.mit.theta.common.ObjectUtils;
import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.expr.impl.Exprs;
import hu.bme.mit.theta.core.type.BoolType;
import hu.bme.mit.theta.core.utils.impl.ExprUtils;
import hu.bme.mit.theta.core.utils.impl.PathUtils;
import hu.bme.mit.theta.solver.Solver;

public class ExplStatePredicate implements Predicate<ExplState> {

	private final Expr<? extends BoolType> expr;
	private final Solver solver;

	public ExplStatePredicate(final Expr<? extends BoolType> expr, final Solver solver) {
		this.expr = checkNotNull(expr);
		this.solver = checkNotNull(solver);
	}

	@Override
	public boolean test(final ExplState state) {
		final Expr<? extends BoolType> simplified = ExprUtils.simplify(expr, state);
		if (simplified.equals(Exprs.True())) {
			return true;
		}
		if (simplified.equals(Exprs.False())) {
			return false;
		}
		solver.push();
		solver.add(PathUtils.unfold(simplified, 0));
		final boolean result = solver.check().isSat();
		solver.pop();
		return result;
	}

	@Override
	public String toString() {
		return ObjectUtils.toStringBuilder(getClass().getSimpleName()).add(expr).toString();
	}
}
