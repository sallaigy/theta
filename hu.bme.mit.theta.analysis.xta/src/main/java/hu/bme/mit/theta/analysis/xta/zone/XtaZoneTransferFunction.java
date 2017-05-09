package hu.bme.mit.theta.analysis.xta.zone;

import java.util.Collection;

import com.google.common.collect.ImmutableList;

import hu.bme.mit.theta.analysis.TransferFunction;
import hu.bme.mit.theta.analysis.xta.XtaAction;
import hu.bme.mit.theta.analysis.zone.ZonePrec;
import hu.bme.mit.theta.analysis.zone.ZoneState;

final class XtaZoneTransferFunction implements TransferFunction<ZoneState, XtaAction, ZonePrec> {

	private final static XtaZoneTransferFunction INSTANCE = new XtaZoneTransferFunction();

	private XtaZoneTransferFunction() {
	}

	static XtaZoneTransferFunction getInstance() {
		return INSTANCE;
	}

	@Override
	public Collection<ZoneState> getSuccStates(final ZoneState state, final XtaAction action, final ZonePrec prec) {

		final ZoneState succState = XtaZoneUtils.post(state, action, prec);

		if (succState.isBottom()) {
			return ImmutableList.of();
		} else {
			return ImmutableList.of(succState);
		}
	}

}
