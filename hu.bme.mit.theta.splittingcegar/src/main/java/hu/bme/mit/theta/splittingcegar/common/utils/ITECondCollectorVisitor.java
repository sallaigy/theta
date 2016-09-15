package hu.bme.mit.theta.splittingcegar.common.utils;

import java.util.Collection;

import hu.bme.mit.theta.core.expr.ArrayReadExpr;
import hu.bme.mit.theta.core.expr.ArrayWriteExpr;
import hu.bme.mit.theta.core.expr.BinaryExpr;
import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.expr.FuncAppExpr;
import hu.bme.mit.theta.core.expr.FuncLitExpr;
import hu.bme.mit.theta.core.expr.IteExpr;
import hu.bme.mit.theta.core.expr.MultiaryExpr;
import hu.bme.mit.theta.core.expr.NullaryExpr;
import hu.bme.mit.theta.core.expr.UnaryExpr;
import hu.bme.mit.theta.core.type.BoolType;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.utils.impl.ArityBasedExprVisitor;
import hu.bme.mit.theta.formalism.common.expr.ClockRefExpr;
import hu.bme.mit.theta.formalism.common.expr.PrimedExpr;
import hu.bme.mit.theta.formalism.common.expr.ProcCallExpr;
import hu.bme.mit.theta.formalism.common.expr.ProcRefExpr;
import hu.bme.mit.theta.formalism.common.expr.VarRefExpr;
import hu.bme.mit.theta.formalism.utils.FormalismExprVisitor;

/**
 * Collect condition formulas of ITE expressions.
 */
public class ITECondCollectorVisitor extends ArityBasedExprVisitor<Collection<Expr<? extends BoolType>>, Void>
		implements FormalismExprVisitor<Collection<Expr<? extends BoolType>>, Void> {

	@Override
	public <ExprType extends Type> Void visit(final PrimedExpr<ExprType> expr,
			final Collection<Expr<? extends BoolType>> param) {
		return visitUnary(expr, param);
	}

	@Override
	public <ReturnType extends Type> Void visit(final ProcCallExpr<ReturnType> expr,
			final Collection<Expr<? extends BoolType>> param) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public <ReturnType extends Type> Void visit(final ProcRefExpr<ReturnType> expr,
			final Collection<Expr<? extends BoolType>> param) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public <DeclType extends Type> Void visit(final VarRefExpr<DeclType> expr,
			final Collection<Expr<? extends BoolType>> param) {
		return visitNullary(expr, param);
	}

	@Override
	public Void visit(final ClockRefExpr expr, final Collection<Expr<? extends BoolType>> param) {
		return visitNullary(expr, param);
	}

	@Override
	protected <ExprType extends Type> Void visitNullary(final NullaryExpr<ExprType> expr,
			final Collection<Expr<? extends BoolType>> param) {
		return null;
	}

	@Override
	protected <OpType extends Type, ExprType extends Type> Void visitUnary(final UnaryExpr<OpType, ExprType> expr,
			final Collection<Expr<? extends BoolType>> param) {
		expr.getOp().accept(this, param);
		return null;
	}

	@Override
	protected <LeftOpType extends Type, RightOpType extends Type, ExprType extends Type> Void visitBinary(
			final BinaryExpr<LeftOpType, RightOpType, ExprType> expr,
			final Collection<Expr<? extends BoolType>> param) {
		expr.getLeftOp().accept(this, param);
		expr.getRightOp().accept(this, param);
		return null;
	}

	@Override
	protected <OpsType extends Type, ExprType extends Type> Void visitMultiary(
			final MultiaryExpr<OpsType, ExprType> expr, final Collection<Expr<? extends BoolType>> param) {
		for (final Expr<? extends OpsType> op : expr.getOps())
			op.accept(this, param);
		return null;
	}

	@Override
	public <IndexType extends Type, ElemType extends Type> Void visit(final ArrayReadExpr<IndexType, ElemType> expr,
			final Collection<Expr<? extends BoolType>> param) {
		return null;
	}

	@Override
	public <IndexType extends Type, ElemType extends Type> Void visit(final ArrayWriteExpr<IndexType, ElemType> expr,
			final Collection<Expr<? extends BoolType>> param) {
		return null;
	}

	@Override
	public <ParamType extends Type, ResultType extends Type> Void visit(final FuncLitExpr<ParamType, ResultType> expr,
			final Collection<Expr<? extends BoolType>> param) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public <ParamType extends Type, ResultType extends Type> Void visit(final FuncAppExpr<ParamType, ResultType> expr,
			final Collection<Expr<? extends BoolType>> param) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public <ExprType extends Type> Void visit(final IteExpr<ExprType> expr,
			final Collection<Expr<? extends BoolType>> param) {
		param.add(expr.getCond());
		expr.getThen().accept(this, param);
		expr.getElse().accept(this, param);
		return null;
	}

}