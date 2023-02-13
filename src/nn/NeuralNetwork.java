package nn;

import static fencingBotsSim.Bridge.p;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

import processing.core.PGraphics;

public class NeuralNetwork {

	public int[] layerCounts;

	public double[][][] weights;

	private int maxLayerSize;

	public double[][] currentValues;

	private PGraphics gBackground = null;;


	
	public NeuralNetwork(int[] rawLayerCounts) {
		this.layerCounts = new int[rawLayerCounts.length];
		for (int i = 0; i < rawLayerCounts.length; i++) {
			this.layerCounts[i] = rawLayerCounts[i] + 1;
		}

		maxLayerSize = 0;
		for (int count : this.layerCounts) {
			if (count > maxLayerSize) {
				maxLayerSize = count;
			}
		}
		
		weights = new double[this.layerCounts.length - 1][maxLayerSize][maxLayerSize];

		currentValues = new double[this.layerCounts.length][maxLayerSize];

		reset();
	}
	


	
	public NeuralNetwork(int[] rawLayerCounts, NNGenome genome) {
		this.layerCounts = new int[rawLayerCounts.length];
		for (int i = 0; i < rawLayerCounts.length; i++) {
			this.layerCounts[i] = rawLayerCounts[i] + 1;
		}

		maxLayerSize = 0;
		for (int count : this.layerCounts) {
			if (count > maxLayerSize) {
				maxLayerSize = count;
			}
		}
		
		weights = new double[this.layerCounts.length - 1][maxLayerSize][maxLayerSize];

		currentValues = new double[this.layerCounts.length][maxLayerSize];

		reset();

		int n = 0;
		for (int i = 0; i < this.layerCounts.length - 1; i++) { // For each layer
			for (int j = 0; j < this.layerCounts[i]; j++) { // For each neuron in that layer

				for (int k = 0; k < this.layerCounts[i + 1] - 1; k++) { // For each neuron in the next layer
					weights[i][j][k] = genome.weights[n];
					n += 1;
				}
			}
		}
		//System.out.println("Final n : " + n);
	}
	


	public double[] calculate(double[] input) {
		if (input.length != this.currentValues[0].length - 1) {
			System.out.println("Input mismatch");
			System.out.println(input.length + " : " + this.currentValues[0].length);
		}

		// Set input into network
		for (int i = 0; i < input.length; i++) {
			currentValues[0][i] = input[i];
		}
		// Set bias on input layer
		currentValues[0][currentValues[0].length - 1] = 1;

		for (int v = 0; v < currentValues.length - 1; v++) { // For each layer
			for (int i = 0; i < layerCounts[v]; i++) { // For each neuron in that lyaer
				for (int j = 0; j < layerCounts[v + 1] - 1; j++) { // For each neuron in the next layer, except the bias
					currentValues[v + 1][j] += currentValues[v][i] * weights[v][i][j];
				}
			}
			// After totalling, set neuron output according to sigmoid function, except for bias
			for (int j = 0; j < layerCounts[v + 1] - 1; j++) {
				currentValues[v + 1][j] = sigmoid(currentValues[v + 1][j]);
			}
			// Ensure bias is 1 (though why wouldn't it be)
			currentValues[v + 1][layerCounts[v + 1 ] - 1] = 1;
		}

		return (currentValues[currentValues.length - 1]);

	}


	public void preRender(PGraphics g2, int height) {
		gBackground = p.createGraphics(g2.width, height);
		gBackground.beginDraw();
		gBackground.background(0);

		int w = gBackground.width / (layerCounts.length + 1);

		int x = w;
		
		for (int i = 0; i < layerCounts.length; i++) {
			for (int j = 0; j < layerCounts[i]; j++) {
				int h = height / layerCounts[i];
				float y = (j - (layerCounts[i] / 2.0f) + 0.5f) * h + (height / 2.0f);
				if (i < layerCounts.length - 1) {
					for (int k = 0; k < layerCounts[i + 1]; k++) {
						double weight = weights[i][j][k];
						//System.out.println(weight);
						if (weight > 0) {
							gBackground.stroke(0, 0, 255, (float) weight * 255);
						}
						else {
							gBackground.stroke(255, 0, 0, (float) Math.abs(weight) * 255);
						}
						int h2 = height / layerCounts[i + 1];
						float y2 = (k - (layerCounts[i + 1] / 2.0f) + 0.5f) * h2 + (height / 2.0f);
						gBackground.line(x, y, x + w, y2);

					}
				}
			}
			x += w;
		}
		
		gBackground.endDraw();
		
	}


	public void render(PGraphics g, int xOffset, int yOffset, int height) {
		if (gBackground == null) {
			g.endDraw();
			preRender(g,height);
			g.beginDraw();
		}
		
		g.pushMatrix();
		g.translate(xOffset, yOffset);
		g.fill(255);
		g.stroke(0);

		if (gBackground != null) {
			g.image(gBackground,0,0);
		}
		else {
			g.rect(0, 0, g.width, height);	
		}
		
		// g.line(0, height / 2, g.width, height / 2);

		int w = g.width / (layerCounts.length + 1);

		int x = w;

		DecimalFormat df = new DecimalFormat("#.###");

		ArrayList<Integer> inputEdges = new ArrayList<Integer>();
		inputEdges.add(1);
		inputEdges.add(3);
		inputEdges.add(16);
		inputEdges.add(22);
		inputEdges.add(31);
		inputEdges.add(40);

		// List<Integer> inputEdges = Arrays.asList(new int[] {1,3,16,22,31,40});

		for (int i = 0; i < layerCounts.length; i++) {
			for (int j = 0; j < layerCounts[i]; j++) {
				int h = height / layerCounts[i];
				float y = (j - (layerCounts[i] / 2.0f) + 0.5f) * h + (height / 2.0f);

				if (currentValues[i][j] > 0) {
					g.fill(0, 0, (float) currentValues[i][j] * 255);

				}
				else {
					g.fill((float) currentValues[i][j] * -255, 0, 0);
				}
				g.ellipse(x, y, 10, 10);

				if (i == 0 || i == layerCounts.length - 1) {
					g.fill(0);
					g.text(df.format(currentValues[i][j]), x + 10, y + 6);
				}

				if (i == 0) {
					g.stroke(0);
					if (inputEdges.contains(j)) {
						g.line(x - 20, y + 8, x + 10, y + 8);
					}
				}

			}
			x += w;
		}
		g.popMatrix();
	}


	public int getOutputSize() {
		return (layerCounts[layerCounts.length - 1]);
	}


	public double[] getOutputLayer() {
		return Arrays.copyOfRange(currentValues[currentValues.length - 1], 0, layerCounts[layerCounts.length - 1] - 1);
	}


	public int getWeightSum() {
		int count = 0;
		for (int i = 0; i < layerCounts.length - 1; i++) {
			for (int j = 0; j < layerCounts[i]; j++) {
				count += layerCounts[i + 1];
			}
		}

		System.out.println(count + " total weights");

		return count;
	}


	private void reset() {
		for (int i = 0; i < currentValues.length; i++) {
			for (int j = 0; j < currentValues[i].length; j++) {
				currentValues[i][j] = 0;
			}
		}

		for (int i = 0; i < layerCounts.length - 1; i++) {
			for (int j = 0; j < layerCounts[i]; j++) {
				for (int k = 0; k < layerCounts[i + 1]; k++) {
					weights[i][j][k] = 0;
				}
			}
		}
	}


	private static double sigmoid(double x) {
		// Reaches ~0.5 at 1, reaches ~1 at 4
		return ((1 / (1 + Math.exp(-x))) - 0.5) * 2;
	}

}
