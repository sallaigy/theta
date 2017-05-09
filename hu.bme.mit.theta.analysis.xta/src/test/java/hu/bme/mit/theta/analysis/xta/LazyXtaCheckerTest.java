package hu.bme.mit.theta.analysis.xta;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.SearchStrategy;
import hu.bme.mit.theta.analysis.unit.UnitPrec;
import hu.bme.mit.theta.analysis.xta.algorithm.lazy.BinItpStrategy;
import hu.bme.mit.theta.analysis.xta.algorithm.lazy.ItpStrategy.ItpOperator;
import hu.bme.mit.theta.analysis.xta.algorithm.lazy.LazyXtaChecker;
import hu.bme.mit.theta.analysis.xta.algorithm.lazy.LuStrategy;
import hu.bme.mit.theta.analysis.xta.algorithm.lazy.SeqItpStrategy;
import hu.bme.mit.theta.analysis.zone.itp.ItpZoneState;
import hu.bme.mit.theta.formalism.xta.XtaSystem;
import hu.bme.mit.theta.formalism.xta.dsl.XtaDslManager;

@RunWith(Parameterized.class)
public final class LazyXtaCheckerTest {

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {

				{ "/critical-2-25-50.xta" },

				{ "/csma-2.xta" },

				{ "/fddi-2.xta" },

				{ "/fischer-2-32-64.xta" },

				{ "/lynch-2-16.xta" }

		});
	}

	@Parameter(0)
	public String filepath;

	private XtaSystem system;

	@Before
	public void initialize() throws FileNotFoundException, IOException {
		final InputStream inputStream = getClass().getResourceAsStream(filepath);
		system = XtaDslManager.createSystem(inputStream);
	}

	@Test
	public void testBinItpStrategy() {
		// Arrange
		final LazyXtaChecker<ItpZoneState> checker = LazyXtaChecker.create(system,
				BinItpStrategy.create(system, ItpOperator.DEFAULT), SearchStrategy.breadthFirst(), l -> false);

		// Act
		final SafetyResult<?, XtaAction> status = checker.check(UnitPrec.getInstance());

		// Assert
		assertTrue(status.isSafe());
		System.out.println(status.getStats().get());
	}

	@Test
	public void testSeqItpStrategy() {
		// Arrange
		final LazyXtaChecker<ItpZoneState> checker = LazyXtaChecker.create(system,
				SeqItpStrategy.create(system, ItpOperator.DEFAULT), SearchStrategy.breadthFirst(), l -> false);

		// Act
		final SafetyResult<?, XtaAction> status = checker.check(UnitPrec.getInstance());

		// Assert
		assertTrue(status.isSafe());
		System.out.println(status.getStats().get());
	}

	@Test
	public void testLuStrategy() {
		// Arrange
		final LazyXtaChecker<?> checker = LazyXtaChecker.create(system, LuStrategy.create(system),
				SearchStrategy.breadthFirst(), l -> false);

		// Act
		final SafetyResult<?, XtaAction> status = checker.check(UnitPrec.getInstance());

		// Assert
		assertTrue(status.isSafe());
		System.out.println(status.getStats().get());
	}

}
