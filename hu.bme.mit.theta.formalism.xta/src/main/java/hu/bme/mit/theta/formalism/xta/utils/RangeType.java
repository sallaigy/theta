package hu.bme.mit.theta.formalism.xta.utils;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import hu.bme.mit.theta.common.ObjectUtils;
import hu.bme.mit.theta.core.expr.IntLitExpr;
import hu.bme.mit.theta.core.expr.LitExpr;
import hu.bme.mit.theta.core.expr.impl.Exprs;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.utils.TypeVisitor;

public final class RangeType implements Type {
	private static final int HASH_SEED = 5441;
	private volatile int hashCode = 0;

	private final int lower;
	private final int upper;

	private RangeType(final int lower, final int upper) {
		checkArgument(lower <= upper);
		this.lower = lower;
		this.upper = upper;
	}

	public static RangeType Range(final int lower, final int upper) {
		return new RangeType(lower, upper);
	}

	public IntLitExpr Int(final int value) {
		checkArgument(value >= lower && value <= upper);
		return Exprs.Int(value);
	}

	public Stream<IntLitExpr> values() {
		return IntStream.rangeClosed(lower, upper).mapToObj(Exprs::Int);
	}

	public int getLower() {
		return lower;
	}

	public int getUpper() {
		return upper;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = HASH_SEED;
			result = 31 * result + lower;
			result = 31 * result + upper;
			hashCode = result;
		}
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof RangeType) {
			final RangeType that = (RangeType) obj;
			return this.lower == that.lower && this.upper == that.upper;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return ObjectUtils.toStringBuilder("Range").add(lower).add(upper).toString();
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
