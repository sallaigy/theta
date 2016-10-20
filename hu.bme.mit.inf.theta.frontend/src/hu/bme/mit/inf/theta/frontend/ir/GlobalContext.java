package hu.bme.mit.inf.theta.frontend.ir;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import hu.bme.mit.inf.theta.core.decl.Decl;
import hu.bme.mit.inf.theta.core.expr.Expr;
import hu.bme.mit.inf.theta.core.type.Type;
import hu.bme.mit.inf.theta.formalism.common.decl.ProcDecl;
import hu.bme.mit.inf.theta.formalism.common.decl.VarDecl;
import hu.bme.mit.inf.theta.frontend.ir.node.AssignNode;
import hu.bme.mit.inf.theta.frontend.ir.node.NodeFactory;
import hu.bme.mit.inf.theta.frontend.ir.utils.SymbolTable;

public class GlobalContext {

	public static class GlobalVariable<T extends Type> {
		private final VarDecl<T> decl;
		private final Expr<? extends T> init;

		public GlobalVariable(VarDecl<T> decl, Expr<? extends T> init) {
			this.decl = decl;
			this.init = init;
		}

		public VarDecl<T> getDecl() {
			return this.decl;
		}

		public Expr<? extends T> getInit() {
			return this.init;
		}

		public AssignNode<T, ? extends T> getAssignment() {
			return NodeFactory.Assign(this.decl, init);
		}
	}

	private final SymbolTable<Decl<? extends Type, ?>> symbols = new SymbolTable<>();

	private final Map<String, Function> functions = new HashMap<>();
	private final List<GlobalVariable<?>> globals = new ArrayList<>();

	public void addFunction(Function func, ProcDecl<? extends Type> proc) {
		func.setContext(this);
		this.functions.put(func.getName(), func);
		this.symbols.put(func.getName(), proc);
	}

	public void addFunctionDeclaration(String name, ProcDecl<? extends Type> proc) {
		this.symbols.put(name, proc);
	}

	public SymbolTable<Decl<? extends Type, ?>> getSymbolTable() {
		return this.symbols;
	}

	public Function getFunctionByName(String name) {
		Function func = this.functions.get(name);
		if (func == null)
			throw new RuntimeException("Unknown function: " + name);

		return func;
	}

	public Collection<Function> functions() {
		return Collections.unmodifiableCollection(functions.values());
	}

	public <T extends Type, E extends T> void addGlobal(String name, VarDecl<T> var, Expr<E> init) {
		this.symbols.putGlobal(name, var);
		this.globals.add(new GlobalVariable<>(var, init));
	}

	public List<GlobalVariable<?>> globals() {
		return Collections.unmodifiableList(this.globals);
	}

	public Function getEntryPoint() {
		Function main = this.functions.get("main");
		if (main == null)
			throw new RuntimeException("This context contains no entry point");

		return main;
	}

	public boolean hasFunctionDefinition(String name) {
		return this.functions.containsKey(name);
	}

	public Function getFunctionByProc(ProcDecl<?> callerProc) {
		return this.functions.get(callerProc.getName());
	}

}
