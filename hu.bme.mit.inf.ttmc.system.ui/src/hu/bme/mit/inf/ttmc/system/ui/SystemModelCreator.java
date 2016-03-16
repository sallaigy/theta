package hu.bme.mit.inf.ttmc.system.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;

import com.google.common.collect.ImmutableList;

import hu.bme.mit.inf.ttmc.constraint.expr.Expr;
import hu.bme.mit.inf.ttmc.constraint.model.Expression;
import hu.bme.mit.inf.ttmc.constraint.type.BoolType;
import hu.bme.mit.inf.ttmc.constraint.type.Type;
import hu.bme.mit.inf.ttmc.constraint.ui.TypeHelper;
import hu.bme.mit.inf.ttmc.constraint.utils.impl.ExprUtils;
import hu.bme.mit.inf.ttmc.formalism.common.decl.VarDecl;
import hu.bme.mit.inf.ttmc.formalism.sts.STS;
import hu.bme.mit.inf.ttmc.formalism.sts.STSManager;
import hu.bme.mit.inf.ttmc.formalism.sts.impl.STSImpl;
import hu.bme.mit.inf.ttmc.system.model.GloballyExpression;
import hu.bme.mit.inf.ttmc.system.model.InitialConstraintDefinition;
import hu.bme.mit.inf.ttmc.system.model.InvariantConstraintDefinition;
import hu.bme.mit.inf.ttmc.system.model.PropertyDeclaration;
import hu.bme.mit.inf.ttmc.system.model.SystemConstraintDefinition;
import hu.bme.mit.inf.ttmc.system.model.SystemDefinition;
import hu.bme.mit.inf.ttmc.system.model.SystemSpecification;
import hu.bme.mit.inf.ttmc.system.model.TransitionConstraintDefinition;
import hu.bme.mit.inf.ttmc.system.model.VariableDeclaration;

public class SystemModelCreator {

	public static SystemModel create(final STSManager manager, final SystemSpecification specification) {
		checkNotNull(manager);
		checkNotNull(specification);

		final Collection<STS> stss = new ArrayList<>();

		final TypeHelper typeHelper = new TypeHelper(manager.getTypeFactory());
		final SystemDeclarationHelper declarationHelper = new SystemDeclarationHelper(manager.getDeclFactory(),
				typeHelper);
		final SystemExpressionHelper expressionHelper = new SystemExpressionHelper(manager,
				declarationHelper);

		for (PropertyDeclaration propertyDeclaration : specification.getPropertyDeclarations()) {
			final SystemDefinition systemDefinition = (SystemDefinition) propertyDeclaration.getSystem();
			final STS sts = createSTS(manager, systemDefinition, propertyDeclaration.getExpression(), declarationHelper,
					expressionHelper);
			stss.add(sts);
		}

		return new SystemModelImpl(stss);
	}

	private static STS createSTS(final STSManager manager, final SystemDefinition systemDefinition, final Expression prop,
			final SystemDeclarationHelper declarationHelper, final SystemExpressionHelper expressionHelper) {
		final STSImpl.Builder builder = new STSImpl.Builder();
		if (prop instanceof GloballyExpression) {
			builder.setProp(ExprUtils.cast(manager.getTypeInferrer(),
					expressionHelper.toExpr(((GloballyExpression) prop).getOperand()), BoolType.class));
		} else {
			throw new UnsupportedOperationException("Currently only expressions in the form of"
					+ " G(expr) are supported, where 'expr' contains no temporal operators.");
		}

		for (final VariableDeclaration variableDeclaration : systemDefinition.getVariableDeclarations()) {
			final VarDecl<Type> varDecl = (VarDecl<Type>) declarationHelper.toDecl(variableDeclaration);
			builder.addVar(varDecl);
		}

		for (SystemConstraintDefinition constraintDef : systemDefinition.getSystemConstraintDefinitions()) {
			final Expr<? extends BoolType> expr = ExprUtils.cast(manager.getTypeInferrer(),
					expressionHelper.toExpr(constraintDef.getExpression()), BoolType.class);
			if (constraintDef instanceof InitialConstraintDefinition)
				builder.addInit(expr);
			if (constraintDef instanceof InvariantConstraintDefinition)
				builder.addInvar(expr);
			if (constraintDef instanceof TransitionConstraintDefinition)
				builder.addTrans(expr);
		}

		return builder.build();
	}

	private static class SystemModelImpl implements SystemModel {

		private final Collection<STS> stss;

		private SystemModelImpl(Collection<STS> stss) {
			this.stss = ImmutableList.copyOf(checkNotNull(stss));
		}

		@Override
		public Collection<STS> getSTSs() {
			return stss;
		}
	}

}