package hu.bme.mit.inf.ttmc.formalism.utils.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableSet;

import hu.bme.mit.inf.ttmc.constraint.ConstraintManager;
import hu.bme.mit.inf.ttmc.constraint.expr.AddExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.AndExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.ArrayReadExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.ArrayWriteExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.ConstRefExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.EqExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.ExistsExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.Expr;
import hu.bme.mit.inf.ttmc.constraint.expr.FalseExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.ForallExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.FuncAppExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.FuncLitExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.GeqExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.GtExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.IffExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.ImplyExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.IntDivExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.IntLitExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.IteExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.LeqExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.LtExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.ModExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.MulExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.NegExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.NeqExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.NotExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.OrExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.ParamRefExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.RatDivExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.RatLitExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.RemExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.SubExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.TrueExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.TupleLitExpr;
import hu.bme.mit.inf.ttmc.constraint.expr.TupleProjExpr;
import hu.bme.mit.inf.ttmc.constraint.factory.ExprFactory;
import hu.bme.mit.inf.ttmc.constraint.type.BoolType;
import hu.bme.mit.inf.ttmc.constraint.type.Type;
import hu.bme.mit.inf.ttmc.constraint.type.closure.ClosedUnderAdd;
import hu.bme.mit.inf.ttmc.constraint.type.closure.ClosedUnderMul;
import hu.bme.mit.inf.ttmc.constraint.type.closure.ClosedUnderNeg;
import hu.bme.mit.inf.ttmc.constraint.type.closure.ClosedUnderSub;
import hu.bme.mit.inf.ttmc.formalism.common.decl.VarDecl;
import hu.bme.mit.inf.ttmc.formalism.common.expr.PrimedExpr;
import hu.bme.mit.inf.ttmc.formalism.common.expr.ProcCallExpr;
import hu.bme.mit.inf.ttmc.formalism.common.expr.ProcRefExpr;
import hu.bme.mit.inf.ttmc.formalism.common.expr.VarRefExpr;
import hu.bme.mit.inf.ttmc.formalism.common.factory.VarFactory;
import hu.bme.mit.inf.ttmc.formalism.utils.FormalismExprVisitor;

public class CNFTransformation {
	private final String CNFPREFIX = "__CNF";
	private final ConstraintManager manager;
	private final CNFTransformationVisitor cnfTransfVisitor;
	
	public CNFTransformation(ConstraintManager manager, VarFactory varFactory) {
		this.manager = manager;
		cnfTransfVisitor = new CNFTransformationVisitor(manager, varFactory);
	}
	
	public Expr<? extends BoolType> transform(Expr<? extends BoolType> expr) {
		Collection<Expr<? extends BoolType>> encoding = new ArrayList<>();
		expr.accept(cnfTransfVisitor, encoding);
		return manager.getExprFactory().And(encoding);
	}
	
	public Collection<VarDecl<? extends BoolType>> getRepresentatives() {
		return cnfTransfVisitor.getReps();
	}
	
	
	private final class CNFTransformationVisitor implements FormalismExprVisitor<Collection<Expr<? extends BoolType>>, Expr<? extends BoolType>> {
		private int nextCNFVarId;
		private Map<Expr<?>, VarDecl<? extends BoolType>> representatives;
		private ConstraintManager manager;
		private ExprFactory ef;
		private VarFactory vf;
		
		public CNFTransformationVisitor(ConstraintManager manager, VarFactory varFactory) {
			this.manager = manager;
			vf = varFactory;
			ef = manager.getExprFactory();
			nextCNFVarId = 0;
			representatives = new HashMap<>();
		}
		
		public Collection<VarDecl<? extends BoolType>> getReps() {
			return representatives.values();
		}
		
		private Expr<? extends BoolType> getRep(Expr<?> expr) {
			VarDecl<BoolType> rep = vf.Var(CNFPREFIX + (nextCNFVarId++), manager.getTypeFactory().Bool());
			representatives.put(expr, rep);
			return vf.Ref(rep);
		}
		
		@SuppressWarnings("unchecked")
		private Expr<? extends BoolType> visitNonBoolConn(Expr<? extends Type> expr) {
			return (Expr<? extends BoolType>) expr;
		}
		
		@Override
		public <DeclType extends Type> Expr<? extends BoolType> visit(ConstRefExpr<DeclType> expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public <DeclType extends Type> Expr<? extends BoolType> visit(ParamRefExpr<DeclType> expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(FalseExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(TrueExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(NotExpr expr, Collection<Expr<? extends BoolType>> param) {
			if (representatives.containsKey(expr)) return vf.Ref(representatives.get(expr));
			Expr<? extends BoolType> rep = getRep(expr);
			Expr<? extends BoolType> op = expr.getOp().accept(this, param);
			param.add(
					ef.And(ImmutableSet.of(
						ef.Or(ImmutableSet.of(ef.Not(rep), ef.Not(op))),
						ef.Or(ImmutableSet.of(rep,op))
					)));
			return rep;
		}
		@Override
		public Expr<? extends BoolType> visit(ImplyExpr expr, Collection<Expr<? extends BoolType>> param) {
			if (representatives.containsKey(expr)) return vf.Ref(representatives.get(expr));
			Expr<? extends BoolType> rep = getRep(expr);
			Expr<? extends BoolType> op1 = expr.getLeftOp().accept(this, param);
			Expr<? extends BoolType> op2 = expr.getRightOp().accept(this, param);
			param.add(
					ef.And(ImmutableSet.of(
						ef.Or(ImmutableSet.of(ef.Not(rep), ef.Not(op1), op2)),
						ef.Or(ImmutableSet.of(op1, rep)),
						ef.Or(ImmutableSet.of(ef.Not(op2), rep))
					)));
			return rep;
		}
		@Override
		public Expr<? extends BoolType> visit(IffExpr expr, Collection<Expr<? extends BoolType>> param) {
			if (representatives.containsKey(expr)) return vf.Ref(representatives.get(expr));
			Expr<? extends BoolType> rep = getRep(expr);
			Expr<? extends BoolType> op1 = expr.getLeftOp().accept(this, param);
			Expr<? extends BoolType> op2 = expr.getRightOp().accept(this, param);
			param.add(
					ef.And(ImmutableSet.of(
						ef.Or(ImmutableSet.of(ef.Not(rep), ef.Not(op1), op2)),
						ef.Or(ImmutableSet.of(ef.Not(rep), op1, ef.Not(op2))),
						ef.Or(ImmutableSet.of(rep, ef.Not(op1), ef.Not(op2))),
						ef.Or(ImmutableSet.of(rep, op1, op2))
					)));
			return rep;
		}
		@Override
		public Expr<? extends BoolType> visit(AndExpr expr, Collection<Expr<? extends BoolType>> param) {
			if (representatives.containsKey(expr)) return vf.Ref(representatives.get(expr));
			Expr<? extends BoolType> rep = getRep(expr);
			Collection<Expr<? extends BoolType>> ops = new ArrayList<>(expr.getOps().size());
			for (Expr<? extends BoolType> op : expr.getOps()) ops.add(op.accept(this, param));
			Collection<Expr<? extends BoolType>> lastClause = new ArrayList<>();
			lastClause.add(rep);
			Collection<Expr<? extends BoolType>> en = new ArrayList<>();
			for (Expr<? extends BoolType> op : ops) {
				en.add(ef.Or(ImmutableSet.of(ef.Not(rep), op)));
				lastClause.add(ef.Not(op));
			}
			en.add(ef.Or(lastClause));
			param.add(ef.And(en));
			return rep;
		}
		@Override
		public Expr<? extends BoolType> visit(OrExpr expr, Collection<Expr<? extends BoolType>> param) {
			if (representatives.containsKey(expr)) return vf.Ref(representatives.get(expr));
			Expr<? extends BoolType> rep = getRep(expr);
			Collection<Expr<? extends BoolType>> ops = new ArrayList<>(expr.getOps().size());
			for (Expr<? extends BoolType> op : expr.getOps()) ops.add(op.accept(this, param));
			Collection<Expr<? extends BoolType>> en = new ArrayList<>();
			for (Expr<? extends BoolType> op : ops) {
				en.add(ef.Or(ImmutableSet.of(ef.Not(op), rep)));
			}
			en.add(ef.Or(ops));
			param.add(ef.And(en));
			return rep;
		}
		@Override
		public Expr<? extends BoolType> visit(ExistsExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(ForallExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(EqExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(NeqExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(GeqExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(GtExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(LeqExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(LtExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(IntLitExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(IntDivExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(RemExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(ModExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(RatLitExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(RatDivExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public <ExprType extends ClosedUnderNeg> Expr<? extends BoolType> visit(NegExpr<ExprType> expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public <ExprType extends ClosedUnderSub> Expr<? extends BoolType> visit(SubExpr<ExprType> expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public <ExprType extends ClosedUnderAdd> Expr<? extends BoolType> visit(AddExpr<ExprType> expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public <ExprType extends ClosedUnderMul> Expr<? extends BoolType> visit(MulExpr<ExprType> expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public <IndexType extends Type, ElemType extends Type> Expr<? extends BoolType> visit(
				ArrayReadExpr<IndexType, ElemType> expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public <IndexType extends Type, ElemType extends Type> Expr<? extends BoolType> visit(
				ArrayWriteExpr<IndexType, ElemType> expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public <ParamType extends Type, ResultType extends Type> Expr<? extends BoolType> visit(
				FuncLitExpr<ParamType, ResultType> expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public <ParamType extends Type, ResultType extends Type> Expr<? extends BoolType> visit(
				FuncAppExpr<ParamType, ResultType> expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(TupleLitExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public Expr<? extends BoolType> visit(TupleProjExpr expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public <ExprType extends Type> Expr<? extends BoolType> visit(IteExpr<ExprType> expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public <ExprType extends Type> Expr<? extends BoolType> visit(PrimedExpr<ExprType> expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
		@Override
		public <DeclType extends Type> Expr<? extends BoolType> visit(VarRefExpr<DeclType> expr, Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}

		@Override
		public <ReturnType extends Type> Expr<? extends BoolType> visit(ProcCallExpr<ReturnType> expr,
				Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}

		@Override
		public <ReturnType extends Type> Expr<? extends BoolType> visit(ProcRefExpr<ReturnType> expr,
				Collection<Expr<? extends BoolType>> param) {
			return visitNonBoolConn(expr);
		}
	}
}
