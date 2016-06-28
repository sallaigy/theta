package hu.bme.mit.inf.ttmc.core.expr.impl;

import hu.bme.mit.inf.ttmc.core.expr.EqExpr;
import hu.bme.mit.inf.ttmc.core.expr.Expr;
import hu.bme.mit.inf.ttmc.core.type.BoolType;
import hu.bme.mit.inf.ttmc.core.type.Type;
import hu.bme.mit.inf.ttmc.core.type.impl.Types;
import hu.bme.mit.inf.ttmc.core.utils.ExprVisitor;

final class EqExprImpl extends AbstractBinaryExpr<Type, Type, BoolType> implements EqExpr {

	private static final int HASH_SEED = 43;

	private static final String OPERATOR_LABEL = "Eq";

	EqExprImpl(final Expr<? extends Type> leftOp, final Expr<? extends Type> rightOp) {
		super(leftOp, rightOp);
	}

	@Override
	public BoolType getType() {
		return Types.Bool();
	}

	@Override
	public EqExpr withOps(final Expr<? extends Type> leftOp, final Expr<? extends Type> rightOp) {
		if (leftOp == getLeftOp() && rightOp == getRightOp()) {
			return this;
		} else {
			return Exprs.Eq(leftOp, rightOp);
		}
	}

	@Override
	public EqExpr withLeftOp(final Expr<? extends Type> leftOp) {
		return withOps(leftOp, getRightOp());
	}

	@Override
	public EqExpr withRightOp(final Expr<? extends Type> rightOp) {
		return withOps(getLeftOp(), rightOp);
	}

	@Override
	public <P, R> R accept(final ExprVisitor<? super P, ? extends R> visitor, final P param) {
		return visitor.visit(this, param);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof EqExpr) {
			final EqExpr that = (EqExpr) obj;
			return this.getLeftOp().equals(that.getLeftOp()) && this.getRightOp().equals(that.getRightOp());
		} else {
			return false;
		}
	}

	@Override
	protected int getHashSeed() {
		return HASH_SEED;
	}

	@Override
	protected String getOperatorLabel() {
		return OPERATOR_LABEL;
	}

}
