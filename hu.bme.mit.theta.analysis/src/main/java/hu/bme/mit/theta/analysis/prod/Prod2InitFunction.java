package hu.bme.mit.theta.analysis.prod;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import hu.bme.mit.theta.analysis.InitFunction;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;

final class Prod2InitFunction<S1 extends State, S2 extends State, P1 extends Prec, P2 extends Prec>
		implements InitFunction<Prod2State<S1, S2>, Prod2Prec<P1, P2>> {

	private final InitFunction<S1, P1> initFunction1;
	private final InitFunction<S2, P2> initFunction2;
	private final StrengtheningOperator<S1, S2, P1, P2> strenghteningOperator;

	private Prod2InitFunction(final InitFunction<S1, P1> initFunction1, final InitFunction<S2, P2> initFunction2,
			final StrengtheningOperator<S1, S2, P1, P2> strenghteningOperator) {
		this.initFunction1 = checkNotNull(initFunction1);
		this.initFunction2 = checkNotNull(initFunction2);
		this.strenghteningOperator = checkNotNull(strenghteningOperator);
	}

	public static <S1 extends State, S2 extends State, P1 extends Prec, P2 extends Prec> Prod2InitFunction<S1, S2, P1, P2> create(
			final InitFunction<S1, P1> initFunction1, final InitFunction<S2, P2> initFunction2,
			final StrengtheningOperator<S1, S2, P1, P2> strenghteningOperator) {
		return new Prod2InitFunction<>(initFunction1, initFunction2, strenghteningOperator);
	}

	public static <S1 extends State, S2 extends State, P1 extends Prec, P2 extends Prec> Prod2InitFunction<S1, S2, P1, P2> create(
			final InitFunction<S1, P1> initFunction1, final InitFunction<S2, P2> initFunction2) {
		return new Prod2InitFunction<>(initFunction1, initFunction2, (states, prec) -> states);
	}

	@Override
	public Collection<? extends Prod2State<S1, S2>> getInitStates(final Prod2Prec<P1, P2> prec) {
		checkNotNull(prec);

		final Collection<? extends S1> initStates1 = initFunction1.getInitStates(prec._1());
		final Collection<? extends S2> initStates2 = initFunction2.getInitStates(prec._2());
		final Collection<Prod2State<S1, S2>> compositeIniStates = ProdState.product(initStates1, initStates2);
		return strenghteningOperator.strengthen(compositeIniStates, prec);
	}

}
