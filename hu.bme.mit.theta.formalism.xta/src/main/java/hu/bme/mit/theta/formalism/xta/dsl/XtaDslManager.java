package hu.bme.mit.theta.formalism.xta.dsl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import hu.bme.mit.theta.formalism.xta.XtaSystem;
import hu.bme.mit.theta.formalism.xta.dsl.gen.XtaDslLexer;
import hu.bme.mit.theta.formalism.xta.dsl.gen.XtaDslParser;
import hu.bme.mit.theta.formalism.xta.dsl.gen.XtaDslParser.XtaContext;

public final class XtaDslManager {

	private XtaDslManager() {
	}

	public static XtaSystem createSystem(final InputStream inputStream) throws FileNotFoundException, IOException {
		final ANTLRInputStream input = new ANTLRInputStream(inputStream);

		final XtaDslLexer lexer = new XtaDslLexer(input);
		final CommonTokenStream tokens = new CommonTokenStream(lexer);
		final XtaDslParser parser = new XtaDslParser(tokens);

		final XtaContext context = parser.xta();
		final XtaSpecification scope = XtaSpecification.fromContext(context);
		final XtaSystem system = scope.getSystem();

		return system;
	}
}
