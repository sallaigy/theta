package hu.bme.mit.theta.formalism.xta.dsl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import hu.bme.mit.theta.common.ObjectUtils;
import hu.bme.mit.theta.common.dsl.Symbol;

final class Environment {

	private Frame currentFrame;

	public Environment() {
		this.currentFrame = new Frame(null);
	}

	public void push() {
		currentFrame = new Frame(currentFrame);
	}

	public void pop() {
		checkState(currentFrame.parent != null);
		currentFrame = currentFrame.parent;
	}

	public boolean isDefined(final Symbol symbol) {
		checkNotNull(symbol);
		return (currentFrame.eval(symbol) != null);
	}

	public void define(final Symbol symbol, final Object value) {
		checkNotNull(symbol);
		checkNotNull(value);
		currentFrame.define(symbol, value);
	}

	public Object eval(final Symbol symbol) {
		checkNotNull(symbol);
		final Object value = currentFrame.eval(symbol);
		checkArgument(symbol != null, "Symbol " + symbol.getName() + " is undefined");
		return value;
	}

	public <S extends Symbol, V extends Object> Object compute(final S symbol,
			final Function<? super S, ? extends Object> mapping) {
		checkNotNull(symbol);
		checkNotNull(mapping);
		Object value = currentFrame.eval(symbol);
		if (value == null) {
			value = mapping.apply(symbol);
			checkArgument(value != null);
			currentFrame.define(symbol, value);
		}
		return value;
	}

	private static final class Frame {
		private final Frame parent;
		private final Map<Symbol, Object> symbolToValue;

		private Frame(final Frame parent) {
			this.parent = parent;
			symbolToValue = new HashMap<>();
		}

		public void define(final Symbol symbol, final Object value) {
			checkArgument(eval(symbol) == null, "Symbol " + symbol.getName() + " is already defined");
			symbolToValue.put(symbol, value);
		}

		public Object eval(final Symbol symbol) {
			final Object value = symbolToValue.get(symbol);
			if (value != null) {
				return value;
			} else if (parent == null) {
				return null;
			} else {
				return parent.eval(symbol);
			}
		}

		@Override
		public String toString() {
			return ObjectUtils.toStringBuilder(getClass().getSimpleName()).addAll(symbolToValue.entrySet().stream()
					.map(e -> e.getKey().getName() + " <- " + e.getValue()).collect(toList())).toString();
		}
	}

}