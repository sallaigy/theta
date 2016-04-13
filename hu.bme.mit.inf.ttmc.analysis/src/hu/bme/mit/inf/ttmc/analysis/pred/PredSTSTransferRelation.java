package hu.bme.mit.inf.ttmc.analysis.pred;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import hu.bme.mit.inf.ttmc.analysis.TransferRelation;
import hu.bme.mit.inf.ttmc.core.expr.AndExpr;
import hu.bme.mit.inf.ttmc.core.expr.Expr;
import hu.bme.mit.inf.ttmc.core.expr.impl.Exprs;
import hu.bme.mit.inf.ttmc.core.type.BoolType;
import hu.bme.mit.inf.ttmc.formalism.sts.STS;
import hu.bme.mit.inf.ttmc.solver.Solver;

public class PredSTSTransferRelation implements TransferRelation<PredState> {

	private final Solver solver;
	private final STS sts;

	public PredSTSTransferRelation(final STS sts, final Solver solver) {
		this.sts = checkNotNull(sts);
		this.solver = checkNotNull(solver);
	}

	@Override
	public Collection<? extends PredState> getSuccStates(final PredState state) {
		final Set<PredState> successors = new HashSet<>();
		while (true) {
			AndExpr successor = null;
			solver.push();
			for (final Expr<? extends BoolType> pred : state.getPreds())
				solver.add(sts.unroll(pred, 0));
			solver.add(sts.unrollInv(0));
			solver.add(sts.unrollInv(1));
			solver.add(sts.unrollTrans(0));
			if (solver.check().boolValue())
				successor = sts.getConcreteState(solver.getModel(), 0);
			solver.pop();
			if (successor != null) {
				final Set<Expr<? extends BoolType>> successorPreds = new HashSet<>();
				solver.push();
				solver.add(sts.unroll(successor, 0));
				for (final Expr<? extends BoolType> pred : state.getPreds()) {
					solver.push();
					solver.add(sts.unroll(pred, 0));
					if (solver.check().boolValue()) {
						successorPreds.add(pred);
					} else {
						successorPreds.add(Exprs.Not(pred));
					}
					solver.pop();
				}
				solver.pop();
				successors.add(PredState.create(successorPreds, solver));
			} else {
				break;
			}

		}
		return successors;
	}

}
