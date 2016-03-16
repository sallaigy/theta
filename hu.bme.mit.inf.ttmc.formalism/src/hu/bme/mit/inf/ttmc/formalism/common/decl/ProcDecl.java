package hu.bme.mit.inf.ttmc.formalism.common.decl;

import java.util.List;

import hu.bme.mit.inf.ttmc.constraint.decl.Decl;
import hu.bme.mit.inf.ttmc.constraint.decl.ParamDecl;
import hu.bme.mit.inf.ttmc.constraint.type.Type;
import hu.bme.mit.inf.ttmc.formalism.common.expr.ProcRefExpr;
import hu.bme.mit.inf.ttmc.formalism.common.type.ProcType;

public interface ProcDecl<ReturnType extends Type> extends Decl<ProcType<ReturnType>> {
	
	public List<? extends ParamDecl<?>> getParamDecls();

	public ReturnType getReturnType();

	@Override
	public ProcRefExpr<ReturnType> getRef();
}