package hu.bme.mit.theta.core.stmt;

import hu.bme.mit.theta.core.utils.StmtVisitor;

public interface Stmt {

	public <P, R> R accept(StmtVisitor<? super P, ? extends R> visitor, P param);

}