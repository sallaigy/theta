package hu.bme.mit.inf.ttmc.frontend.cfa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import hu.bme.mit.inf.ttmc.core.expr.impl.Exprs;
import hu.bme.mit.inf.ttmc.core.type.Type;
import hu.bme.mit.inf.ttmc.core.utils.impl.ExprUtils;
import hu.bme.mit.inf.ttmc.formalism.cfa.CFA;
import hu.bme.mit.inf.ttmc.formalism.cfa.CFAEdge;
import hu.bme.mit.inf.ttmc.formalism.cfa.CFALoc;
import hu.bme.mit.inf.ttmc.formalism.cfa.impl.MutableCFA;
import hu.bme.mit.inf.ttmc.formalism.common.stmt.impl.Stmts;
import hu.bme.mit.inf.ttmc.frontend.ir.BasicBlock;
import hu.bme.mit.inf.ttmc.frontend.ir.Function;
import hu.bme.mit.inf.ttmc.frontend.ir.node.AssertNode;
import hu.bme.mit.inf.ttmc.frontend.ir.node.AssignNode;
import hu.bme.mit.inf.ttmc.frontend.ir.node.EntryNode;
import hu.bme.mit.inf.ttmc.frontend.ir.node.ExitNode;
import hu.bme.mit.inf.ttmc.frontend.ir.node.GotoNode;
import hu.bme.mit.inf.ttmc.frontend.ir.node.IrNode;
import hu.bme.mit.inf.ttmc.frontend.ir.node.JumpIfNode;
import hu.bme.mit.inf.ttmc.frontend.ir.node.NonTerminatorIrNode;
import hu.bme.mit.inf.ttmc.frontend.ir.node.TerminatorIrNode;

/**
 * A utility class for function to CFA (control flow automata) transformations
 *
 * @author Gyula Sallai <salla016@gmail.com>
 */
public class FunctionToCFATransformer {

	public static CFA createLBE(Function func) {
		MutableCFA cfa = new MutableCFA();
		List<BasicBlock> blocks = func.getBlocksDFS();
		Map<BasicBlock, CFALoc> mapping = new HashMap<>();

		// We only need locations for the begin, end and multiple parent blocks in LBE
		blocks
			.stream()
			.filter(b -> b.parents().size() > 1)
			.forEach(b -> mapping.put(b, cfa.createLoc()));

		mapping.put(func.getEntryBlock(), cfa.getInitLoc());
		mapping.put(func.getExitBlock(), cfa.getFinalLoc());

		for (Entry<BasicBlock, CFALoc> entry : mapping.entrySet()) {
			BasicBlock block = entry.getKey();
			CFALoc loc = entry.getValue();

			for (BasicBlock child : block.children()) {
			}
		}

		return cfa;
	}

	/**
	 * Creates a singe-block encoded CFA from an input function
	 *
	 * @param func The function to transform
	 *
	 * @return A CFA instance semantically equivalent to the given function
	 */
	public static CFA createSBE(Function func) {
		MutableCFA cfa = new MutableCFA();
		Map<IrNode, CFALoc> mapping = new HashMap<>();
		List<BasicBlock> blocks = func.getBlocksDFS();
		blocks.remove(func.getEntryBlock());

		// Initialize the CFA locations
		for (BasicBlock block : blocks) {
			for (IrNode node : block.getNodes()) {
				mapping.put(node, cfa.createLoc());
			}

			// Branch nodes need a separate location on their own
			if (block.getTerminator() instanceof JumpIfNode) {
				mapping.put(block.getTerminator(), cfa.createLoc());
			}
		}

		IrNode firstNode = func.getEntryNode().getTarget().getNodeByIndex(0);

		// Replace the initial node
		cfa.removeLoc(mapping.get(firstNode));
		mapping.put(firstNode, cfa.getInitLoc());
		mapping.put(func.getExitNode(), cfa.getFinalLoc());

		/*
		 * Create the CFA edges
		 *
		 * Nodes inside a block are sequential, so they just need to be linked together.
		 * Terminator nodes' targets need to be replaced by their block's first node.
		 */
		for (BasicBlock block : func.getBlocksDFS()) {
			List<NonTerminatorIrNode> nodes = block.getNodes();
			int i;
			for (i = 0; i < nodes.size() - 1; i++) { // Skip the last node (the terminator), it will be handled out of the loop
				IrNode node = nodes.get(i);

				CFALoc source = mapping.get(node);
				CFALoc target = mapping.get(nodes.get(i + 1));
				createCFAEdge(cfa, node, source, target);
			}

			TerminatorIrNode terminator = block.getTerminator();

			CFALoc jumpSource;
			if (block.countNodes() != 0) { // There were nonterminators in the block
				IrNode last = nodes.get(i);
				jumpSource = mapping.get(last);
				if (terminator instanceof GotoNode) {
					CFALoc jumpTarget = mapping.get(((GotoNode) terminator).getTarget().getNodeByIndex(0));
					createCFAEdge(cfa, last, jumpSource, jumpTarget);
				} else if (terminator instanceof JumpIfNode) {
					JumpIfNode branch = (JumpIfNode) terminator;
					CFALoc branchLoc = mapping.get(branch);

					// Jump to the location corresponding to the first node in each branch
					CFALoc then = mapping.get(branch.getThenTarget().getNodeByIndex(0));
					CFALoc elze = mapping.get(branch.getElseTarget().getNodeByIndex(0));

					// Add the then and else edges with the required Assume statements
					CFAEdge thenEdge = cfa.createEdge(branchLoc, then);
					thenEdge.getStmts().add(Stmts.Assume(branch.getCond()));

					CFAEdge elzeEdge = cfa.createEdge(branchLoc, elze);
					elzeEdge.getStmts().add(Stmts.Assume(Exprs.Not(branch.getCond())));

					// Create the last -> branch node edge.
					// This will also annotate the edge with the statement corrseponding to the last instruction node
					createCFAEdge(cfa, last, jumpSource, branchLoc);
				}
			} else {
				if (terminator instanceof JumpIfNode) {
					JumpIfNode branch = (JumpIfNode) terminator;
					CFALoc branchLoc = mapping.get(branch);

					CFALoc then = mapping.get(branch.getThenTarget().getNodeByIndex(0));
					CFALoc elze = mapping.get(branch.getElseTarget().getNodeByIndex(0));

					CFAEdge thenEdge = cfa.createEdge(branchLoc, then);
					thenEdge.getStmts().add(Stmts.Assume(branch.getCond()));

					CFAEdge elzeEdge = cfa.createEdge(branchLoc, elze);
					elzeEdge.getStmts().add(Stmts.Assume(Exprs.Not(branch.getCond())));
				} else if (!(terminator instanceof EntryNode) && !(terminator instanceof ExitNode)) {
					// The goto case should be impossible for normalized CFGs, as they cannot have a block with a single goto node
					throw new RuntimeException(String.format(
						"Invalid terminator class (%s) found in block '%s'. The input function is possibly not normalized.",
						terminator.getClass().getName(),
						block.getName()
					));
				}
			}
		}

		return cfa;
	}

	/**
	 * Appends a new edge into the CFA
	 *
	 * @param cfa		The CFA to append into
	 * @param node		The instruction node of the source
	 * @param source	The source location
	 * @param target	The target location
	 *
	 * @return A newly created source -> target edge in the CFA
	 */
	private static CFAEdge createCFAEdge(MutableCFA cfa, IrNode node, CFALoc source, CFALoc target) {
		CFAEdge edge = cfa.createEdge(source, target);

		if (node instanceof AssignNode<?, ?>) {
			AssignNode<? extends Type, ? extends Type> assign = (AssignNode<?, ?>) node;
			edge.getStmts().add(Stmts.Assign(assign.getVar(), ExprUtils.cast(assign.getExpr(), assign.getVar().getType().getClass())));
		} else if (node instanceof AssertNode) {
			AssertNode assrt = (AssertNode) node;
			edge.getStmts().add(Stmts.Assume(assrt.getCond()));

			CFAEdge errorEdge = cfa.createEdge(source, cfa.getErrorLoc());
			errorEdge.getStmts().add(Stmts.Assume(Exprs.Not(assrt.getCond())));
		}

		return edge;
	}

}