package hu.bme.mit.inf.ttmc.code.visitor;

import java.util.ArrayList;
import java.util.List;

import hu.bme.mit.inf.ttmc.code.ast.AssignmentInitializerAst;
import hu.bme.mit.inf.ttmc.code.ast.BinaryExpressionAst;
import hu.bme.mit.inf.ttmc.code.ast.BinaryExpressionAst.Operator;
import hu.bme.mit.inf.ttmc.code.ast.BreakStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.CaseStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.CompoundStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.ContinueStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.DeclarationAst;
import hu.bme.mit.inf.ttmc.code.ast.DoStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.ExpressionAst;
import hu.bme.mit.inf.ttmc.code.ast.ExpressionListAst;
import hu.bme.mit.inf.ttmc.code.ast.ExpressionStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.ForStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.FunctionDefinitionAst;
import hu.bme.mit.inf.ttmc.code.ast.GotoStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.FunctionCallExpressionAst;
import hu.bme.mit.inf.ttmc.code.ast.IfStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.LiteralExpressionAst;
import hu.bme.mit.inf.ttmc.code.ast.NameExpressionAst;
import hu.bme.mit.inf.ttmc.code.ast.NullStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.ReturnStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.StatementAst;
import hu.bme.mit.inf.ttmc.code.ast.SwitchStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.UnaryExpressionAst;
import hu.bme.mit.inf.ttmc.code.ast.VarDeclarationAst;
import hu.bme.mit.inf.ttmc.code.ast.DeclarationStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.DefaultStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.InitDeclaratorAst;
import hu.bme.mit.inf.ttmc.code.ast.LabeledStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.WhileStatementAst;
import hu.bme.mit.inf.ttmc.code.ast.visitor.DeclarationVisitor;
import hu.bme.mit.inf.ttmc.code.ast.visitor.ExpressionVisitor;
import hu.bme.mit.inf.ttmc.code.ast.visitor.StatementVisitor;
import hu.bme.mit.inf.ttmc.code.util.SymbolTable;
import hu.bme.mit.inf.ttmc.core.decl.Decl;
import hu.bme.mit.inf.ttmc.core.expr.Expr;
import hu.bme.mit.inf.ttmc.core.type.BoolType;
import hu.bme.mit.inf.ttmc.core.type.IntType;
import hu.bme.mit.inf.ttmc.core.type.RatType;
import hu.bme.mit.inf.ttmc.core.type.Type;
import hu.bme.mit.inf.ttmc.core.type.closure.ClosedUnderAdd;
import hu.bme.mit.inf.ttmc.core.type.closure.ClosedUnderMul;
import hu.bme.mit.inf.ttmc.core.type.closure.ClosedUnderNeg;
import hu.bme.mit.inf.ttmc.core.type.closure.ClosedUnderSub;
import hu.bme.mit.inf.ttmc.core.utils.impl.ExprUtils;
import hu.bme.mit.inf.ttmc.formalism.common.decl.VarDecl;
import hu.bme.mit.inf.ttmc.formalism.common.expr.VarRefExpr;

import hu.bme.mit.inf.ttmc.formalism.common.stmt.Stmt;

import static hu.bme.mit.inf.ttmc.core.expr.impl.Exprs.*;
import static hu.bme.mit.inf.ttmc.formalism.common.expr.impl.Exprs2.*;
import static hu.bme.mit.inf.ttmc.formalism.common.stmt.impl.Stmts.*;
import static hu.bme.mit.inf.ttmc.core.type.impl.Types.*;
import static hu.bme.mit.inf.ttmc.core.decl.impl.Decls.*;
import static hu.bme.mit.inf.ttmc.formalism.common.decl.impl.Decls2.*;

public class TransformProgramVisitor implements ExpressionVisitor<Expr<? extends Type>>, StatementVisitor<Stmt>, DeclarationVisitor<Decl<? extends Type, ?>> {

		
	private SymbolTable<Decl<? extends Type, ?>> symbols = new SymbolTable<>();
	
	@Override
	public Expr<? extends Type> visit(BinaryExpressionAst ast) {
		ExpressionAst lhs = ast.getLeft();
		ExpressionAst rhs = ast.getRight();
		
		Expr<? extends Type> left  = lhs.accept(this);
		Expr<? extends Type> right = rhs.accept(this);
		
		switch (ast.getOperator()) {
			case OP_ADD:
				return Add(ExprUtils.cast(left, ClosedUnderAdd.class), ExprUtils.cast(right, ClosedUnderAdd.class));
			case OP_SUB:
				return Sub(ExprUtils.cast(left, ClosedUnderSub.class), ExprUtils.cast(right, ClosedUnderSub.class));
			case OP_MUL:
				return Mul(ExprUtils.cast(left, ClosedUnderMul.class), ExprUtils.cast(right, ClosedUnderMul.class));
			case OP_IS_GT:
				return Gt(ExprUtils.cast(left, RatType.class), ExprUtils.cast(right, RatType.class));
			case OP_IS_LT:
				return Lt(ExprUtils.cast(left, RatType.class), ExprUtils.cast(right, RatType.class));
			case OP_IS_EQ:
				return Eq(left, right);
			case OP_IS_NOT_EQ:
				return Neq(left, right);
			case OP_DIV:
				return IntDiv(ExprUtils.cast(left, IntType.class), ExprUtils.cast(right, IntType.class));
			case OP_IS_GTEQ:
				return Geq(ExprUtils.cast(left, RatType.class), ExprUtils.cast(right, RatType.class));
			case OP_IS_LTEQ:
				return Leq(ExprUtils.cast(left, RatType.class), ExprUtils.cast(right, RatType.class));
			case OP_ASSIGN: // intentional
			default:
				break;
		}
		
		return null;
	}

	@Override
	public Expr<? extends Type> visit(LiteralExpressionAst ast) {
		return Int(ast.getValue());
	}

	@Override
	public Expr<? extends Type> visit(NameExpressionAst ast) {
		return this.symbols.get(ast.getName()).getRef();
	}

	@Override
	public Stmt visit(IfStatementAst ast) {
		ExpressionAst condAst = ast.getCondition();
		StatementAst thenAst = ast.getThen();
		StatementAst elseAst = ast.getElse();

		Expr<? extends BoolType> cond = ExprUtils.cast(condAst.accept(this), BoolType.class);
		Stmt then = thenAst.accept(this);
		
		if (elseAst != null) {
			Stmt elze = elseAst.accept(this);
			return If(cond, then, elze);
		} else {
			return If(cond, then);
		}
	}

	@Override
	public Stmt visit(CompoundStatementAst ast) {
		List<Stmt> stmts = new ArrayList<>();
		
		for (StatementAst child : ast.getStatements()) {			
			stmts.add(child.accept(this));
		}
		
		return Block(stmts);
	}

	@Override
	public Stmt visit(DeclarationStatementAst ast) {
		List<Stmt> stmts = new ArrayList<>();
		DeclarationAst decl = ast.getDeclaration();
		
		if (decl instanceof VarDeclarationAst) {
			VarDeclarationAst varDecl = (VarDeclarationAst) decl;

			// Every declaration contains a single declarator because of the earlier transformations
			InitDeclaratorAst declarator = (InitDeclaratorAst) varDecl.getDeclarators().get(0); // TODO
			AssignmentInitializerAst initializer = (AssignmentInitializerAst) declarator.getInitializer();
			
			String name = declarator.getName();
			
			VarDecl<? extends Type> var = Var(name, Int());
			this.symbols.put(name, var);			
			
			if (null == initializer) {
				stmts.add(Decl(var));
			} else {
				Expr<? extends Type> initExpr = initializer.getExpression().accept(this);
				stmts.add(Decl(var, ExprUtils.cast(initExpr, var.getType().getClass())));
			}
		}
		
		return Block(stmts);
	}
	
	@Override
	public Stmt visit(ReturnStatementAst ast) {
		Expr<? extends Type> expr = ast.getExpression().accept(this);
		
		return Return(expr);
	}

	@Override
	public Stmt visit(ExpressionStatementAst ast) {
		// In Stmt, assignments cannot be expressions, only statements
		ExpressionAst expr = ast.getExpression();
		
		if (expr instanceof BinaryExpressionAst && ((BinaryExpressionAst) expr).getOperator() == Operator.OP_ASSIGN) {
			BinaryExpressionAst binary = (BinaryExpressionAst) expr;
			
			Expr<? extends Type> lhs = binary.getLeft().accept(this);
			Expr<?> rhs = binary.getRight().accept(this);
			
			if (!(lhs instanceof VarRefExpr<?>)) {
				throw new RuntimeException("Assignment lvalue can only be a variable reference.");
			}
			
			VarRefExpr<Type> left = (VarRefExpr<Type>) lhs;
			
			return Assign(left.getDecl(), rhs);
		}
		
		// Call to assertion functions must be replaced by condition assert statements
		if (expr instanceof FunctionCallExpressionAst) {
			FunctionCallExpressionAst func = (FunctionCallExpressionAst) ast.getExpression();
			
			if (func.getName().equals("assert")) {
				ExpressionAst cond = func.getParams().get(0); // The first parameter is the condition
				return Assert(ExprUtils.cast(cond.accept(this), BoolType.class));
			}
		}
		
		return Skip();
	}

	@Override
	public Stmt visit(WhileStatementAst ast) {
		ExpressionAst condAst = ast.getCondition();
		StatementAst  bodyAst = ast.getBody();
		
		Expr<? extends BoolType> cond = ExprUtils.cast(condAst.accept(this), BoolType.class);
		Stmt body = bodyAst.accept(this);
		
		return While(cond, body);
	}

	@Override
	public Decl<? extends Type, ?> visit(VarDeclarationAst ast) {
		throw new RuntimeException("This code should not be reachable");
	}

	@Override
	public Expr<? extends Type> visit(UnaryExpressionAst ast) {
		switch (ast.getOperator()) {
		case OP_MINUS:
			// The minus operation is negation
			return Neg(ExprUtils.cast(ast.getOperand().accept(this), ClosedUnderNeg.class));
		case OP_PLUS:
			// The unary plus operator promotes the operand to an integral type
			// Since only integer variables are supported atm, this means a no-op
			return ast.getOperand().accept(this);
		case OP_NOT:
			return Not(ExprUtils.cast(ast.getOperand().accept(this), BoolType.class));
		case OP_POSTFIX_DECR:
		case OP_PREFIX_DECR:
		case OP_POSTFIX_INCR:
		case OP_PREFIX_INCR:
		default:
			// These operations should have been eliminated earlier.
			throw new RuntimeException("This code should not be reachable.");
		}
	}

	@Override
	public Stmt visit(DoStatementAst ast) {
		ExpressionAst condAst = ast.getCondition();
		StatementAst  bodyAst = ast.getBody();
		
		Expr<? extends BoolType> cond = ExprUtils.cast(condAst.accept(this), BoolType.class);
		Stmt body = bodyAst.accept(this);
		
		return Do(body, cond);
	}
	
	@Override
	public Stmt visit(NullStatementAst ast) {
		return Skip();
	}


	@Override
	public Decl<? extends Type, ?> visit(FunctionDefinitionAst ast) {
		throw new UnsupportedOperationException("TODO: Function Defs");
	}

	@Override
	public Expr<? extends Type> visit(FunctionCallExpressionAst ast) {
		throw new UnsupportedOperationException("TODO: Function Calls");
	}
	
	@Override
	public Stmt visit(GotoStatementAst ast) {
		throw new UnsupportedOperationException("TODO: GOTO Stmt");
	}

	@Override
	public Stmt visit(LabeledStatementAst ast) {
		throw new UnsupportedOperationException("TODO: Labeled Stmt");
	}
	
	/* These statements are not supported since earlier transformations should eliminate them. */


	@Override
	public Stmt visit(ForStatementAst ast) {
		throw new UnsupportedOperationException("TransformProgramVisitor does not support for loops.");
	}

	@Override
	public Stmt visit(SwitchStatementAst ast) {
		throw new UnsupportedOperationException("TransformProgramVisitor does not support switch statements.");
	}

	@Override
	public Stmt visit(CaseStatementAst ast) {
		throw new UnsupportedOperationException("TransformProgramVisitor does not support case statements.");
	}

	@Override
	public Stmt visit(DefaultStatementAst ast) {
		throw new UnsupportedOperationException("TransformProgramVisitor does not support default statements.");
	}

	@Override
	public Stmt visit(ContinueStatementAst ast) {
		throw new UnsupportedOperationException("TransformProgramVisitor does not support continue statements.");
	}

	@Override
	public Stmt visit(BreakStatementAst ast) {
		throw new UnsupportedOperationException("TransformProgramVisitor does not support break statements.");
	}

	@Override
	public Expr<? extends Type> visit(ExpressionListAst ast) {
		throw new UnsupportedOperationException("TransformProgramVisitor does not support expression lists.");
	}
}
