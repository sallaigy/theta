package hu.bme.mit.theta.frontend.aiger.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.decl.impl.Decls;
import hu.bme.mit.theta.core.expr.Expr;
import hu.bme.mit.theta.core.expr.impl.Exprs;
import hu.bme.mit.theta.core.type.BoolType;
import hu.bme.mit.theta.core.type.impl.Types;
import hu.bme.mit.theta.formalism.sts.STS;
import hu.bme.mit.theta.frontend.aiger.AigerParser;

/**
 * A simple AIGER parser that encodes each variable of the circuit with a STS
 * variable. It supports AIGER format 1.7.
 */
public class AigerParserSimple implements AigerParser {

	@Override
	public STS parse(final String fileName) throws IOException {

		final STS.Builder builder = STS.builder();
		final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));

		try {
			int maxVars, inputs, latches, outputs, andGates;
			// Parse header
			final String[] header = checkNotNull(br.readLine(), "Header expected").split(" ");
			maxVars = Integer.parseInt(header[1]);
			inputs = Integer.parseInt(header[2]);
			latches = Integer.parseInt(header[3]);
			outputs = Integer.parseInt(header[4]);
			andGates = Integer.parseInt(header[5]);
			// Create variables
			final List<VarDecl<BoolType>> vars = new ArrayList<>(maxVars + 1);
			final List<Expr<? extends BoolType>> outVars = new ArrayList<>(1);
			for (int i = 0; i <= maxVars; ++i)
				vars.add(Decls.Var("v" + i, Types.Bool()));
			// v0 is the constant 'false'
			builder.addInvar(Exprs.Not(vars.get(0).getRef()));

			// Inputs
			for (int i = 0; i < inputs; ++i)
				br.readLine();

			// Latches
			for (int i = 0; i < latches; ++i) {
				final String v[] = checkNotNull(br.readLine(), "Latch expected").split(" ");
				final int v1 = Integer.parseInt(v[0]);
				final int v2 = Integer.parseInt(v[1]);
				builder.addInit(Exprs.Not(vars.get(v1 / 2).getRef()));
				builder.addTrans(Exprs.Iff(Exprs.Prime(vars.get(v1 / 2).getRef()),
						v2 % 2 == 0 ? vars.get(v2 / 2).getRef() : Exprs.Not(vars.get(v2 / 2).getRef())));
			}

			// Outputs
			for (int i = 0; i < outputs; ++i) {
				final int v = Integer.parseInt(br.readLine());
				outVars.add(v % 2 == 0 ? vars.get(v / 2).getRef() : Exprs.Not(vars.get(v / 2).getRef()));
			}

			// And gates
			for (int i = 0; i < andGates; ++i) {
				final String[] v = checkNotNull(br.readLine(), "And gate expected").split(" ");
				final int vo = Integer.parseInt(v[0]);
				final int vi1 = Integer.parseInt(v[1]);
				final int vi2 = Integer.parseInt(v[2]);
				builder.addInvar(Exprs.Iff(vars.get(vo / 2).getRef(),
						Exprs.And(vi1 % 2 == 0 ? vars.get(vi1 / 2).getRef() : Exprs.Not(vars.get(vi1 / 2).getRef()),
								vi2 % 2 == 0 ? vars.get(vi2 / 2).getRef() : Exprs.Not(vars.get(vi2 / 2).getRef()))));
			}

			br.close();

			// Requirement
			if (outVars.size() == 1) {
				builder.setProp(Exprs.Not(outVars.get(0)));
			} else {
				throw new UnsupportedOperationException(
						"Currently only models with a single output variable are supported (this model has "
								+ outVars.size() + ").");
			}

			return builder.build();
		} finally {
			br.close();
		}
	}
}
