package hu.bme.mit.inf.ttmc.constraint.utils.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;

import hu.bme.mit.inf.ttmc.constraint.ConstraintManager;
import hu.bme.mit.inf.ttmc.constraint.expr.ArrayReadExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.ArrayWriteExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.BinaryExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.Expr;
import hu.bme.mit.inf.ttmc.constraint.expr.FuncAppExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.FuncLitExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.IteExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.MultiaryExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.NullaryExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.UnaryExpr;
import hu.bme.mit.inf.ttmc.constraint.factory.ExprFactory;
import hu.bme.mit.inf.ttmc.constraint.type.BoolType;
import hu.bme.mit.inf.ttmc.constraint.type.Type;
import hu.bme.mit.inf.ttmc.constraint.utils.ExprVisitor;

public class ExprITEEliminator {
	private PropagateITEVisitor propagateITEVisitor;
	private RemoveITEVisitor removeITEVisitor;
	
	public ExprITEEliminator(ConstraintManager manager) {
		propagateITEVisitor = getPropageteITEVisitor(manager);
		removeITEVisitor = getRemoveITEVisitor(manager);
	}
	
	/**
	 * Eliminate if-then-else expressions by replacing them with boolean connectives.
	 * @param expr Expression from where ITEs have to be eliminated
	 * @return New expression with no ITEs
	 */
	@SuppressWarnings("unchecked")
	public final Expr<? extends BoolType> eliminate(Expr<? extends BoolType> expr) {
		return (Expr<? extends BoolType>) expr.accept(propagateITEVisitor, null).accept(removeITEVisitor, null);
	}
	
	// Subclasses can override this method to provide a different visitor
	// (supporting more types of expressions)
	protected PropagateITEVisitor getPropageteITEVisitor(ConstraintManager manager) {
		return new PropagateITEVisitor(manager, new PushBelowITEVisitor(manager, new IsBoolConnExprVisitor()));
	}
	
	// Subclasses can override this method to provide a different visitor
	// (supporting more types of expressions)
	protected RemoveITEVisitor getRemoveITEVisitor(ConstraintManager manager) {
		return new RemoveITEVisitor(manager);
	}
	
	/**
	 * Helper visitor 1: Propagate ITE up in the expression tree as high as possible.
	 * For example: x = 1 + ite(c, t, e) --> ite(c, x = 1 + t, x = 1 + e)
	 */
	protected static class PropagateITEVisitor extends ArityBasedExprVisitor<Void, Expr<? extends Type>> {
		private ExprVisitor<Void, Expr<? extends Type>> pushBelowITEVisitor;

		public PropagateITEVisitor(ConstraintManager manager, ExprVisitor<Void, Expr<? extends Type>> pushBelowIteVisitor) {
			this.pushBelowITEVisitor = pushBelowIteVisitor;
		}
		
		@Override
		protected <ExprType extends Type> Expr<? extends Type> visitNullary(NullaryExpr<ExprType> expr, Void param) {
			return expr;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected <OpType extends Type, ExprType extends Type> Expr<? extends Type> visitUnary(
				UnaryExpr<OpType, ExprType> expr, Void param) {
			// Apply propagation to operand(s) first, then apply pushdown
			return expr.withOp((Expr<? extends OpType>) expr.getOp().accept(this, param)).accept(pushBelowITEVisitor, param);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected <LeftOpType extends Type, RightOpType extends Type, ExprType extends Type> Expr<? extends Type> visitBinary(
				BinaryExpr<LeftOpType, RightOpType, ExprType> expr, Void param) {
			// Apply propagation to operand(s) first, then apply pushdown
			return expr.withOps(
					(Expr<? extends LeftOpType>) expr.getLeftOp().accept(this, param),
					(Expr<? extends RightOpType>) expr.getRightOp().accept(this, param))
					.accept(pushBelowITEVisitor, param);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected <OpsType extends Type, ExprType extends Type> Expr<? extends Type> visitMultiary(
				MultiaryExpr<OpsType, ExprType> expr, Void param) {
			// Apply propagation to operand(s) first, then apply pushdown
			List<Expr<? extends OpsType>> ops = new ArrayList<>(expr.getOps().size());
			for (Expr<? extends OpsType> op : expr.getOps()) ops.add((Expr<? extends OpsType>) op.accept(this, param));
			return expr.withOps(ops).accept(pushBelowITEVisitor, param);
		}

		@Override
		public <IndexType extends Type, ElemType extends Type> Expr<? extends Type> visit(
				ArrayReadExpr<IndexType, ElemType> expr, Void param) {
			throw new UnsupportedOperationException("TODO");
		}

		@Override
		public <IndexType extends Type, ElemType extends Type> Expr<? extends Type> visit(
				ArrayWriteExpr<IndexType, ElemType> expr, Void param) {
			throw new UnsupportedOperationException("TODO");
		}

		@Override
		public <ParamType extends Type, ResultType extends Type> Expr<? extends Type> visit(
				FuncLitExpr<ParamType, ResultType> expr, Void param) {
			throw new UnsupportedOperationException("TODO");
		}

		@Override
		public <ParamType extends Type, ResultType extends Type> Expr<? extends Type> visit(
				FuncAppExpr<ParamType, ResultType> expr, Void param) {
			throw new UnsupportedOperationException("TODO");
		}

		@SuppressWarnings("unchecked")
		@Override
		public <ExprType extends Type> Expr<? extends Type> visit(IteExpr<ExprType> expr, Void param) {
			// Apply propagation to operand(s)
			return expr.withOps(
					expr.getCond(),
					(Expr<? extends ExprType>) expr.getThen().accept(this, param),
					(Expr<? extends ExprType>) expr.getElse().accept(this, param));
		}
	}
	
	/**
	 * Helper visitor 2: Push an expression below an ITE recursively.
	 * For example: x = ite (c1, ite(c2, t, e1), e2) --> ite(c1, ite(c2, x = t, x = e1), x = e2)
	 */
	protected static class PushBelowITEVisitor extends ArityBasedExprVisitor<Void, Expr<? extends Type>> {
		
		private ConstraintManager manager;
		private ExprVisitor<Void, Boolean> isBooleanVisitor;
		
		/**
		 * Constructor.
		 * @param manager Constraint manager
		 */
		public PushBelowITEVisitor(ConstraintManager manager, ExprVisitor<Void, Boolean> isBooleanVisitor) {
			this.manager = manager;
			this.isBooleanVisitor = isBooleanVisitor;
		}

		@Override
		protected <ExprType extends Type> Expr<? extends Type> visitNullary(NullaryExpr<ExprType> expr, Void param) {
			return expr;
		}

		@Override
		protected <OpType extends Type, ExprType extends Type> Expr<? extends Type> visitUnary(
				UnaryExpr<OpType, ExprType> expr, Void param) {
			// Nothing to do if the operand is not an ITE or it is of bool type
			if (!(expr.getOp() instanceof IteExpr) || expr.accept(isBooleanVisitor, null)) {
				return expr;
			} else {
				// Push expression below ITE, e.g., -ite(C,T,E) => ite(C,-T,-E)
				IteExpr<? extends OpType> op = (IteExpr<? extends OpType>) expr.getOp();
				return manager.getExprFactory().Ite(op.getCond(), expr.withOp(op.getThen()).accept(this, param), expr.withOp(op.getElse()).accept(this, param));
			}
			
		}
		
		@Override
		protected <LeftOpType extends Type, RightOpType extends Type, ExprType extends Type> Expr<? extends Type> visitBinary(
				BinaryExpr<LeftOpType, RightOpType, ExprType> expr, Void param) {
			// Nothing to do if the none of the operands are ITE or it is of bool type
			if (!(expr.getLeftOp() instanceof IteExpr || expr.getRightOp() instanceof IteExpr) || expr.accept(isBooleanVisitor, null)) {
				return expr;
			} else if (expr.getLeftOp() instanceof IteExpr) {
				// Push expression below ITE, e.g., ite(C,T,E) = X => ite(C,T=X,E=X)
				IteExpr<? extends LeftOpType> op = (IteExpr<? extends LeftOpType>) expr.getLeftOp();
				return manager.getExprFactory().Ite(
						op.getCond(),
						expr.withLeftOp(op.getThen()).accept(this, param),
						expr.withLeftOp(op.getElse()).accept(this, param));
			} else /* expr.getRightOp() must be an ITE */ {
				// Push expression below ITE, e.g., X = ite(C,T,E) => ite(C,x=T,X=E)
				IteExpr<? extends RightOpType> op = (IteExpr<? extends RightOpType>) expr.getRightOp();
				return manager.getExprFactory().Ite(
						op.getCond(),
						expr.withRightOp(op.getThen()).accept(this, param),
						expr.withRightOp(op.getElse()).accept(this, param));
			}
		}

		@Override
		protected <OpsType extends Type, ExprType extends Type> Expr<? extends Type> visitMultiary(
				MultiaryExpr<OpsType, ExprType> expr, Void param) {
			// Get the first operand which is an ITE
			Optional<? extends Expr<? extends OpsType>> firstIte = expr.getOps().stream().filter(op -> op instanceof IteExpr).findFirst();
			// Nothing to do if the none of the operands are ITE or it is of bool type
			if (!firstIte.isPresent() || expr.accept(isBooleanVisitor, null)) {
				return expr;
			} else {
				// Push expression below ITE, e.g., X + ite(C,T,E) + Y  => ite(C,X+T+Y,X+E+Y)
				List<Expr<? extends OpsType>> thenOps = new ArrayList<>(expr.getOps().size());
				List<Expr<? extends OpsType>> elseOps = new ArrayList<>(expr.getOps().size());
				IteExpr<? extends OpsType> ite = (IteExpr<? extends OpsType>) firstIte.get();

				for (Expr<? extends OpsType> op : expr.getOps()) {
					if (op == ite) {
						thenOps.add(ite.getThen());
						elseOps.add(ite.getElse());
					} else {
						thenOps.add(op);
						elseOps.add(op);
					}
				}
				
				return manager.getExprFactory().Ite(
						ite.getCond(),
						expr.withOps(thenOps).accept(this, param),
						expr.withOps(elseOps).accept(this, param));
			}
		}

		@Override
		public <IndexType extends Type, ElemType extends Type> Expr<? extends Type> visit(
				ArrayReadExpr<IndexType, ElemType> expr, Void param) {
			throw new UnsupportedOperationException("TODO");
		}

		@Override
		public <IndexType extends Type, ElemType extends Type> Expr<? extends Type> visit(
				ArrayWriteExpr<IndexType, ElemType> expr, Void param) {
			throw new UnsupportedOperationException("TODO");
		}

		@Override
		public <ParamType extends Type, ResultType extends Type> Expr<? extends Type> visit(
				FuncLitExpr<ParamType, ResultType> expr, Void param) {
			throw new UnsupportedOperationException("TODO");
		}

		@Override
		public <ParamType extends Type, ResultType extends Type> Expr<? extends Type> visit(
				FuncAppExpr<ParamType, ResultType> expr, Void param) {
			throw new UnsupportedOperationException("TODO");
		}

		@Override
		public <ExprType extends Type> Expr<? extends Type> visit(IteExpr<ExprType> expr, Void param) {
			return expr;
		}

	}

	/**
	 * Helper visitor 3: Replace if-then-else expressions with boolean connectives.
	 * For example: (if A then B else C) <=> (!A or B) and (A or C).
	 * 
	 * It is assumed that ite expressions are propagated as high as possible.
	 */
	protected static class RemoveITEVisitor extends ArityBasedExprVisitor<Void, Expr<? extends Type>> {
		
		private ConstraintManager manager;
		
		public RemoveITEVisitor(ConstraintManager manager) {
			this.manager = manager;
		}

		@Override
		protected <ExprType extends Type> Expr<? extends Type> visitNullary(NullaryExpr<ExprType> expr, Void param) {
			return expr;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected <OpType extends Type, ExprType extends Type> Expr<? extends Type> visitUnary(
				UnaryExpr<OpType, ExprType> expr, Void param) {
			return expr.withOp((Expr<? extends OpType>) expr.getOp().accept(this, param));
		}

		@SuppressWarnings("unchecked")
		@Override
		protected <LeftOpType extends Type, RightOpType extends Type, ExprType extends Type> Expr<? extends Type> visitBinary(
				BinaryExpr<LeftOpType, RightOpType, ExprType> expr, Void param) {
			return expr.withOps(
					(Expr<? extends LeftOpType>)expr.getLeftOp().accept(this, param),
					(Expr<? extends RightOpType>)expr.getRightOp().accept(this, param));
		}

		@SuppressWarnings("unchecked")
		@Override
		protected <OpsType extends Type, ExprType extends Type> Expr<? extends Type> visitMultiary(
				MultiaryExpr<OpsType, ExprType> expr, Void param) {
			List<Expr<? extends OpsType>> ops = new ArrayList<>(expr.getOps().size());
			for (Expr<? extends OpsType> op : expr.getOps())
				ops.add((Expr<? extends OpsType>) op.accept(this, param));
			return expr.withOps(ops);
		}

		@Override
		public <IndexType extends Type, ElemType extends Type> Expr<? extends Type> visit(
				ArrayReadExpr<IndexType, ElemType> expr, Void param) {
			throw new UnsupportedOperationException("TODO");
		}

		@Override
		public <IndexType extends Type, ElemType extends Type> Expr<? extends Type> visit(
				ArrayWriteExpr<IndexType, ElemType> expr, Void param) {
			throw new UnsupportedOperationException("TODO");
		}

		@Override
		public <ParamType extends Type, ResultType extends Type> Expr<? extends Type> visit(
				FuncLitExpr<ParamType, ResultType> expr, Void param) {
			throw new UnsupportedOperationException("TODO");
		}

		@Override
		public <ParamType extends Type, ResultType extends Type> Expr<? extends Type> visit(
				FuncAppExpr<ParamType, ResultType> expr, Void param) {
			throw new UnsupportedOperationException("TODO");
		}

		@SuppressWarnings("unchecked")
		@Override
		public <ExprType extends Type> Expr<? extends Type> visit(IteExpr<ExprType> expr, Void param) {
			ExprFactory fact = manager.getExprFactory();
			// Apply ite(C,T,E) <=> (!C or T) and (C or E) transformation
			Expr<? extends BoolType> cond = (Expr<? extends BoolType>)expr.getCond().accept(this, param);
			Expr<? extends Type> then = expr.getThen().accept(this, param);
			Expr<? extends Type> elze = expr.getElse().accept(this, param);
			return fact.And(ImmutableSet.of(
					fact.Or(ImmutableSet.of(fact.Not(cond), (Expr<? extends BoolType>)then)),
					fact.Or(ImmutableSet.of(cond, (Expr<? extends BoolType>)elze))
					));
		}

	}
}