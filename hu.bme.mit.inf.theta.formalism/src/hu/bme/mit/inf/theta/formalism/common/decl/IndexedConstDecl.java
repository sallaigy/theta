package hu.bme.mit.inf.theta.formalism.common.decl;

import hu.bme.mit.inf.theta.core.decl.ConstDecl;
import hu.bme.mit.inf.theta.core.type.Type;
import hu.bme.mit.inf.theta.formalism.common.expr.IndexedConstRefExpr;

public interface IndexedConstDecl<DeclType extends Type> extends ConstDecl<DeclType> {

	@Override
	public IndexedConstRefExpr<DeclType> getRef();

	public VarDecl<DeclType> getVarDecl();

	public int getIndex();

}
