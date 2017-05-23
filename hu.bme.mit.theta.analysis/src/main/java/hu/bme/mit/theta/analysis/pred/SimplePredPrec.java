package hu.bme.mit.theta.analysis.pred;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import hu.bme.mit.theta.common.ObjectUtils;
import hu.bme.mit.theta.core.expr.BoolLitExpr;
import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.expr.impl.Exprs;
import hu.bme.mit.theta.core.model.impl.Valuation;
import hu.bme.mit.theta.core.type.BoolType;
import hu.bme.mit.theta.core.utils.impl.ExprUtils;
import hu.bme.mit.theta.core.utils.impl.PathUtils;
import hu.bme.mit.theta.solver.Solver;

/**
 * Represents an immutable, simple predicate precision that is a set of
 * predicates.
 */
public final class SimplePredPrec implements PredPrec {

	private final Map<Expr<? extends BoolType>, Expr<? extends BoolType>> predToNegMap;
	private final Solver solver;

	public static SimplePredPrec create(final Solver solver) {
		return new SimplePredPrec(Collections.emptySet(), solver);
	}

	public static SimplePredPrec create(final Iterable<Expr<? extends BoolType>> preds, final Solver solver) {
		return new SimplePredPrec(preds, solver);
	}

	public static SimplePredPrec create(final Expr<? extends BoolType> pred, final Solver solver) {
		return new SimplePredPrec(Collections.singleton(pred), solver);
	}

	private SimplePredPrec(final Iterable<Expr<? extends BoolType>> preds, final Solver solver) {
		checkNotNull(preds);
		this.solver = checkNotNull(solver);
		this.predToNegMap = new HashMap<>();

		for (final Expr<? extends BoolType> pred : preds) {
			if (pred instanceof BoolLitExpr) {
				continue;
			}
			final Expr<? extends BoolType> ponatedPred = ExprUtils.ponate(pred);
			if (!this.predToNegMap.containsKey(ponatedPred)) {
				this.predToNegMap.put(ponatedPred, Exprs.Not(ponatedPred));
			}
		}
	}

	public Solver getSolver() {
		return solver;
	}

	private Expr<? extends BoolType> negate(final Expr<? extends BoolType> pred) {
		final Expr<? extends BoolType> negated = predToNegMap.get(pred);
		checkArgument(negated != null, "Negated predicate not found");
		return negated;
	}

	@Override
	public PredState createState(final Valuation valuation) {
		checkNotNull(valuation);
		final Set<Expr<? extends BoolType>> statePreds = new HashSet<>();

		for (final Expr<? extends BoolType> pred : predToNegMap.keySet()) {
			final Expr<? extends BoolType> simplified = ExprUtils.simplify(pred, valuation);
			if (simplified.equals(Exprs.True())) {
				statePreds.add(pred);
			} else if (simplified.equals(Exprs.False())) {
				statePreds.add(negate(pred));
			} else {
				final Expr<? extends BoolType> simplified0 = PathUtils.unfold(simplified, 0);

				solver.push();
				solver.add(Exprs.Not(simplified0));
				final boolean ponValid = solver.check().isUnsat();
				solver.pop();

				solver.push();
				solver.add(simplified0);
				final boolean negValid = solver.check().isUnsat();
				solver.pop();

				assert !(ponValid && negValid) : "Ponated and negated predicates are both valid";
				if (ponValid) {
					statePreds.add(pred);
				} else if (negValid) {
					statePreds.add(negate(pred));
				}
			}
		}

		return PredState.of(statePreds);
	}

	public SimplePredPrec join(final SimplePredPrec other) {
		checkNotNull(other);
		final Collection<Expr<? extends BoolType>> joinedPreds = ImmutableSet.<Expr<? extends BoolType>>builder()
				.addAll(this.predToNegMap.keySet()).addAll(other.predToNegMap.keySet()).build();
		// If no new predicate was added, return same instance (immutable)
		if (joinedPreds.size() == this.predToNegMap.size()) {
			return this;
		} else if (joinedPreds.size() == other.predToNegMap.size()) {
			return other;
		}

		return create(joinedPreds, solver);
	}

	@Override
	public String toString() {
		return ObjectUtils.toStringBuilder(getClass().getSimpleName()).addAll(predToNegMap.keySet()).toString();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof SimplePredPrec) {
			final SimplePredPrec that = (SimplePredPrec) obj;
			return this.predToNegMap.keySet().equals(that.predToNegMap.keySet());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return 31 * predToNegMap.keySet().hashCode();
	}
}
