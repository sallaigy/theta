package hu.bme.mit.theta.formalism.xta;

import java.util.Optional;

import hu.bme.mit.theta.core.expr.LitExpr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.utils.TypeVisitor;

public final class ChanType implements Type {

	private static final ChanType INSTANCE = new ChanType();

	private ChanType() {
	}

	public static ChanType getInstance() {
		return INSTANCE;
	}

	@Override
	public String toString() {
		return "Chan";
	}

	////

	@Override
	public LitExpr<? extends Type> getAny() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	@Override
	public boolean isLeq(final Type type) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	@Override
	public Optional<? extends Type> meet(final Type type) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	@Override
	public Optional<? extends Type> join(final Type type) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	@Override
	public <P, R> R accept(final TypeVisitor<? super P, ? extends R> visitor, final P param) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

}
