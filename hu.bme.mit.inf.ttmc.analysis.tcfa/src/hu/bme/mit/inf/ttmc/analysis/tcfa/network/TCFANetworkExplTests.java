package hu.bme.mit.inf.ttmc.analysis.tcfa.network;

import static hu.bme.mit.inf.ttmc.core.type.impl.Types.Int;
import static hu.bme.mit.inf.ttmc.formalism.common.decl.impl.Decls2.Var;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import hu.bme.mit.inf.ttmc.analysis.algorithm.Abstractor;
import hu.bme.mit.inf.ttmc.analysis.algorithm.ArgPrinter;
import hu.bme.mit.inf.ttmc.analysis.composite.CompositeDomain;
import hu.bme.mit.inf.ttmc.analysis.composite.CompositeInitFunction;
import hu.bme.mit.inf.ttmc.analysis.composite.CompositePrecision;
import hu.bme.mit.inf.ttmc.analysis.composite.CompositeState;
import hu.bme.mit.inf.ttmc.analysis.composite.CompositeTransferFunction;
import hu.bme.mit.inf.ttmc.analysis.expl.ExplDomain;
import hu.bme.mit.inf.ttmc.analysis.expl.ExplPrecision;
import hu.bme.mit.inf.ttmc.analysis.expl.ExplState;
import hu.bme.mit.inf.ttmc.analysis.expl.GlobalExplPrecision;
import hu.bme.mit.inf.ttmc.analysis.tcfa.TCFAAction;
import hu.bme.mit.inf.ttmc.analysis.tcfa.TCFAAnalysisContext;
import hu.bme.mit.inf.ttmc.analysis.tcfa.TCFADomain;
import hu.bme.mit.inf.ttmc.analysis.tcfa.TCFAInitFunction;
import hu.bme.mit.inf.ttmc.analysis.tcfa.TCFALocTargetPredicate;
import hu.bme.mit.inf.ttmc.analysis.tcfa.TCFAState;
import hu.bme.mit.inf.ttmc.analysis.tcfa.TCFATransferFunction;
import hu.bme.mit.inf.ttmc.analysis.tcfa.expl.TCFAExplInitFunction;
import hu.bme.mit.inf.ttmc.analysis.tcfa.expl.TCFAExplTransferFunction;
import hu.bme.mit.inf.ttmc.analysis.tcfa.zone.TCFAZoneInitFunction;
import hu.bme.mit.inf.ttmc.analysis.tcfa.zone.TCFAZoneTransferFunction;
import hu.bme.mit.inf.ttmc.analysis.zone.ZoneDomain;
import hu.bme.mit.inf.ttmc.analysis.zone.ZonePrecision;
import hu.bme.mit.inf.ttmc.analysis.zone.ZoneState;
import hu.bme.mit.inf.ttmc.core.type.IntType;
import hu.bme.mit.inf.ttmc.formalism.common.decl.ClockDecl;
import hu.bme.mit.inf.ttmc.formalism.common.decl.VarDecl;
import hu.bme.mit.inf.ttmc.formalism.tcfa.TCFALoc;
import hu.bme.mit.inf.ttmc.formalism.tcfa.instances.FischerTCFA;
import hu.bme.mit.inf.ttmc.solver.Solver;
import hu.bme.mit.inf.ttmc.solver.SolverManager;
import hu.bme.mit.inf.ttmc.solver.z3.Z3SolverManager;

public class TCFANetworkExplTests {

	@Test
	public void test() {
		final int n = 6;

		final VarDecl<IntType> vlock = Var("lock", Int());

		final List<FischerTCFA> network = new ArrayList<FischerTCFA>(n);
		for (int i = 0; i < n; i++) {
			network.add(new FischerTCFA(i + 1, 1, 2, vlock));
		}

		final Collection<ClockDecl> clocks = network.stream().map(comp -> comp.getClock()).collect(toSet());
		final List<TCFALoc> initLocs = network.stream().map(comp -> comp.getInitial()).collect(toList());

		////

		final TCFAAnalysisContext context = new TCFAAnalysisContext();

		final SolverManager manager = new Z3SolverManager();
		final Solver solver = manager.createSolver(true, true);

		final TCFADomain<CompositeState<ZoneState, ExplState>> domain = new TCFADomain<>(
				new CompositeDomain<>(ZoneDomain.getInstance(), ExplDomain.getInstance()));

		final TCFAInitFunction<CompositeState<ZoneState, ExplState>, CompositePrecision<ZonePrecision, ExplPrecision>> initFunction = new TCFAInitFunction<>(
				TCFANetworkLoc.create(initLocs),
				new CompositeInitFunction<>(new TCFAZoneInitFunction(), new TCFAExplInitFunction()));

		final TCFATransferFunction<CompositeState<ZoneState, ExplState>, CompositePrecision<ZonePrecision, ExplPrecision>> transferFunction = new TCFATransferFunction<>(
				new CompositeTransferFunction<>(new TCFAZoneTransferFunction(), new TCFAExplTransferFunction(solver)));

		final TCFALocTargetPredicate targetPredicate = new TCFALocTargetPredicate(loc -> false);

		final CompositePrecision<ZonePrecision, ExplPrecision> precision = CompositePrecision.create(
				ZonePrecision.builder().addAll(clocks).build(),
				GlobalExplPrecision.create(Collections.singleton(vlock), Collections.emptySet()));

		final Abstractor<TCFAState<CompositeState<ZoneState, ExplState>>, TCFAAction, CompositePrecision<ZonePrecision, ExplPrecision>> abstractor = new Abstractor<>(
				context, domain, initFunction, transferFunction, targetPredicate);

		abstractor.init(precision);
		abstractor.check(precision);

		System.out.println(ArgPrinter.toGraphvizString(abstractor.getARG()));
	}

}
