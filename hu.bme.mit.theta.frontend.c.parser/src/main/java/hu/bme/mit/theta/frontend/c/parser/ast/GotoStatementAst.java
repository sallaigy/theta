package hu.bme.mit.theta.frontend.c.parser.ast;

import hu.bme.mit.theta.frontend.c.parser.ast.visitor.StatementVisitor;

public class GotoStatementAst extends StatementAst {

	private String label;

	public GotoStatementAst(String label) {
		this.label = label;
	}

	public String getLabel() {
		return this.label;
	}

	@Override
	public <S> S accept(StatementVisitor<S> visitor) {
		return visitor.visit(this);
	}

	@Override
	public StatementAst copy() {
		return new GotoStatementAst(label);
	}

	@Override
	public AstNode[] getChildren() {
		return new AstNode[] {};
	}

}