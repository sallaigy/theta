package hu.bme.mit.theta.analysis.pred;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.ActionFunction;
import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.Domain;
import hu.bme.mit.theta.analysis.InitFunction;
import hu.bme.mit.theta.analysis.TransferFunction;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.type.BoolType;
import hu.bme.mit.theta.solver.Solver;

public final class PredAnalysis implements Analysis<PredState, ExprAction, PredPrecision> {

	private final Domain<PredState> domain;
	private final InitFunction<PredState, PredPrecision> initFunction;
	private final TransferFunction<PredState, ExprAction, PredPrecision> transferFunction;
	private final ActionFunction<? super PredState, ? extends ExprAction> actionFunction;

	private PredAnalysis(final Solver solver, final Expr<? extends BoolType> initExpr,
			final ActionFunction<? super PredState, ? extends ExprAction> actionFunction) {
		domain = PredDomain.create(solver);
		initFunction = PredInitFunction.create(solver, initExpr);
		transferFunction = PredTransferFunction.create(solver);
		this.actionFunction = checkNotNull(actionFunction);
	}

	public static PredAnalysis create(final Solver solver, final Expr<? extends BoolType> initExpr,
			final ActionFunction<? super PredState, ? extends ExprAction> actionFunction) {
		return new PredAnalysis(solver, initExpr, actionFunction);
	}

	////

	@Override
	public Domain<PredState> getDomain() {
		return domain;
	}

	@Override
	public InitFunction<PredState, PredPrecision> getInitFunction() {
		return initFunction;
	}

	@Override
	public TransferFunction<PredState, ExprAction, PredPrecision> getTransferFunction() {
		return transferFunction;
	}

	@Override
	public ActionFunction<? super PredState, ? extends ExprAction> getActionFunction() {
		return actionFunction;
	}

}