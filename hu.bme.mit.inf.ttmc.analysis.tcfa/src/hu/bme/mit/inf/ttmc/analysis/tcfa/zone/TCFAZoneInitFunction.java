package hu.bme.mit.inf.ttmc.analysis.tcfa.zone;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;

import hu.bme.mit.inf.ttmc.analysis.InitFunction;
import hu.bme.mit.inf.ttmc.analysis.zone.ZonePrecision;
import hu.bme.mit.inf.ttmc.analysis.zone.ZoneState;

public class TCFAZoneInitFunction implements InitFunction<ZoneState, ZonePrecision> {

	@Override
	public Collection<ZoneState> getInitStates(final ZonePrecision precision) {
		checkNotNull(precision);
		return Collections.singleton(ZoneState.zero(precision.getClocks()));
	}

}
