package hu.bme.mit.inf.ttmc.formalism.common.expr.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.inf.ttmc.core.expr.Expr;
import hu.bme.mit.inf.ttmc.core.type.Type;
import hu.bme.mit.inf.ttmc.formalism.common.decl.ClockDecl;
import hu.bme.mit.inf.ttmc.formalism.common.decl.ProcDecl;
import hu.bme.mit.inf.ttmc.formalism.common.decl.VarDecl;
import hu.bme.mit.inf.ttmc.formalism.common.expr.ClockRefExpr;
import hu.bme.mit.inf.ttmc.formalism.common.expr.PrimedExpr;
import hu.bme.mit.inf.ttmc.formalism.common.expr.ProcRefExpr;
import hu.bme.mit.inf.ttmc.formalism.common.expr.VarRefExpr;

public final class Exprs2 {

	private Exprs2() {
	}

	// TODO Find a more elegant solution
	public static <T extends Type> VarRefExpr<T> Ref(final VarDecl<T> varDecl) {
		checkNotNull(varDecl);
		if (varDecl instanceof ClockDecl) {
			@SuppressWarnings("unchecked")
			final VarRefExpr<T> result = (VarRefExpr<T>) Ref((ClockDecl) varDecl);
			return result;
		} else {
			return new VarRefExprImpl<>(varDecl);
		}
	}

	public static ClockRefExpr Ref(final ClockDecl clockDecl) {
		checkNotNull(clockDecl);
		return new ClockRefExprImpl(clockDecl);
	}

	public static <R extends Type> ProcRefExpr<R> Ref(final ProcDecl<R> procDecl) {
		checkNotNull(procDecl);
		return new ProcRefExprImpl<>(procDecl);
	}

	public static <T extends Type> PrimedExpr<T> Prime(final Expr<? extends T> op) {
		checkNotNull(op);
		return new PrimedExprImpl<>(op);
	}

}
