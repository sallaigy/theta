package hu.bme.mit.inf.ttmc.frontend.ir.utils;

import hu.bme.mit.inf.ttmc.frontend.dependency.ControlDependencyGraph;
import hu.bme.mit.inf.ttmc.frontend.dependency.ControlDependencyGraph.CDGNode;
import hu.bme.mit.inf.ttmc.frontend.dependency.DominatorTree;
import hu.bme.mit.inf.ttmc.frontend.dependency.ProgramDependency;
import hu.bme.mit.inf.ttmc.frontend.dependency.ProgramDependency.PDGNode;
import hu.bme.mit.inf.ttmc.frontend.ir.BasicBlock;
import hu.bme.mit.inf.ttmc.frontend.ir.Function;
import hu.bme.mit.inf.ttmc.frontend.ir.node.IrNode;
import hu.bme.mit.inf.ttmc.frontend.ir.node.JumpIfNode;

public class IrPrinter {

	public static String toText(Function func) {
		StringBuilder sb = new StringBuilder();

		return sb.toString();
	}

	public static String controlDependencyGraph(ControlDependencyGraph cdg) {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph G {\n");

		for (CDGNode node : cdg.getNodes()) {
			sb.append(String.format("node_%s [label=\"%s\"];\n", System.identityHashCode(node), node.block.getLabel()));
			for (CDGNode child : node.children) {
				sb.append(String.format("node_%s -> node_%s [color=\"blue\"];\n", System.identityHashCode(node), System.identityHashCode(child)));
			}
		}

		sb.append("}\n");
		return sb.toString();
	}

	public static String programDependencyGraph(ProgramDependency pdg) {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph G {\n");

		int id = 0;
		for (PDGNode n : pdg.getNodes()) {
			sb.append(String.format("node_%s [label=\"%s\"];\n", System.identityHashCode(n), n.node.getLabel()));
			n.controlChildren.forEach(c -> {
				sb.append(String.format("node_%s -> node_%s [color=\"blue\"];\n", System.identityHashCode(n), System.identityHashCode(c)));
			});
			n.flowChildren.forEach(c -> {
				sb.append(String.format("node_%s -> node_%s [color=\"green\"];\n", System.identityHashCode(n), System.identityHashCode(c)));
			});
		}

		sb.append("}\n");

		return sb.toString();
	}

	public static String dominatorTreeGraph(DominatorTree dt) {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph G {\n");

		sb.append(String.format("node_%s [label=\"%s\"];\n", dt.getRoot().getName(), dt.getRoot().getLabel()));
		dt.getFunction().getBlocks().stream().filter(s -> s != dt.getRoot()).forEach(b -> {
			sb.append(String.format("node_%s [label=\"%s\"];\n", b.getName(), b.getLabel()));
			sb.append(String.format("node_%s -> node_%s;\n", dt.getParent(b).getName(), b.getName()));
		});

		sb.append("}\n");

		return sb.toString();
	}

	public static String toGraphvizString(Function func) {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph G {\n");

		for (BasicBlock block : func.getBlocks()) {
			sb.append(String.format("node_%s [label=\"%s\"];\n", block.getName(), block.getLabel()));

			if (block.getTerminator() instanceof JumpIfNode) {
				JumpIfNode terminator = (JumpIfNode) block.getTerminator();
				sb.append(String.format("node_%s -> node_%s [label=\" True\"];\n", block.getName(), terminator.getThenTarget().getName()));
				sb.append(String.format("node_%s -> node_%s [label=\" False\"];\n", block.getName(), terminator.getElseTarget().getName()));
			} else {
				block.children().forEach(s ->
					sb.append(String.format("node_%s -> node_%s;\n", block.getName(), s.getName()))
				);
			}

		}

		sb.append("}\n");

		return sb.toString();
	}

}
