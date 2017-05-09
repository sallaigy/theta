package hu.bme.mit.theta.formalism.xta.dsl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import hu.bme.mit.theta.common.visualization.GraphvizWriter;
import hu.bme.mit.theta.formalism.xta.XtaProcess;
import hu.bme.mit.theta.formalism.xta.XtaSystem;
import hu.bme.mit.theta.formalism.xta.XtaVisualizer;

@RunWith(Parameterized.class)
public final class XtaDslManagerTest {

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {

				{ "/critical-4-25-50.xta" },

				{ "/csma-4.xta" },

				{ "/fddi-4.xta" },

				{ "/fischer-4-32-64.xta" },

				{ "/lynch-4-16.xta" }

		});
	}

	@Parameter(0)
	public String filepath;

	@Test
	public void test() throws FileNotFoundException, IOException {
		final InputStream inputStream = getClass().getResourceAsStream(filepath);
		final XtaSystem system = XtaDslManager.createSystem(inputStream);
		final XtaProcess process = system.getProcesses().get(0);
		System.out.println(new GraphvizWriter().writeString(XtaVisualizer.visualize(process)));
	}

}
