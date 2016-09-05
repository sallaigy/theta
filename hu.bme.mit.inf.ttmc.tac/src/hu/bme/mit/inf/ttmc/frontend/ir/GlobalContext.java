package hu.bme.mit.inf.ttmc.frontend.ir;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import hu.bme.mit.inf.ttmc.core.decl.Decl;
import hu.bme.mit.inf.ttmc.core.type.Type;
import hu.bme.mit.inf.ttmc.formalism.common.decl.ProcDecl;
import hu.bme.mit.inf.ttmc.formalism.common.decl.VarDecl;
import hu.bme.mit.inf.ttmc.frontend.ir.utils.SymbolTable;

public class GlobalContext {

	private final SymbolTable<Decl<? extends Type, ?>> symbols = new SymbolTable<>();
	private final Map<String, Function> functions = new HashMap<>();
	private final Map<Function, ProcDecl<? extends Type>> procs = new HashMap<>();

	private Map<String, VarDecl<? extends Type>> globals = new HashMap<>();

	public void addFunction(Function func, ProcDecl<? extends Type> proc) {
		this.functions.put(func.getName(), func);
		this.procs.put(func, proc);
	}

	public SymbolTable<Decl<? extends Type, ?>> getSymbolTable() {
		return this.symbols;
	}

	public Collection<Function> functions() {
		return Collections.unmodifiableCollection(functions.values());
	}

}
