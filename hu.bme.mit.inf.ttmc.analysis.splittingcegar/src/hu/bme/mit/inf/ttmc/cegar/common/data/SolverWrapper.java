package hu.bme.mit.inf.ttmc.cegar.common.data;

import hu.bme.mit.inf.ttmc.solver.ItpSolver;
import hu.bme.mit.inf.ttmc.solver.Solver;

public class SolverWrapper {
	private Solver solver;
	private ItpSolver itpSolver;

	public SolverWrapper(final Solver solver, final ItpSolver itpSolver) {
		this.solver = solver;
		this.itpSolver = itpSolver;
	}

	public Solver getSolver() {
		return solver;
	}

	public void setSolver(final Solver solver) {
		this.solver = solver;
	}

	public ItpSolver getItpSolver() {
		return itpSolver;
	}

	public void setItpSolver(final ItpSolver itpSolver) {
		this.itpSolver = itpSolver;
	}

}
