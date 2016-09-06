package hu.bme.mit.inf.theta.code;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import hu.bme.mit.inf.theta.frontend.cfa.FunctionToCFATransformer;
import hu.bme.mit.inf.theta.frontend.dependency.ControlDependencyGraph;
import hu.bme.mit.inf.theta.frontend.dependency.DominatorTree;
import hu.bme.mit.inf.theta.frontend.dependency.LoopInfo;
import hu.bme.mit.inf.theta.frontend.ir.Function;
import hu.bme.mit.inf.theta.frontend.ir.GlobalContext;
import hu.bme.mit.inf.theta.frontend.ir.utils.IrPrinter;
import hu.bme.mit.inf.theta.frontend.transform.ConstantPropagator;
import hu.bme.mit.inf.theta.frontend.transform.DeadBranchEliminator;
import hu.bme.mit.inf.theta.frontend.transform.FunctionSlicer;
import hu.bme.mit.inf.theta.frontend.transform.LoopUnroller;
import hu.bme.mit.inf.theta.formalism.cfa.CFA;
import hu.bme.mit.inf.theta.formalism.utils.impl.CfaPrinter;

class Application {

	public static void main(String[] args)
			throws CoreException, FileNotFoundException, IOException, InterruptedException {

		GlobalContext context = Parser.parse("all.c");

		context.functions().forEach(function -> {
			System.out.println("===============" + function.getName() + "===============");
			System.out.println("------" + "CFG" + "------");
			System.out.println(IrPrinter.toGraphvizString(function));
/*
			System.out.println("------" + "const prop" + "------");
			ConstantPropagator constProp = new ConstantPropagator();
			constProp.transform(function);
			System.out.println(IrPrinter.toGraphvizString(function));
			System.out.println("------" + "dead branch" + "------");
			DeadBranchEliminator dead = new DeadBranchEliminator();
			dead.transform(function);
			System.out.println(IrPrinter.toGraphvizString(function));
*/
			System.out.println("------" + "CFA SBE" + "------");
			CFA cfa = FunctionToCFATransformer.createSBE(function);
			System.out.println(CfaPrinter.toGraphvizSting(cfa));
			System.out.println("------" + "CFA LBE" + "------");
			cfa = FunctionToCFATransformer.createLBE(function);
			System.out.println(CfaPrinter.toGraphvizSting(cfa));

//
//			System.out.println("------" + "Constant prop" + "------");
//			ConstantPropagator constProp = new ConstantPropagator();
//			constProp.transform(function);
//			//System.out.println(IrPrinter.toGraphvizString(function));
//
//			System.out.println("------" + "Dead branch elim" + "------");
//			DeadBranchEliminator dbe = new DeadBranchEliminator();
//			dbe.transform(function);
//			//System.out.println(IrPrinter.toGraphvizString(function));
//
//			function.normalize();
//			System.out.println("------" + "final CFG" + "------");
//			System.out.println(IrPrinter.toGraphvizString(function));
//			System.out.println("------" + "PDT" + "------");
//			//DominatorTree pdt = DominatorTree.createDominatorTree(function);
//			//System.out.println(IrPrinter.dominatorTreeGraph(pdt));
//
//			System.out.println("------" + "LoopUtils" + "------");
//			LoopAnalysis.findLoops(function).forEach(l -> System.out.println(l.head + " " + l.body));
//			LoopUnroller unroll = new LoopUnroller(1);
//
//			//unroll.transform(function);
//			System.out.println(IrPrinter.toGraphvizString(function));
//
//
////			System.out.println("------" + "Dominators" + "------");
////			DominatorTree dt = DominatorTree.createDominatorTree(s);
////			System.out.println(IrPrinter.dominatorTreeGraph(dt));
////
//
////			System.out.println("------" + "Constant prop" + "------");
////			ConstantPropagator constProp = new ConstantPropagator();
////			constProp.transform(s);
////			System.out.println(IrPrinter.toGraphvizString(s));
////
////			System.out.println("------" + "Dead branch elim" + "------");
////			DeadBranchEliminator dbe = new DeadBranchEliminator();
////			dbe.transform(s);
////			System.out.println(IrPrinter.toGraphvizString(s));
////
////			s.normalize();
////			System.out.println("------" + "final CFG" + "------");
////			System.out.println(IrPrinter.toGraphvizString(s));
////
////			System.out.println("------" + "PDT" + "------");
////			DominatorTree pdt = DominatorTree.createPostDominatorTree(s);
////			System.out.println(IrPrinter.dominatorTreeGraph(pdt));
////
			System.out.println("------" + "slicer" + "------");
			FunctionSlicer slicer = new FunctionSlicer();
			List<Function> slices = slicer.allSlices(function, FunctionSlicer.SLICE_ON_ASSERTS);

			slices.forEach(f -> {
				System.out.println("---" + "slice" + "---");
				System.out.println(IrPrinter.toGraphvizString(f));
			});
		});
	}
}
