package hu.bme.mit.theta.frontend.c.transform.slicer;

import java.util.Collection;

import hu.bme.mit.theta.frontend.c.ir.Function;
import hu.bme.mit.theta.frontend.c.ir.node.IrNode;

public class IdentitySlicer implements FunctionSlicer {

    @Override
    public Slice slice(Function function, IrNode criteria, Collection<IrNode> additional) {
        return Slice.fromFunction(function);
    }

}
