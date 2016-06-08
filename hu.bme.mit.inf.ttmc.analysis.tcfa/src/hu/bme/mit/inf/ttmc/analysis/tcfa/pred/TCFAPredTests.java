package hu.bme.mit.inf.ttmc.analysis.tcfa.pred;

import static hu.bme.mit.inf.ttmc.core.expr.impl.Exprs.Eq;
import static hu.bme.mit.inf.ttmc.core.expr.impl.Exprs.Int;
import static hu.bme.mit.inf.ttmc.core.type.impl.Types.Int;
import static hu.bme.mit.inf.ttmc.formalism.common.decl.impl.Decls2.Var;

import java.util.Collections;
import java.util.Iterator;

import org.junit.Test;

import hu.bme.mit.inf.ttmc.analysis.algorithm.Abstractor;
import hu.bme.mit.inf.ttmc.analysis.algorithm.ArgPrinter;
import hu.bme.mit.inf.ttmc.analysis.algorithm.NullLabeling;
import hu.bme.mit.inf.ttmc.analysis.pred.GlobalPredPrecision;
import hu.bme.mit.inf.ttmc.analysis.pred.PredDomain;
import hu.bme.mit.inf.ttmc.analysis.pred.PredPrecision;
import hu.bme.mit.inf.ttmc.analysis.pred.PredState;
import hu.bme.mit.inf.ttmc.analysis.tcfa.TCFAAnalysisContext;
import hu.bme.mit.inf.ttmc.analysis.tcfa.TCFADomain;
import hu.bme.mit.inf.ttmc.analysis.tcfa.TCFAInitFunction;
import hu.bme.mit.inf.ttmc.analysis.tcfa.TCFAState;
import hu.bme.mit.inf.ttmc.analysis.tcfa.TCFATargetPredicate;
import hu.bme.mit.inf.ttmc.analysis.tcfa.TCFATrans;
import hu.bme.mit.inf.ttmc.analysis.tcfa.TCFATransferFunction;
import hu.bme.mit.inf.ttmc.core.type.IntType;
import hu.bme.mit.inf.ttmc.formalism.common.decl.VarDecl;
import hu.bme.mit.inf.ttmc.formalism.tcfa.TCFA;
import hu.bme.mit.inf.ttmc.formalism.tcfa.TCFAInstances;
import hu.bme.mit.inf.ttmc.formalism.tcfa.TCFALoc;
import hu.bme.mit.inf.ttmc.solver.Solver;
import hu.bme.mit.inf.ttmc.solver.SolverManager;
import hu.bme.mit.inf.ttmc.solver.z3.Z3SolverManager;

public class TCFAPredTests {

	@Test
	public void test() {
		final VarDecl<IntType> vlock = Var("lock", Int());
		final TCFA tcfa = TCFAInstances.fischer(1, 1, 2, vlock);

		final Iterator<? extends TCFALoc> iterator = tcfa.getLocs().iterator();
		iterator.next();
		iterator.next();
		iterator.next();
		final TCFALoc targetLoc = iterator.next();

		final TCFAAnalysisContext context = new TCFAAnalysisContext(tcfa.getInitLoc(), targetLoc);

		final SolverManager manager = new Z3SolverManager();
		final Solver solver = manager.createSolver(true, true);

		final TCFADomain<PredState> domain = new TCFADomain<>(PredDomain.create(solver));
		final TCFAInitFunction<PredState, PredPrecision> initFunction = new TCFAInitFunction<>(
				new TCFAPredInitFunction());
		final TCFATransferFunction<PredState, PredPrecision> transferFunction = new TCFATransferFunction<>(
				new TCFAPredTransferFunction(solver));
		final TCFATargetPredicate targetPredicate = new TCFATargetPredicate();

		final PredPrecision precision = GlobalPredPrecision.create(Collections.singleton(Eq(vlock.getRef(), Int(0))));

		final Abstractor<TCFAState<PredState>, PredPrecision, Void, Void, TCFALoc, TCFATrans, TCFALoc> abstractor = new Abstractor<>(
				context, NullLabeling.getInstance(), domain, initFunction, transferFunction, targetPredicate);

		abstractor.init(precision);
		abstractor.check(precision);

		System.out.println(ArgPrinter.toGraphvizString(abstractor.getARG()));

	}

}
