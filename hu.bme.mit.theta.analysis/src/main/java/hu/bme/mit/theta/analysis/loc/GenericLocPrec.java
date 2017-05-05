package hu.bme.mit.theta.analysis.loc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.common.ObjectUtils;
import hu.bme.mit.theta.common.ToStringBuilder;
import hu.bme.mit.theta.formalism.common.Edge;
import hu.bme.mit.theta.formalism.common.Loc;

/**
 * Represents an immutable generic precision that can assign a precision to each
 * location.
 */
public final class GenericLocPrec<P extends Prec, L extends Loc<L, E>, E extends Edge<L, E>>
		implements LocPrec<P, L, E> {
	private final Map<L, P> mapping;
	private final Optional<P> defaultPrec;

	private GenericLocPrec(final Map<L, P> mapping, final Optional<P> defaultPrec) {
		this.mapping = mapping;
		this.defaultPrec = defaultPrec;
	}

	public static <P extends Prec, L extends Loc<L, E>, E extends Edge<L, E>> GenericLocPrec<P, L, E> create(
			final Map<L, P> mapping) {
		return new GenericLocPrec<>(ImmutableMap.copyOf(mapping), Optional.empty());
	}

	public static <P extends Prec, L extends Loc<L, E>, E extends Edge<L, E>> GenericLocPrec<P, L, E> create(
			final P defaultPrec) {
		return new GenericLocPrec<P, L, E>(Collections.emptyMap(), Optional.of(defaultPrec));
	}

	public static <P extends Prec, L extends Loc<L, E>, E extends Edge<L, E>> GenericLocPrec<P, L, E> create(
			final Map<L, P> mapping, final P defaultPrec) {
		return new GenericLocPrec<>(ImmutableMap.copyOf(mapping), Optional.of(defaultPrec));
	}

	@Override
	public P getPrec(final L loc) {
		if (mapping.containsKey(loc)) {
			return mapping.get(loc);
		}
		if (defaultPrec.isPresent()) {
			return defaultPrec.get();
		}
		throw new NoSuchElementException("Location not found.");
	}

	public GenericLocPrec<P, L, E> refine(final Map<L, P> refinedPrecs) {
		checkNotNull(refinedPrecs);

		final Map<L, P> refinedMapping = new HashMap<>(this.mapping);

		for (final Entry<L, P> entry : refinedPrecs.entrySet()) {
			final L loc = entry.getKey();
			final P prec = entry.getValue();

			// TODO: instead of == this should be 'equals' (it is correct this way as well, but it would be more efficient)
			if (defaultPrec.isPresent() && !mapping.containsKey(loc) && defaultPrec.get() == prec) {
				continue;
			}
			refinedMapping.put(loc, prec);
		}

		return new GenericLocPrec<>(refinedMapping, this.defaultPrec);
	}

	public GenericLocPrec<P, L, E> refine(final L loc, final P refinedPrec) {
		return refine(Collections.singletonMap(loc, refinedPrec));
	}

	@Override
	public String toString() {
		final ToStringBuilder builder = ObjectUtils.toStringBuilder(getClass().getSimpleName());
		builder.add("Precs: " + mapping.size());
		if (defaultPrec.isPresent()) {
			builder.add("Default: " + defaultPrec.get());
		}
		return builder.toString();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof GenericLocPrec) {
			final GenericLocPrec<?, ?, ?> that = (GenericLocPrec<?, ?, ?>) obj;
			return this.defaultPrec.equals(that.defaultPrec) && this.mapping.equals(that.mapping);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return 31 * (defaultPrec.hashCode() + 13 * mapping.hashCode());
	}
}
