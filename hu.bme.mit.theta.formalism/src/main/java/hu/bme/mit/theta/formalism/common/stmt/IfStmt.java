package hu.bme.mit.theta.formalism.common.stmt;

import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.type.BoolType;
import hu.bme.mit.theta.formalism.utils.StmtVisitor;

public interface IfStmt extends Stmt {
	
	public Expr<? extends BoolType> getCond();
	public Stmt getThen();
	
	@Override
	public default <P, R> R accept(StmtVisitor<? super P, ? extends R> visitor, P param) {
		return visitor.visit(this, param);
	}
	
}