package hu.bme.mit.inf.ttmc.code.ast;

import hu.bme.mit.inf.ttmc.code.ast.visitor.StatementVisitor;

public class IfStatementAst extends StatementAst {

	private ExpressionAst condition;
	private StatementAst thenStatement;
	private StatementAst elseStatement;
	
	public IfStatementAst(ExpressionAst condition, StatementAst thenStatement, StatementAst elseStatement) {
		this.condition = condition;
		this.thenStatement = thenStatement;
		this.elseStatement = elseStatement;
	}
	
	public IfStatementAst(ExpressionAst condition, StatementAst thenStatement) {
		this.condition = condition;
		this.thenStatement = thenStatement;
	}
	
	public ExpressionAst getCondition() {
		return this.condition;
	}
	
	public StatementAst getThen() {
		return this.thenStatement;
	}
	
	public StatementAst getElse() {
		return this.elseStatement;
	}
	
	@Override
	public AstNode[] getChildren() {
		if (this.elseStatement != null) {
			return new AstNode[] { condition, thenStatement, elseStatement };
		} else {
			return new AstNode[] { condition, thenStatement };
		}
	}
	
	@Override
	public <S> S accept(StatementVisitor<S> visitor) {
		return visitor.visit(this);
	}

	@Override
	public StatementAst copy() {		
		if (elseStatement != null)
			return new IfStatementAst(condition.copy(), thenStatement.copy(), elseStatement.copy());
		else
			return new IfStatementAst(condition.copy(), thenStatement.copy());
	}

}