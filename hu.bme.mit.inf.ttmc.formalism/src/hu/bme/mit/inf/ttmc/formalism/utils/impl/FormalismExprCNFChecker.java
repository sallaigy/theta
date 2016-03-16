package hu.bme.mit.inf.ttmc.formalism.utils.impl;

import hu.bme.mit.inf.ttmc.constraint.type.Type;
import hu.bme.mit.inf.ttmc.constraint.utils.impl.ExprCNFChecker;
import hu.bme.mit.inf.ttmc.formalism.common.expr.PrimedExpr;
import hu.bme.mit.inf.ttmc.formalism.common.expr.ProcCallExpr;
import hu.bme.mit.inf.ttmc.formalism.common.expr.ProcRefExpr;
import hu.bme.mit.inf.ttmc.formalism.common.expr.VarRefExpr;
import hu.bme.mit.inf.ttmc.formalism.utils.FormalismExprVisitor;

public final class FormalismExprCNFChecker extends ExprCNFChecker {

	// Provide own visitor that supports all formalism expressions
	@Override
	protected ExprCNFVisitor getCNFExprVisitor() {
		return new IsCNFFormalismExprVisitor();
	}
	
	private class IsCNFFormalismExprVisitor extends ExprCNFVisitor implements FormalismExprVisitor<CNFStatus, Boolean> {

		@Override
		public <ExprType extends Type> Boolean visit(PrimedExpr<ExprType> expr, CNFStatus param) {
			// A prime counts as a NOT expression, no boolean connective can be inside it
			return expr.getOp().accept(this, CNFStatus.INSIDE_NOT);
		}

		@Override
		public <DeclType extends Type> Boolean visit(VarRefExpr<DeclType> expr, CNFStatus param) {
			return true;
		}

		@Override
		public <ReturnType extends Type> Boolean visit(ProcRefExpr<ReturnType> expr, CNFStatus param) {
			return true;
		}

		@Override
		public <ReturnType extends Type> Boolean visit(ProcCallExpr<ReturnType> expr, CNFStatus param) {
			return true;
		}
		
	}
}