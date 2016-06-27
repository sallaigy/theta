package hu.bme.mit.inf.ttmc.formalism.common.expr.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.inf.ttmc.formalism.common.type.impl.Types2.Pointer;

import hu.bme.mit.inf.ttmc.core.type.Type;
import hu.bme.mit.inf.ttmc.core.utils.ExprVisitor;
import hu.bme.mit.inf.ttmc.formalism.common.expr.NewExpr;
import hu.bme.mit.inf.ttmc.formalism.common.type.PointerType;

final class NewExprImpl<PointedType extends Type> implements NewExpr<PointedType> {

	private static final int HASH_SEED = 8699;
	private volatile int hashCode = 0;

	private static final String EXPR_LABEL = "New";

	private final PointedType pointedType;

	NewExprImpl(final PointedType pointedType) {
		this.pointedType = checkNotNull(pointedType);
	}

	@Override
	public PointedType getPointedType() {
		return pointedType;
	}

	@Override
	public PointerType<PointedType> getType() {
		return Pointer(pointedType);
	}

	@Override
	public <P, R> R accept(final ExprVisitor<? super P, ? extends R> visitor, final P param) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = HASH_SEED;
			result = 31 * result + pointedType.hashCode();
			hashCode = result;
		}
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof NewExpr) {
			final NewExpr<?> that = (NewExpr<?>) obj;
			return this.getPointedType().equals(that.getPointedType());
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(EXPR_LABEL);
		sb.append("(");
		sb.append(pointedType);
		sb.append(")");
		return sb.toString();

	}

}
