package hu.bme.mit.theta.core.stmt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import hu.bme.mit.theta.common.ToStringBuilder;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.utils.StmtVisitor;

public final class DeclStmt<DeclType extends Type> implements Stmt {

	private static final int HASH_SEED = 4201;

	private volatile int hashCode = 0;

	private final VarDecl<DeclType> varDecl;

	private final Optional<Expr<DeclType>> initVal;

	DeclStmt(final VarDecl<DeclType> varDecl) {
		this.varDecl = checkNotNull(varDecl);
		initVal = Optional.empty();
	}

	public DeclStmt(final VarDecl<DeclType> varDecl, final Expr<DeclType> initVal) {
		this.varDecl = checkNotNull(varDecl);
		this.initVal = Optional.of(checkNotNull(initVal));
	}

	public VarDecl<DeclType> getVarDecl() {
		return varDecl;
	}

	public Optional<Expr<DeclType>> getInitVal() {
		return initVal;
	}

	@Override
	public <P, R> R accept(final StmtVisitor<? super P, ? extends R> visitor, final P param) {
		return visitor.visit(this, param);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = HASH_SEED;
			result = 31 * result + varDecl.hashCode();
			result = 31 * result + initVal.hashCode();
			hashCode = result;
		}
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof DeclStmt) {
			final DeclStmt<?> that = (DeclStmt<?>) obj;
			return this.getVarDecl().equals(that.getVarDecl()) && this.getInitVal().equals(that.getInitVal());
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		final ToStringBuilder builder = Utils.toStringBuilder("Decl").add(varDecl);
		if (initVal.isPresent()) {
			builder.add(initVal.get());
		}
		return builder.toString();
	}

}
