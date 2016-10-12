package hu.bme.mit.theta.analysis.tcfa.zone;

import static hu.bme.mit.theta.core.decl.impl.Decls.Var;
import static hu.bme.mit.theta.core.type.impl.Types.Int;

import java.util.Collections;

import org.junit.Test;

import hu.bme.mit.theta.analysis.algorithm.LifoWaitlist;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.WaitlistBasedAbstractor;
import hu.bme.mit.theta.analysis.automaton.AutomatonPrecision;
import hu.bme.mit.theta.analysis.automaton.AutomatonState;
import hu.bme.mit.theta.analysis.tcfa.TcfaAction;
import hu.bme.mit.theta.analysis.tcfa.TcfaAnalyis;
import hu.bme.mit.theta.analysis.utils.ArgVisualizer;
import hu.bme.mit.theta.analysis.zone.ZonePrecision;
import hu.bme.mit.theta.analysis.zone.ZoneState;
import hu.bme.mit.theta.common.visualization.GraphvizWriter;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.IntType;
import hu.bme.mit.theta.formalism.tcfa.TcfaEdge;
import hu.bme.mit.theta.formalism.tcfa.TcfaLoc;
import hu.bme.mit.theta.formalism.tcfa.instances.FischerTcfa;

public class TcfaZoneTest {

	@Test
	public void test() {
		final VarDecl<IntType> vlock = Var("lock", Int());
		final FischerTcfa fischer = new FischerTcfa(1, 1, 2, vlock);

		final TcfaAnalyis<ZoneState, ZonePrecision> analyis = TcfaAnalyis.create(fischer.getInitial(),
				TcfaZoneAnalysis.getInstance());

		final ZonePrecision subPrecision = ZonePrecision.create(Collections.singleton(fischer.getClock()));
		final AutomatonPrecision<ZonePrecision, TcfaLoc, TcfaEdge> precision = AutomatonPrecision
				.create(l -> subPrecision);

		final Abstractor<AutomatonState<ZoneState, TcfaLoc, TcfaEdge>, TcfaAction, AutomatonPrecision<ZonePrecision, TcfaLoc, TcfaEdge>> abstractor = new WaitlistBasedAbstractor<>(
				analyis, s -> s.getLoc().equals(fischer.getCritical()), new LifoWaitlist<>());

		abstractor.init(precision);
		abstractor.check(precision);

		System.out.println(new GraphvizWriter().writeString(ArgVisualizer.visualize(abstractor.getARG())));
	}

}
