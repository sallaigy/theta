package hu.bme.mit.inf.ttmc.code.ast.visitor;

import java.util.ArrayList;
import java.util.List;

import hu.bme.mit.inf.ttmc.code.ast.BinaryExpressionAst;
import hu.bme.mit.inf.ttmc.code.ast.BreakStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.CaseStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.CompoundStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.ContinueStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.DeclarationAst;
import hu.bme.mit.inf.ttmc.code.ast.DeclarationStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.DeclaratorAst;
import hu.bme.mit.inf.ttmc.code.ast.DefaultStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.DoStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.ExpressionAst;
import hu.bme.mit.inf.ttmc.code.ast.ExpressionListAst;
import hu.bme.mit.inf.ttmc.code.ast.ExpressionStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.ForStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.FunctionCallExpressionAst;
import hu.bme.mit.inf.ttmc.code.ast.FunctionDeclaratorAst;
import hu.bme.mit.inf.ttmc.code.ast.FunctionDefinitionAst;
import hu.bme.mit.inf.ttmc.code.ast.GotoStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.IfStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.InitDeclaratorAst;
import hu.bme.mit.inf.ttmc.code.ast.LabeledStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.LiteralExpressionAst;
import hu.bme.mit.inf.ttmc.code.ast.NameExpressionAst;
import hu.bme.mit.inf.ttmc.code.ast.NullStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.ReturnStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.StatementAst;
import hu.bme.mit.inf.ttmc.code.ast.SwitchStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.TranslationUnitAst;
import hu.bme.mit.inf.ttmc.code.ast.UnaryExpressionAst;
import hu.bme.mit.inf.ttmc.code.ast.VarDeclarationAst;
import hu.bme.mit.inf.ttmc.code.ast.WhileStatementAst;

public class CloneAstVisitor implements AstVisitor<ExpressionAst, StatementAst, DeclarationAst, DeclaratorAst, TranslationUnitAst> {

	@Override
	public ExpressionAst visit(BinaryExpressionAst ast) {
		return new BinaryExpressionAst(ast.getLeft().accept(this), ast.getRight().accept(this), ast.getOperator());
	}

	@Override
	public ExpressionAst visit(LiteralExpressionAst ast) {
		return new LiteralExpressionAst(ast.getValue());
	}

	@Override
	public ExpressionAst visit(NameExpressionAst ast) {
		return new NameExpressionAst(ast.getName());
	}

	@Override
	public ExpressionAst visit(FunctionCallExpressionAst ast) {
		List<ExpressionAst> params = new ArrayList<>();
		for (ExpressionAst param : ast.getParams()) {
			params.add(param.accept(this));
		}
		
		return new FunctionCallExpressionAst(ast.getName(), params);
	}

	@Override
	public ExpressionAst visit(ExpressionListAst ast) {
		List<ExpressionAst> members = new ArrayList<>();
		for (ExpressionAst expr : ast.getExpressions()) {
			members.add(expr.accept(this));
		}
		
		return new ExpressionListAst(members);
	}

	@Override
	public ExpressionAst visit(UnaryExpressionAst ast) {
		return new UnaryExpressionAst(ast.getOperand(), ast.getOperator());
	}

	@Override
	public StatementAst visit(IfStatementAst ast) {
		if (ast.getElse() == null)
			return new IfStatementAst(ast.getCondition(), ast.getThen().accept(this));
		else
			return new IfStatementAst(ast.getCondition(), ast.getThen().accept(this), ast.getElse().accept(this));
	}

	@Override
	public StatementAst visit(CompoundStatementAst ast) {
		List<StatementAst> stmts = new ArrayList<>();
		for (StatementAst stmt : ast.getStatements()) {
			stmts.add(stmt.accept(this));
		}
		
		return new CompoundStatementAst(stmts);
	}

	@Override
	public StatementAst visit(DeclarationStatementAst ast) {
		return new DeclarationStatementAst(ast.getDeclaration().accept(this));
	}

	@Override
	public StatementAst visit(ReturnStatementAst ast) {
		return new ReturnStatementAst(ast.getExpression().accept(this));
	}

	@Override
	public StatementAst visit(ExpressionStatementAst ast) {
		return new ExpressionStatementAst(ast.getExpression().accept(this));
	}

	@Override
	public StatementAst visit(WhileStatementAst ast) {
		return new WhileStatementAst(ast.getCondition().accept(this), ast.getBody().accept(this));
	}

	@Override
	public StatementAst visit(ForStatementAst ast) {
		return new ForStatementAst(ast.getInit().accept(this), ast.getCondition().accept(this), ast.getIteration().accept(this), ast.getBody().accept(this));
	}

	@Override
	public StatementAst visit(DoStatementAst ast) {
		return new DoStatementAst(ast.getCondition().accept(this), ast.getBody());
	}

	@Override
	public DeclarationAst visit(VarDeclarationAst ast) {
		List<DeclaratorAst> declarators = new ArrayList<>();
		for (DeclaratorAst declarator : ast.getDeclarators()) {
			declarators.add(declarator.accept(this));
		}
		
		return new VarDeclarationAst(ast.getSpecifier(), declarators);
	}

	@Override
	public DeclarationAst visit(FunctionDefinitionAst ast) {
		FunctionDeclaratorAst declarator = (FunctionDeclaratorAst) ast.getDeclarator().accept(this);
		CompoundStatementAst body = (CompoundStatementAst) ast.getBody().accept(this);
		
		return new FunctionDefinitionAst(ast.getName(), ast.getDeclarationSpecifier(), declarator, body);
	}

	@Override
	public DeclaratorAst visit(InitDeclaratorAst ast) {
		return new InitDeclaratorAst(ast.getName(), ast.getInitializer());
	}

	@Override
	public DeclaratorAst visit(FunctionDeclaratorAst ast) {
		return new FunctionDeclaratorAst(ast.getName());
	}

	@Override
	public StatementAst visit(SwitchStatementAst ast) {
		return new SwitchStatementAst(ast.getExpression().accept(this), ast.getBody().accept(this));
	}

	@Override
	public StatementAst visit(CaseStatementAst ast) {
		return new CaseStatementAst(ast.getExpression());
	}

	@Override
	public StatementAst visit(DefaultStatementAst ast) {
		return ast;
	}

	@Override
	public StatementAst visit(ContinueStatementAst ast) {
		return ast;
	}

	@Override
	public StatementAst visit(BreakStatementAst ast) {
		return ast;
	}

	@Override
	public StatementAst visit(GotoStatementAst ast) {
		return ast;
	}

	@Override
	public StatementAst visit(LabeledStatementAst ast) {
		return ast;
	}

	@Override
	public StatementAst visit(NullStatementAst ast) {
		return ast;
	}

	@Override
	public TranslationUnitAst visit(TranslationUnitAst ast) {
		List<DeclarationAst> decl = new ArrayList<>();
		for (DeclarationAst d : ast.getDeclarations()) {
			decl.add(d.accept(this));
		}
		
		return new TranslationUnitAst(decl);
	}

}