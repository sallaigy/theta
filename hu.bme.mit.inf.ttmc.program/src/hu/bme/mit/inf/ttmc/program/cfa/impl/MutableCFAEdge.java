package hu.bme.mit.inf.ttmc.program.cfa.impl;

import java.util.ArrayList;
import java.util.List;

import hu.bme.mit.inf.ttmc.program.cfa.CFAEdge;
import hu.bme.mit.inf.ttmc.program.cfa.CFALoc;
import hu.bme.mit.inf.ttmc.program.stmt.Stmt;

class MutableCFAEdge implements CFAEdge {

	private CFALoc source;
	private CFALoc target;
	private final List<Stmt> stmts;
	
	MutableCFAEdge(final MutableCFALoc source, final MutableCFALoc target) {
		this.source = source;
		this.target = target;
		stmts = new ArrayList<>();
	}

	////

	@Override
	public CFALoc getSource() {
		return source;
	}

	@Override
	public CFALoc getTarget() {
		return target;
	}

	@Override
	public List<Stmt> getStmts() {
		return stmts;
	}

	////

}
