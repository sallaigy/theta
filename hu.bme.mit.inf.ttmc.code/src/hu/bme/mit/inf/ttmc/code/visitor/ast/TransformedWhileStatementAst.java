package hu.bme.mit.inf.ttmc.code.visitor.ast;

import hu.bme.mit.inf.ttmc.code.ast.ExpressionAst;
import hu.bme.mit.inf.ttmc.code.ast.StatementAst;
import hu.bme.mit.inf.ttmc.code.ast.WhileStatementAst;

public class TransformedWhileStatementAst extends WhileStatementAst {

	public TransformedWhileStatementAst(ExpressionAst cond, StatementAst body) {
		super(cond, body);
	}

}
