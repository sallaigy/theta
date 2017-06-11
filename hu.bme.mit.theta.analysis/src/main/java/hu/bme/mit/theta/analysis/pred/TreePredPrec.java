package hu.bme.mit.theta.analysis.pred;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.expr.LitExpr;
import hu.bme.mit.theta.core.expr.NotExpr;
import hu.bme.mit.theta.core.expr.impl.Exprs;
import hu.bme.mit.theta.core.model.impl.Valuation;
import hu.bme.mit.theta.core.type.BoolType;
import hu.bme.mit.theta.core.utils.impl.ExprUtils;

/**
 * Represents a mutable tree-shaped precision for predicates, capable of storing
 * a different precision for every individual abstract state. Each node of the
 * tree contains a predicate. Each node may have child nodes that refine the
 * ponated or the negated version of the predicate in the node. A valuation of
 * variables determines a path from the root to a leaf, thus determining an
 * abstract state. Refinement is performed by adding new child nodes to leaf
 * nodes.
 */
public final class TreePredPrec implements PredPrec {

	private final Node root;

	/**
	 * Create a new instance with no predicates
	 */
	public static TreePredPrec create() {
		return create(Collections.emptySet());
	}

	/**
	 * Create a new instance with a list of predicates
	 */
	public static TreePredPrec create(final Iterable<? extends Expr<? extends BoolType>> preds) {
		return new TreePredPrec(preds);
	}

	private TreePredPrec(final Iterable<? extends Expr<? extends BoolType>> preds) {
		checkNotNull(preds);

		final Set<Expr<? extends BoolType>> ponatedPreds = new HashSet<>();
		for (final Expr<? extends BoolType> pred : preds) {
			ponatedPreds.add(ExprUtils.ponate(pred));
		}

		if (ponatedPreds.isEmpty()) {
			ponatedPreds.add(Exprs.True());
		}

		root = new Node(new ArrayList<>(ponatedPreds));
	}

	private final static class Node {
		private final Expr<? extends BoolType> ponPred;
		private final Expr<? extends BoolType> negPred;

		private Optional<Node> ponRefined;
		private Optional<Node> negRefined;

		public Node(final Expr<? extends BoolType> pred) {
			this(Collections.singletonList(pred));
		}

		public Node(final List<? extends Expr<? extends BoolType>> preds) {
			assert !preds.isEmpty();
			assert !(preds.get(0) instanceof NotExpr);
			this.ponPred = preds.get(0);
			this.negPred = Exprs.Not(this.ponPred);
			if (preds.size() == 1) {
				this.ponRefined = Optional.empty();
				this.negRefined = Optional.empty();
			} else {
				this.ponRefined = Optional.of(new Node(preds.subList(1, preds.size())));
				this.negRefined = Optional.of(new Node(preds.subList(1, preds.size())));
			}
		}

		public Expr<? extends BoolType> getPonPred() {
			return ponPred;
		}

		public Expr<? extends BoolType> getNegPred() {
			return negPred;
		}

		public void refinePon(final Expr<? extends BoolType> pred) {
			assert !ponRefined.isPresent();
			ponRefined = Optional.of(new Node(pred));
		}

		public void refineNeg(final Expr<? extends BoolType> pred) {
			assert !negRefined.isPresent();
			negRefined = Optional.of(new Node(pred));
		}

		public Optional<Node> getPonRefined() {
			return ponRefined;
		}

		public Optional<Node> getNegRefined() {
			return negRefined;
		}
	}

	@Override
	public PredState createState(final Valuation valuation) {
		checkNotNull(valuation);
		final Set<Expr<? extends BoolType>> statePreds = new HashSet<>();

		Node node = root;

		while (node != null) {
			final LitExpr<? extends BoolType> predHolds = ExprUtils.evaluate(node.getPonPred(), valuation);
			if (predHolds.equals(Exprs.True())) {
				statePreds.add(node.getPonPred());
				node = node.getPonRefined().isPresent() ? node.getPonRefined().get() : null;
			} else if (predHolds.equals(Exprs.False())) {
				statePreds.add(node.getNegPred());
				node = node.getNegRefined().isPresent() ? node.getNegRefined().get() : null;
			} else {
				throw new UnsupportedOperationException("Predicate cannot be evaluated in TreePredPrec");
			}
		}

		return PredState.of(statePreds);
	}

	/**
	 * Refine a state with a new predicate. The state must exist in the tree.
	 */
	public void refine(final PredState state, final Expr<? extends BoolType> pred) {
		checkNotNull(state);
		checkNotNull(pred);

		final Expr<? extends BoolType> refiningPred = ExprUtils.ponate(pred);

		Node node = root;
		while (node != null) {
			if (state.getPreds().contains(node.getPonPred())) {
				if (node.getPonRefined().isPresent()) {
					node = node.getPonRefined().get();
				} else {
					node.refinePon(refiningPred);
					node = null;
				}
			} else if (state.getPreds().contains(node.getNegPred())) {
				if (node.getNegRefined().isPresent()) {
					node = node.getNegRefined().get();
				} else {
					node.refineNeg(refiningPred);
					node = null;
				}
			} else {
				throw new IllegalStateException("State does not contain predicate or its negation of a Node");
			}
		}
	}
}
