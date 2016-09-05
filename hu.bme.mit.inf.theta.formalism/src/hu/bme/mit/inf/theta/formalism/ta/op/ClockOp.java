package hu.bme.mit.inf.theta.formalism.ta.op;

import java.util.Collection;

import hu.bme.mit.inf.theta.formalism.common.decl.ClockDecl;
import hu.bme.mit.inf.theta.formalism.common.stmt.Stmt;
import hu.bme.mit.inf.theta.formalism.ta.utils.ClockOpVisitor;

public interface ClockOp {

	public Collection<? extends ClockDecl> getClocks();

	public Stmt toStmt();

	public <P, R> R accept(final ClockOpVisitor<? super P, ? extends R> visitor, final P param);

}
