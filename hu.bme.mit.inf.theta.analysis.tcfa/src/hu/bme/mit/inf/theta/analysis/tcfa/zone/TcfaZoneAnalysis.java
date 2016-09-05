package hu.bme.mit.inf.theta.analysis.tcfa.zone;

import hu.bme.mit.inf.theta.analysis.ActionFunction;
import hu.bme.mit.inf.theta.analysis.Analysis;
import hu.bme.mit.inf.theta.analysis.Domain;
import hu.bme.mit.inf.theta.analysis.InitFunction;
import hu.bme.mit.inf.theta.analysis.TransferFunction;
import hu.bme.mit.inf.theta.analysis.tcfa.TcfaAction;
import hu.bme.mit.inf.theta.analysis.zone.ZoneDomain;
import hu.bme.mit.inf.theta.analysis.zone.ZonePrecision;
import hu.bme.mit.inf.theta.analysis.zone.ZoneState;

public class TcfaZoneAnalysis implements Analysis<ZoneState, TcfaAction, ZonePrecision> {

	private static final TcfaZoneAnalysis INSTANCE = new TcfaZoneAnalysis();

	private TcfaZoneAnalysis() {
	}

	public static TcfaZoneAnalysis getInstance() {
		return INSTANCE;
	}

	@Override
	public Domain<ZoneState> getDomain() {
		return ZoneDomain.getInstance();
	}

	@Override
	public InitFunction<ZoneState, ZonePrecision> getInitFunction() {
		return TcfaZoneInitFunction.getInstance();
	}

	@Override
	public TransferFunction<ZoneState, TcfaAction, ZonePrecision> getTransferFunction() {
		return TcfaZoneTransferFunction.getInstance();
	}

	@Override
	public ActionFunction<? super ZoneState, ? extends TcfaAction> getActionFunction() {
		throw new UnsupportedOperationException();
	}

}
