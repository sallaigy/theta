package hu.bme.mit.theta.analysis.algorithm;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.TransferFunction;

public final class ArgBuilder<S extends State, A extends Action, P extends Prec> {

	private final LTS<? super S, ? extends A> lts;
	private final Analysis<S, ? super A, ? super P> analysis;
	private final Predicate<? super S> target;

	private ArgBuilder(final LTS<? super S, ? extends A> lts, final Analysis<S, ? super A, ? super P> analysis,
			final Predicate<? super S> target) {
		this.lts = checkNotNull(lts);
		this.analysis = checkNotNull(analysis);
		this.target = checkNotNull(target);
	}

	public static <S extends State, A extends Action, P extends Prec> ArgBuilder<S, A, P> create(
			final LTS<? super S, ? extends A> lts, final Analysis<S, ? super A, ? super P> analysis,
			final Predicate<? super S> target) {
		return new ArgBuilder<>(lts, analysis, target);
	}

	public ARG<S, A> createArg() {
		return ARG.create(analysis.getDomain());
	}

	public Collection<ArgNode<S, A>> init(final ARG<S, A> arg, final P prec) {
		checkNotNull(arg);
		checkNotNull(prec);

		final Collection<ArgNode<S, A>> newInitNodes = new ArrayList<>();

		final Collection<? extends S> initStates = analysis.getInitFunction().getInitStates(prec);
		for (final S initState : initStates) {
			if (arg.getInitStates().noneMatch(s -> analysis.getDomain().isLeq(initState, s))) {
				final boolean isTarget = target.test(initState);
				final ArgNode<S, A> newNode = arg.createInitNode(initState, isTarget);
				newInitNodes.add(newNode);
			}
		}
		arg.initialized = true;

		return newInitNodes;
	}

	public Collection<ArgNode<S, A>> expand(final ArgNode<S, A> node, final P prec) {
		checkNotNull(node);
		checkNotNull(prec);

		final Collection<ArgNode<S, A>> newSuccNodes = new ArrayList<>();
		final S state = node.getState();
		final Collection<? extends A> actions = lts.getEnabledActionsFor(state);
		final TransferFunction<S, ? super A, ? super P> transferFunc = analysis.getTransferFunction();
		for (final A action : actions) {
			final Collection<? extends S> succStates = transferFunc.getSuccStates(state, action, prec);
			for (final S succState : succStates) {
				if (node.getSuccStates().noneMatch(s -> analysis.getDomain().isLeq(succState, s))) {
					final boolean isTarget = target.test(succState);
					final ArgNode<S, A> newNode = node.arg.createSuccNode(node, action, succState, isTarget);
					newSuccNodes.add(newNode);
				}
			}
		}
		node.expanded = true;

		return newSuccNodes;
	}

	public void close(final ArgNode<S, A> node) {
		checkNotNull(node);
		if (!node.isSubsumed()) {
			final ARG<S, A> arg = node.arg;
			final Optional<ArgNode<S, A>> nodeToCoverWith = arg.getNodes().filter(n -> n.mayCover(node)).findFirst();
			nodeToCoverWith.ifPresent(node::cover);
		}
	}

}
