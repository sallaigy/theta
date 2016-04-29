package hu.bme.mit.inf.ttmc.formalism.common.decl.impl;

import static hu.bme.mit.inf.ttmc.core.type.impl.Types.Rat;

import hu.bme.mit.inf.ttmc.core.decl.impl.AbstractDecl;
import hu.bme.mit.inf.ttmc.core.type.RatType;
import hu.bme.mit.inf.ttmc.core.utils.DeclVisitor;
import hu.bme.mit.inf.ttmc.formalism.common.decl.ClockDecl;

class ClockDeclImpl extends AbstractDecl<RatType> implements ClockDecl {

	private static final int HASH_SEED = 8053;
	private static final String DECL_LABEL = "Clock";

	ClockDeclImpl(final String name) {
		super(name, Rat());
	}

	@Override
	public <P, R> R accept(final DeclVisitor<? super P, ? extends R> visitor, final P param) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO: auto-generated method stub");
	}

	@Override
	protected int getHashSeed() {
		return HASH_SEED;
	}

	@Override
	protected String getDeclLabel() {
		return DECL_LABEL;
	}

}
