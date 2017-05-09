package hu.bme.mit.theta.formalism.xta.dsl;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.antlr.v4.runtime.Token;

import hu.bme.mit.theta.common.dsl.Scope;
import hu.bme.mit.theta.common.dsl.Symbol;
import hu.bme.mit.theta.common.dsl.SymbolTable;
import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.formalism.xta.XtaProcess;
import hu.bme.mit.theta.formalism.xta.XtaSystem;
import hu.bme.mit.theta.formalism.xta.dsl.gen.XtaDslParser.ArrayIdContext;
import hu.bme.mit.theta.formalism.xta.dsl.gen.XtaDslParser.FunctionDeclContext;
import hu.bme.mit.theta.formalism.xta.dsl.gen.XtaDslParser.InstantiationContext;
import hu.bme.mit.theta.formalism.xta.dsl.gen.XtaDslParser.ProcessDeclContext;
import hu.bme.mit.theta.formalism.xta.dsl.gen.XtaDslParser.TypeContext;
import hu.bme.mit.theta.formalism.xta.dsl.gen.XtaDslParser.TypeDeclContext;
import hu.bme.mit.theta.formalism.xta.dsl.gen.XtaDslParser.VariableDeclContext;
import hu.bme.mit.theta.formalism.xta.dsl.gen.XtaDslParser.VariableIdContext;
import hu.bme.mit.theta.formalism.xta.dsl.gen.XtaDslParser.XtaContext;

public final class XtaSpecification implements Scope {
	private final SymbolTable symbolTable;

	private final List<XtaVariableSymbol> variables;
	private final List<XtaTypeSymbol> types;
	private final List<String> processIds;

	private final XtaSystem system;

	private XtaSpecification(final XtaContext context) {
		checkNotNull(context);
		symbolTable = new SymbolTable();

		variables = new ArrayList<>();
		types = new ArrayList<>();
		processIds = context.fSystem.fIds.stream().map(Token::getText).collect(toList());

		declareAllTypes(context.fTypeDecls);
		declareAllVariables(context.fVariableDecls);
		declareAllFunctions(context.fFunctionDecls);
		declareAllProcesses(context.fProcessDecls);
		declareAllInstantiations(context.fInstantiations);
		system = instantiate();
	}

	public static XtaSpecification fromContext(final XtaContext context) {
		return new XtaSpecification(context);
	}

	////

	private XtaSystem instantiate() {
		final List<XtaProcess> processes = new ArrayList<>();

		final Environment env = new Environment();

		defineAllVariables(env);
		defineAllTypes(env);

		for (final String processId : processIds) {
			final Symbol symbol = symbolTable.get(processId).get();

			if (symbol instanceof XtaProcessSymbol) {
				final XtaProcessSymbol processSymbol = (XtaProcessSymbol) symbol;
				final Set<List<Expr<?>>> argumentLists = processSymbol.getArgumentLists(env);

				for (final List<Expr<?>> argumentList : argumentLists) {
					final String name = createName(processSymbol, argumentList);
					final XtaProcess process = processSymbol.instantiate(name, argumentList, env);
					processes.add(process);
				}

			} else if (symbol instanceof XtaInstantiationSymbol) {
				throw new UnsupportedOperationException();
			} else {
				throw new AssertionError();
			}
		}

		return XtaSystem.of(processes);
	}

	private static String createName(final XtaProcessSymbol processSymbol, final List<Expr<?>> argumentList) {
		final StringBuilder sb = new StringBuilder();
		sb.append(processSymbol.getName());
		argumentList.forEach(a -> sb.append("_" + a.toString()));
		return sb.toString();
	}

	////

	private void defineAllVariables(final Environment env) {
		for (final XtaVariableSymbol variable : variables) {
			env.compute(variable, v -> v.instantiate("", env));
		}
	}

	private void defineAllTypes(final Environment env) {
		for (final XtaTypeSymbol type : types) {
			env.compute(type, v -> v.instantiate(env));
		}
	}

	////

	private void declareAllTypes(final List<TypeDeclContext> contexts) {
		contexts.forEach(this::declare);
	}

	private void declare(final TypeDeclContext context) {
		final TypeContext typeContext = context.fType;
		for (final ArrayIdContext arrayIdContext : context.fArrayIds) {
			final XtaTypeSymbol typeSymbol = new XtaTypeSymbol(this, typeContext, arrayIdContext);
			types.add(typeSymbol);
			symbolTable.add(typeSymbol);
		}
	}

	////

	private void declareAllVariables(final List<VariableDeclContext> contexts) {
		contexts.forEach(this::declare);
	}

	private void declare(final VariableDeclContext context) {
		final TypeContext typeContext = context.fType;
		for (final VariableIdContext variableIdContext : context.fVariableIds) {
			final XtaVariableSymbol variableSymbol = new XtaVariableSymbol(this, typeContext, variableIdContext);
			variables.add(variableSymbol);
			symbolTable.add(variableSymbol);
		}
	}

	////

	private void declareAllFunctions(final List<FunctionDeclContext> contexts) {
		contexts.forEach(this::declare);
	}

	private void declare(final FunctionDeclContext context) {
		final XtaFunctionSymbol functionSymbol = new XtaFunctionSymbol(context);
		symbolTable.add(functionSymbol);
	}

	////

	private void declareAllProcesses(final List<ProcessDeclContext> contexts) {
		contexts.forEach(this::declare);
	}

	private void declare(final ProcessDeclContext context) {
		final XtaProcessSymbol processSymbol = new XtaProcessSymbol(this, context);
		symbolTable.add(processSymbol);
	}

	////

	private void declareAllInstantiations(final List<InstantiationContext> contexts) {
		contexts.forEach(this::declare);
	}

	private void declare(final InstantiationContext context) {
		final XtaInstantiationSymbol processSymbol = new XtaInstantiationSymbol(context);
		symbolTable.add(processSymbol);
	}

	////

	public XtaSystem getSystem() {
		return system;
	}

	////

	@Override
	public Optional<? extends Scope> enclosingScope() {
		return Optional.empty();
	}

	@Override
	public Optional<Symbol> resolve(final String name) {
		return symbolTable.get(name);
	}

	////

}
