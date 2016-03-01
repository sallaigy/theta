package hu.bme.mit.inf.ttmc.program.stmt;

import hu.bme.mit.inf.ttmc.constraint.expr.Expr;
import hu.bme.mit.inf.ttmc.constraint.type.BoolType;
import hu.bme.mit.inf.ttmc.program.utils.StmtVisitor;

public interface AssertStmt extends Stmt {
	
	public Expr<? extends BoolType> getCond();
	
	@Override
	public default <P, R> R accept(StmtVisitor<? super P, ? extends R> visitor, P param) {
		return visitor.visit(this, param);
	}
	
}