package hu.bme.mit.theta.frontend.c.transform.slicer;

import java.util.Collection;

import javax.xml.crypto.NodeSetData;

import hu.bme.mit.theta.frontend.c.ir.BasicBlock;
import hu.bme.mit.theta.frontend.c.ir.Function;
import hu.bme.mit.theta.frontend.c.ir.node.IrNode;
import hu.bme.mit.theta.frontend.c.ir.node.NodeFactory;
import hu.bme.mit.theta.frontend.c.ir.node.ReturnNode;
import hu.bme.mit.theta.frontend.c.ir.node.TerminatorIrNode;

public class IdentitySlicer implements FunctionSlicer {

    @Override
    public Slice slice(Function function, IrNode criteria, Collection<IrNode> additional) {

        // HACK XXX
        Function copy = function.copy();
        /*
        for (BasicBlock bb : copy.getExitBlock().parents()) {
            TerminatorIrNode terminator = bb.getTerminator();
            if (terminator instanceof ReturnNode) {
                bb.replaceNode(terminator, NodeFactory.Goto(copy.getExitBlock()));
            }
        }*/

        return Slice.fromFunction(copy);
    }

}
