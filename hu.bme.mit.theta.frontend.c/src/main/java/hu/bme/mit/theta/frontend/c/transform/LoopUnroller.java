package hu.bme.mit.theta.frontend.c.transform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import hu.bme.mit.theta.frontend.c.dependency.LoopInfo;
import hu.bme.mit.theta.frontend.c.ir.BasicBlock;
import hu.bme.mit.theta.frontend.c.ir.Function;

public class LoopUnroller extends FunctionTransformer {

	private int maxDepth;

	public LoopUnroller(int depth) {
		this.maxDepth = depth;
	}

	@Override
	public void transform(Function function) {
		List<LoopInfo> loops = LoopInfo.findLoops(function);

		for (LoopInfo loop : loops) {
			this.unroll(loop, this.maxDepth);
		}
	}

	public void unroll(LoopInfo loop, int depth) {
		BasicBlock header = loop.getHeader();
		List<BasicBlock> blocks = loop.getBlocks();
		Function function = header.getFunction();

		for (int i = 0; i < depth; ++i) {
			Map<BasicBlock, BasicBlock> mapping = new HashMap<>();
			blocks.forEach(block -> {
				BasicBlock copy = function.copyBlock(block);
				copy.terminate(block.getTerminator().copy());

				mapping.put(block, copy);
			});

			/*
			 * Rewire header parents into the header copy
			 */
			BasicBlock headerCopy = mapping.get(header);
			List<BasicBlock> parents = header.parents().stream().filter(parent -> !blocks.contains(parent))
					.filter(parent -> !mapping.containsValue(parent)).collect(Collectors.toList());

			for (BasicBlock parent : parents) {
				parent.getTerminator().replaceTarget(header, headerCopy);
			}

			/*
			 * Rewire copy terminators into the appropiate targets. - All loop
			 * body nodes need to be rewired to their corresponding copy - All
			 * loop exits must point to their original locations - Back edges
			 * need to point to the original loop header
			 */
			for (Entry<BasicBlock, BasicBlock> entry : mapping.entrySet()) {
				BasicBlock orig = entry.getKey();
				BasicBlock copy = entry.getValue();

				for (BasicBlock child : orig.children()) {
					if (child == header)
						continue;

					BasicBlock childCopy = mapping.get(child);
					if (childCopy != null) {
						copy.getTerminator().replaceTarget(child, childCopy);
					}
				}
			}
		}
	}

	@Override
	public String getTransformationName() {
		return "LoopUnroll";
	}

}