package hu.bme.mit.theta.common.visualization;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class Node {

	private final String id;
	private final String label;
	private final Color edgeColor;
	private final Color fillColor;
	private final String lineStyle;

	private final Collection<Edge> inEdges;
	private final Collection<Edge> outEdges;

	public Node(final String id, final String label, final Color fillColor, final Color edgeColor,
			final String lineStyle) {
		this.id = checkNotNull(id);
		this.label = checkNotNull(label);
		this.edgeColor = checkNotNull(edgeColor);
		this.fillColor = checkNotNull(fillColor);
		this.lineStyle = checkNotNull(lineStyle);

		this.inEdges = new ArrayList<>();
		this.outEdges = new ArrayList<>();
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public Color getEdgeColor() {
		return edgeColor;
	}

	public Color getFillColor() {
		return fillColor;
	}

	public String getLineStyle() {
		return lineStyle;
	}

	public Collection<Edge> getInEdges() {
		return Collections.unmodifiableCollection(inEdges);
	}

	public Collection<Edge> getOutEdges() {
		return Collections.unmodifiableCollection(outEdges);
	}

	void addOutEdge(final Edge edge) {
		checkArgument(edge.getSource() == this);
		outEdges.add(edge);
	}

	void addInEdge(final Edge edge) {
		checkArgument(edge.getTarget() == this);
		inEdges.add(edge);
	}
}
