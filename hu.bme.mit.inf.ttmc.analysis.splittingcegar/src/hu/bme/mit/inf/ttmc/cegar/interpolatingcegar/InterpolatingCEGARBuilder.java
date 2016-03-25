package hu.bme.mit.inf.ttmc.cegar.interpolatingcegar;

import java.util.ArrayList;
import java.util.List;

import hu.bme.mit.inf.ttmc.cegar.common.GenericCEGARLoop;
import hu.bme.mit.inf.ttmc.cegar.common.ICEGARBuilder;
import hu.bme.mit.inf.ttmc.cegar.common.utils.visualization.Visualizer;
import hu.bme.mit.inf.ttmc.cegar.common.utils.visualization.NullVisualizer;
import hu.bme.mit.inf.ttmc.cegar.interpolatingcegar.data.InterpolatedAbstractState;
import hu.bme.mit.inf.ttmc.cegar.interpolatingcegar.data.InterpolatedAbstractSystem;
import hu.bme.mit.inf.ttmc.cegar.interpolatingcegar.steps.InterpolatingChecker;
import hu.bme.mit.inf.ttmc.cegar.interpolatingcegar.steps.InterpolatingConcretizer;
import hu.bme.mit.inf.ttmc.cegar.interpolatingcegar.steps.InterpolatingInitializer;
import hu.bme.mit.inf.ttmc.cegar.interpolatingcegar.steps.InterpolatingRefiner;
import hu.bme.mit.inf.ttmc.cegar.interpolatingcegar.steps.refinement.CraigInterpolater;
import hu.bme.mit.inf.ttmc.cegar.interpolatingcegar.steps.refinement.IInterpolater;
import hu.bme.mit.inf.ttmc.cegar.interpolatingcegar.steps.refinement.SequenceInterpolater;
import hu.bme.mit.inf.ttmc.cegar.interpolatingcegar.utils.InterpolatingCEGARDebugger;
import hu.bme.mit.inf.ttmc.common.logging.Logger;
import hu.bme.mit.inf.ttmc.common.logging.impl.NullLogger;

public class InterpolatingCEGARBuilder implements ICEGARBuilder {
	private Logger logger = new NullLogger();
	private Visualizer visualizer = new NullVisualizer();
	private boolean collectFromConditions = false;
	private boolean collectFromSpecification = false;
	private InterpolationMethod interpolationMethod = InterpolationMethod.Craig;
	private boolean incrementalModelChecking = true;
	private boolean useCNFTransformation = false;
	private final List<String> explicitVariables = new ArrayList<>();
	private InterpolatingCEGARDebugger debugger = null;

	public enum InterpolationMethod {
		Craig, Sequence
	};

	/**
	 * Set logger
	 *
	 * @param logger
	 * @return Builder instance
	 */
	public InterpolatingCEGARBuilder logger(final Logger logger) {
		this.logger = logger;
		return this;
	}

	/**
	 * Set visualizer
	 *
	 * @param visualizer
	 * @return Builder instance
	 */
	public InterpolatingCEGARBuilder visualizer(final Visualizer visualizer) {
		this.visualizer = visualizer;
		return this;
	}

	/**
	 * Set whether the initial predicates should be collected from conditions
	 *
	 * @param collectFromConditions
	 *            Should initial conditions be collected from conditions
	 * @return Builder instance
	 */
	public InterpolatingCEGARBuilder collectFromConditions(final boolean collectFromConditions) {
		this.collectFromConditions = collectFromConditions;
		return this;
	}

	/**
	 * Set whether the initial predicates should be collected from the
	 * specification
	 *
	 * @param collectFromSpecification
	 *            Should initial conditions be collected from the specification
	 * @return Builder instance
	 */
	public InterpolatingCEGARBuilder collectFromSpecification(final boolean collectFromSpecification) {
		this.collectFromSpecification = collectFromSpecification;
		return this;
	}

	/**
	 * Set the interpolation method
	 *
	 * @param interpolationMethod
	 *            Interpolation method
	 * @return Builder instance
	 */
	public InterpolatingCEGARBuilder interpolationMethod(final InterpolationMethod interpolationMethod) {
		this.interpolationMethod = interpolationMethod;
		return this;
	}

	/**
	 * Set whether the model checking should be incremental or not
	 *
	 * @param incrementalModelChecking
	 *            True for incremental model checking, false otherwise
	 * @return Builder instance
	 */
	public InterpolatingCEGARBuilder incrementalModelChecking(final boolean incrementalModelChecking) {
		this.incrementalModelChecking = incrementalModelChecking;
		return this;
	}

	/**
	 * Set whether CNF transformation should be applied to the constraints
	 *
	 * @param useCNFTransformation
	 *            True for CNF transformation, false otherwise
	 * @return Builder instance
	 */
	public InterpolatingCEGARBuilder useCNFTransformation(final boolean useCNFTransformation) {
		this.useCNFTransformation = useCNFTransformation;
		return this;
	}

	/**
	 * Add a variable that should be tracked explicitly
	 *
	 * @param variable
	 *            Name of the variable
	 * @return Builder instance
	 */
	public InterpolatingCEGARBuilder explicitVariable(final String variable) {
		this.explicitVariables.add(variable);
		return this;
	}

	public InterpolatingCEGARBuilder debug(final Visualizer visualizer) {
		if (visualizer == null)
			this.debugger = null;
		else
			this.debugger = new InterpolatingCEGARDebugger(visualizer);
		return this;
	}

	/**
	 * Build CEGAR loop instance
	 *
	 * @return CEGAR loop instance
	 */
	@Override
	public GenericCEGARLoop<InterpolatedAbstractSystem, InterpolatedAbstractState> build() {
		IInterpolater interpolater = null;
		switch (interpolationMethod) {
		case Craig:
			interpolater = new CraigInterpolater(logger, visualizer);
			break;
		case Sequence:
			interpolater = new SequenceInterpolater(logger, visualizer);
			break;
		default:
			throw new RuntimeException("Unknown interpolation method: " + interpolationMethod);
		}

		return new GenericCEGARLoop<>(
				new InterpolatingInitializer(logger, visualizer, collectFromConditions, collectFromSpecification, useCNFTransformation, explicitVariables),
				new InterpolatingChecker(logger, visualizer, incrementalModelChecking), new InterpolatingConcretizer(logger, visualizer),
				new InterpolatingRefiner(logger, visualizer, interpolater), debugger, logger, "Interpolating");
	}
}
