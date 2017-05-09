package hu.bme.mit.theta.analysis.tcfa.zone;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.formalism.ta.constr.impl.ClockConstrs.Eq;

import com.google.common.collect.Lists;

import hu.bme.mit.theta.analysis.tcfa.TcfaAction;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.formalism.ta.constr.ClockConstr;
import hu.bme.mit.theta.formalism.ta.decl.ClockDecl;
import hu.bme.mit.theta.formalism.ta.op.ClockOp;
import hu.bme.mit.theta.formalism.ta.op.GuardOp;
import hu.bme.mit.theta.formalism.ta.op.ResetOp;
import hu.bme.mit.theta.formalism.ta.utils.impl.TaExpr;
import hu.bme.mit.theta.formalism.ta.utils.impl.TaStmt;
import hu.bme.mit.theta.formalism.ta.utils.impl.TaExpr.ClockExpr;

public final class TcfaZoneUtils {

	private TcfaZoneUtils() {
	}

	public static ZoneState post(final ZoneState state, final TcfaAction action, final ZonePrec prec) {
		checkNotNull(state);
		checkNotNull(action);
		checkNotNull(prec);

		final ZoneState.Builder succStateBuilder = state.project(prec.getClocks());

		for (final TaExpr invar : action.getSourceInvars()) {
			if (invar.isClockExpr()) {
				final ClockExpr clockExpr = invar.asClockExpr();
				final ClockConstr constr = clockExpr.getClockConstr();
				succStateBuilder.and(constr);
			}
		}

		for (final TaStmt tcfaStmt : action.getTcfaStmts()) {
			if (tcfaStmt.isClockStmt()) {
				final ClockOp op = tcfaStmt.asClockStmt().getClockOp();
				succStateBuilder.execute(op);
			}
		}

		for (final TaExpr invar : action.getTargetInvars()) {
			if (invar.isClockExpr()) {
				final ClockExpr clockExpr = invar.asClockExpr();
				final ClockConstr constr = clockExpr.getClockConstr();
				succStateBuilder.and(constr);
			}
		}

		if (!action.getEdge().getSource().isUrgent()) {
			succStateBuilder.up();
		}

		final ZoneState succState = succStateBuilder.build();
		return succState;
	}

	public static ZoneState pre(final ZoneState state, final TcfaAction action, final ZonePrec prec) {
		checkNotNull(state);
		checkNotNull(action);
		checkNotNull(prec);

		final ZoneState.Builder prevStateBuilder = state.project(prec.getClocks());

		if (!action.getEdge().getSource().isUrgent()) {
			prevStateBuilder.down();
		}

		for (final TaExpr invar : action.getTargetInvars()) {
			if (invar.isClockExpr()) {
				final ClockExpr clockExpr = invar.asClockExpr();
				final ClockConstr constr = clockExpr.getClockConstr();
				prevStateBuilder.and(constr);
			}
		}

		for (final TaStmt tcfaStmt : Lists.reverse(action.getTcfaStmts())) {
			if (tcfaStmt.isClockStmt()) {
				final ClockOp op = tcfaStmt.asClockStmt().getClockOp();
				if (op instanceof ResetOp) {
					final ResetOp resetOp = (ResetOp) op;
					final ClockDecl clock = resetOp.getClock();
					final int value = resetOp.getValue();
					prevStateBuilder.and(Eq(clock, value));
					prevStateBuilder.free(clock);

				} else if (op instanceof GuardOp) {
					final GuardOp guardOp = (GuardOp) op;
					prevStateBuilder.and(guardOp.getConstr());

				} else {
					throw new AssertionError();
				}
			}
		}

		for (final TaExpr invar : action.getSourceInvars()) {
			if (invar.isClockExpr()) {
				final ClockExpr clockExpr = invar.asClockExpr();
				final ClockConstr constr = clockExpr.getClockConstr();
				prevStateBuilder.and(constr);
			}
		}

		final ZoneState prevState = prevStateBuilder.build();
		return prevState;
	}

}
