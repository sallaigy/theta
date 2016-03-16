package hu.bme.mit.inf.ttmc.constraint.ui

import hu.bme.mit.inf.ttmc.constraint.expr.Expr
import hu.bme.mit.inf.ttmc.constraint.factory.ExprFactory
import hu.bme.mit.inf.ttmc.constraint.model.AddExpression
import hu.bme.mit.inf.ttmc.constraint.model.AndExpression
import hu.bme.mit.inf.ttmc.constraint.model.ArrayAccessExpression
import hu.bme.mit.inf.ttmc.constraint.model.ArrayWithExpression
import hu.bme.mit.inf.ttmc.constraint.model.DecimalLiteralExpression
import hu.bme.mit.inf.ttmc.constraint.model.DivExpression
import hu.bme.mit.inf.ttmc.constraint.model.DivideExpression
import hu.bme.mit.inf.ttmc.constraint.model.EqualExpression
import hu.bme.mit.inf.ttmc.constraint.model.EqualityExpression
import hu.bme.mit.inf.ttmc.constraint.model.Expression
import hu.bme.mit.inf.ttmc.constraint.model.FalseExpression
import hu.bme.mit.inf.ttmc.constraint.model.GreaterEqualExpression
import hu.bme.mit.inf.ttmc.constraint.model.GreaterExpression
import hu.bme.mit.inf.ttmc.constraint.model.IfThenElseExpression
import hu.bme.mit.inf.ttmc.constraint.model.ImplyExpression
import hu.bme.mit.inf.ttmc.constraint.model.InequalityExpression
import hu.bme.mit.inf.ttmc.constraint.model.IntegerLiteralExpression
import hu.bme.mit.inf.ttmc.constraint.model.LessEqualExpression
import hu.bme.mit.inf.ttmc.constraint.model.LessExpression
import hu.bme.mit.inf.ttmc.constraint.model.ModExpression
import hu.bme.mit.inf.ttmc.constraint.model.MultiplyExpression
import hu.bme.mit.inf.ttmc.constraint.model.NotExpression
import hu.bme.mit.inf.ttmc.constraint.model.OrExpression
import hu.bme.mit.inf.ttmc.constraint.model.RationalLiteralExpression
import hu.bme.mit.inf.ttmc.constraint.model.ReferenceExpression
import hu.bme.mit.inf.ttmc.constraint.model.SubtractExpression
import hu.bme.mit.inf.ttmc.constraint.model.TrueExpression
import hu.bme.mit.inf.ttmc.constraint.model.UnaryMinusExpression
import hu.bme.mit.inf.ttmc.constraint.type.ArrayType
import hu.bme.mit.inf.ttmc.constraint.type.BoolType
import hu.bme.mit.inf.ttmc.constraint.type.IntType
import hu.bme.mit.inf.ttmc.constraint.type.RatType
import hu.bme.mit.inf.ttmc.constraint.type.Type
import hu.bme.mit.inf.ttmc.constraint.type.closure.ClosedUnderAdd
import hu.bme.mit.inf.ttmc.constraint.type.closure.ClosedUnderMul
import hu.bme.mit.inf.ttmc.constraint.type.closure.ClosedUnderNeg
import hu.bme.mit.inf.ttmc.constraint.type.closure.ClosedUnderSub
import hu.bme.mit.inf.ttmc.constraint.utils.impl.ExprUtils
import hu.bme.mit.inf.ttmc.constraint.ConstraintManager
import hu.bme.mit.inf.ttmc.constraint.utils.TypeInferrer

public class ExpressionHelper {
	
	protected val extension ExprFactory exprFactory;
	protected val extension DeclarationHelper declarationHelper;
	
	private val TypeInferrer typeInferrer;
	
	public new(ConstraintManager manager, DeclarationHelper declarationHelper) {
		this.exprFactory = manager.getExprFactory
		this.declarationHelper = declarationHelper
		this.typeInferrer = manager.typeInferrer
	}
	
	
	/////
	public def dispatch Expr<? extends Type> toExpr(Expression expression) {
		throw new UnsupportedOperationException("Not supported: " + expression.class)
	}

	public def dispatch Expr<? extends Type> toExpr(TrueExpression expression) {
		True
	}

	public def dispatch Expr<? extends Type> toExpr(FalseExpression expression) {
		False
	}

	public def dispatch Expr<? extends Type> toExpr(IntegerLiteralExpression expression) {
		val value = expression.value.longValueExact
		Int(value)
	}

	public def dispatch Expr<? extends Type> toExpr(RationalLiteralExpression expression) {
		val num = expression.numerator.longValueExact
		val denom = expression.denominator.longValueExact
		Rat(num, denom)
	}

	public def dispatch Expr<? extends Type> toExpr(DecimalLiteralExpression expression) {
		throw new UnsupportedOperationException("ToDo")
	}

	public def dispatch Expr<? extends Type> toExpr(AddExpression expression) {
		val ops = expression.operands.map[ExprUtils.cast(typeInferrer, toExpr, ClosedUnderAdd)]
		Add(ops)
	}

	public def dispatch Expr<? extends Type> toExpr(MultiplyExpression expression) {
		val ops = expression.operands.map[ExprUtils.cast(typeInferrer, toExpr, ClosedUnderMul)]
		Mul(ops)
	}

	public def dispatch Expr<? extends Type> toExpr(UnaryMinusExpression expression) {
		val op = ExprUtils.cast(typeInferrer, expression.operand.toExpr, ClosedUnderNeg)
		Neg(op)
	}

	public def dispatch Expr<? extends Type> toExpr(SubtractExpression expression) {
		val leftOp = ExprUtils.cast(typeInferrer, expression.leftOperand.toExpr, ClosedUnderSub)
		val rightOp = ExprUtils.cast(typeInferrer, expression.rightOperand.toExpr, ClosedUnderSub)
		Sub(leftOp, rightOp)
	}

	public def dispatch Expr<? extends Type> toExpr(DivideExpression expression) {
		val leftOp = ExprUtils.cast(typeInferrer, expression.leftOperand.toExpr, RatType)
		val rightOp = ExprUtils.cast(typeInferrer, expression.rightOperand.toExpr, RatType)
		RatDiv(leftOp, rightOp)
	}

	public def dispatch Expr<? extends Type> toExpr(DivExpression expression) {
		val leftOp = ExprUtils.cast(typeInferrer, expression.leftOperand.toExpr, IntType)
		val rightOp = ExprUtils.cast(typeInferrer, expression.rightOperand.toExpr, IntType)
		IntDiv(leftOp, rightOp)
	}

	public def dispatch Expr<? extends Type> toExpr(ModExpression expression) {
		val leftOp = ExprUtils.cast(typeInferrer, expression.leftOperand.toExpr, IntType)
		val rightOp = ExprUtils.cast(typeInferrer, expression.rightOperand.toExpr, IntType)
		IntDiv(leftOp, rightOp)
	}

	public def dispatch Expr<? extends Type> toExpr(EqualityExpression expression) {
		val leftOp = expression.leftOperand.toExpr
		val rightOp = expression.rightOperand.toExpr
		Eq(leftOp, rightOp)
	}

	public def dispatch Expr<? extends Type> toExpr(InequalityExpression expression) {
		val leftOp = expression.leftOperand.toExpr
		val rightOp = expression.rightOperand.toExpr
		Neq(leftOp, rightOp)
	}

	public def dispatch Expr<? extends Type> toExpr(LessExpression expression) {
		val leftOp = ExprUtils.cast(typeInferrer, expression.leftOperand.toExpr, RatType)
		val rightOp = ExprUtils.cast(typeInferrer, expression.rightOperand.toExpr, RatType)
		Lt(leftOp, rightOp)
	}

	public def dispatch Expr<? extends Type> toExpr(LessEqualExpression expression) {
		val leftOp = ExprUtils.cast(typeInferrer, expression.leftOperand.toExpr, RatType)
		val rightOp = ExprUtils.cast(typeInferrer, expression.rightOperand.toExpr, RatType)
		Leq(leftOp, rightOp)
	}

	public def dispatch Expr<? extends Type> toExpr(GreaterExpression expression) {
		val leftOp = ExprUtils.cast(typeInferrer, expression.leftOperand.toExpr, RatType)
		val rightOp = ExprUtils.cast(typeInferrer, expression.rightOperand.toExpr, RatType)
		Gt(leftOp, rightOp)
	}

	public def dispatch Expr<? extends Type> toExpr(GreaterEqualExpression expression) {
		val leftOp = ExprUtils.cast(typeInferrer, expression.leftOperand.toExpr, RatType)
		val rightOp = ExprUtils.cast(typeInferrer, expression.rightOperand.toExpr, RatType)
		Geq(leftOp, rightOp)
	}

	public def dispatch Expr<? extends Type> toExpr(NotExpression expression) {
		val op = ExprUtils.cast(typeInferrer, expression.operand.toExpr, BoolType)
		Not(op)
	}

	public def dispatch Expr<? extends Type> toExpr(ImplyExpression expression) {
		val leftOp = ExprUtils.cast(typeInferrer, expression.leftOperand.toExpr, BoolType)
		val rightOp = ExprUtils.cast(typeInferrer, expression.rightOperand.toExpr, BoolType)
		Imply(leftOp, rightOp)
	}

	public def dispatch Expr<? extends Type> toExpr(EqualExpression expression) {
		val leftOp = ExprUtils.cast(typeInferrer, expression.leftOperand.toExpr, BoolType)
		val rightOp = ExprUtils.cast(typeInferrer, expression.rightOperand.toExpr, BoolType)
		Iff(leftOp, rightOp)
	}

	public def dispatch Expr<? extends Type> toExpr(AndExpression expression) {
		val ops = expression.operands.map[ExprUtils.cast(typeInferrer, toExpr, BoolType)]
		And(ops)
	}

	public def dispatch Expr<? extends Type> toExpr(OrExpression expression) {
		val ops = expression.operands.map[ExprUtils.cast(typeInferrer, toExpr, BoolType)]
		Or(ops)
	}

	public def dispatch Expr<? extends Type> toExpr(ArrayAccessExpression expression) {
		val array = ExprUtils.cast(typeInferrer, expression.operand.toExpr, ArrayType) as Expr<ArrayType<Type, Type>>

		val parameters = expression.parameters
		if (parameters.size == 0) {
			throw new UnsupportedOperationException
		} else if (parameters.size == 1) {
			val parameter = expression.parameters.get(0)
			val index = parameter.toExpr
			Read(array, index)
		} else {
			throw new UnsupportedOperationException
		}
	}

	public def dispatch Expr<? extends Type> toExpr(ArrayWithExpression expression) {
		val array = ExprUtils.cast(typeInferrer, expression.operand.toExpr, ArrayType) as Expr<ArrayType<Type, Type>>
		val elem = expression.value.toExpr

		val parameters = expression.parameters
		if (parameters.size == 0) {
			throw new UnsupportedOperationException
		} else if (parameters.size == 1) {
			val parameter = expression.parameters.get(0)
			val index = parameter.toExpr
			Write(array, index, elem)
		} else {
			throw new UnsupportedOperationException
		}
	}

	public def dispatch Expr<? extends Type> toExpr(IfThenElseExpression expression) {
		val cond = ExprUtils.cast(typeInferrer, expression.condition.toExpr, BoolType)
		val then = expression.then.toExpr
		val elze = expression.^else.toExpr
		Ite(cond, then, elze)
	}

	public def dispatch Expr<? extends Type> toExpr(ReferenceExpression expression) {
		val decl = expression.declaration.toDecl
		decl.ref
	}

	
}