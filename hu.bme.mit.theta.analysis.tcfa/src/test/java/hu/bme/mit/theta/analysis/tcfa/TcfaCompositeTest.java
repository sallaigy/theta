package hu.bme.mit.theta.analysis.tcfa;

import static hu.bme.mit.theta.core.expr.impl.Exprs.True;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Predicate;

import org.junit.Test;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgBuilder;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.WaitlistBasedAbstractor;
import hu.bme.mit.theta.analysis.expl.ExplAnalysis;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.impl.FixedPrecAnalysis;
import hu.bme.mit.theta.analysis.impl.NullPrec;
import hu.bme.mit.theta.analysis.loc.ConstLocPrec;
import hu.bme.mit.theta.analysis.loc.LocAnalysis;
import hu.bme.mit.theta.analysis.loc.LocState;
import hu.bme.mit.theta.analysis.prod.Prod2Analysis;
import hu.bme.mit.theta.analysis.prod.Prod2State;
import hu.bme.mit.theta.analysis.prod.ProdPrec;
import hu.bme.mit.theta.analysis.tcfa.zone.TcfaZoneAnalysis;
import hu.bme.mit.theta.analysis.utils.ArgVisualizer;
import hu.bme.mit.theta.analysis.waitlist.FifoWaitlist;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.common.visualization.GraphvizWriter;
import hu.bme.mit.theta.formalism.tcfa.TCFA;
import hu.bme.mit.theta.formalism.tcfa.TcfaEdge;
import hu.bme.mit.theta.formalism.tcfa.TcfaLoc;
import hu.bme.mit.theta.formalism.tcfa.instances.TcfaModels;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;

public class TcfaCompositeTest {

	@Test
	public void test() throws FileNotFoundException, IOException {
		final int n = 2;
		final TCFA fischer = TcfaModels.fischer(n, 2);

		final Solver solver = Z3SolverFactory.getInstace().createSolver();

		final TcfaLts lts = TcfaLts.create(fischer);

		final Analysis<LocState<Prod2State<ZoneState, ExplState>, TcfaLoc, TcfaEdge>, TcfaAction, NullPrec> analysis = FixedPrecAnalysis
				.create(LocAnalysis.create(fischer.getInitLoc(),
						Prod2Analysis.create(TcfaZoneAnalysis.getInstance(), ExplAnalysis.create(solver, True()))),
						ConstLocPrec.create(ProdPrec.of(ZonePrec.create(fischer.getClockVars()),
								ExplPrec.create(fischer.getDataVars()))));

		final Predicate<LocState<?, TcfaLoc, TcfaEdge>> target = s -> s.getLoc().getName().startsWith("crit, crit");

		final ArgBuilder<LocState<Prod2State<ZoneState, ExplState>, TcfaLoc, TcfaEdge>, TcfaAction, NullPrec> argBuilder = ArgBuilder
				.create(lts, analysis, target);

		final Abstractor<LocState<Prod2State<ZoneState, ExplState>, TcfaLoc, TcfaEdge>, TcfaAction, NullPrec> abstractor = WaitlistBasedAbstractor
				.create(argBuilder, LocState::getLoc, FifoWaitlist.supplier());

		final ARG<LocState<Prod2State<ZoneState, ExplState>, TcfaLoc, TcfaEdge>, TcfaAction> arg = abstractor
				.createArg();

		abstractor.check(arg, NullPrec.getInstance());

		System.out.println(new GraphvizWriter().writeString(ArgVisualizer.visualize(arg)));
	}

}
