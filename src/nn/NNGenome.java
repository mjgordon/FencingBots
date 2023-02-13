package nn;

import java.util.HashSet;

import sim.Util;

public class NNGenome implements Comparable<NNGenome> {
	public double[] weights;
	
	public double fitness = 0;
	
	public String type = "L";
	
	public String parentTypeA = null;
	public String parentTypeB = null;
	
	
	public NNGenome(int length) {
		weights = new double[length];
		for (int i = 0; i < length; i++) {
			weights[i] = 0;
		}
	}
	
	
	public NNGenome(double[] d) {
		weights = new double[d.length];
		for (int i = 0; i < weights.length; i++) {
			weights[i] = d[i];
		}
	}
	
	public NNGenome (NNGenome input) {
		weights = new double[input.weights.length];
		for (int i = 0; i < weights.length; i++) {
			weights[i] = input.weights[i];
		}
	}
	
	public NNGenome mutate() {
		NNGenome output = new NNGenome(this);
		int count = (int)Util.randomRange(10,50);
		for (int i = 0; i < count; i++) {
			// Normal mutation (0.8)
			double r = Math.random();
			int n = (int)Util.randomRange(0, weights.length);
			if (r < 0.8) {
				output.weights[n] += Util.randomRange(-0.3, 0.3);
			}
			// Damping mutation (0.1)
			else if (r < 0.9) {
				output.weights[n] *= Util.randomRange(0.5, 0.99);
			}
			// Inverting mutation (0.1)
			else {
				output.weights[n] *= -1;
			}
		}
		return output;
	}
	
	public NNGenome mutateInitial() {
		NNGenome output = new NNGenome(this);
		int count = 200;
		for (int i = 0; i < count; i++) {
			output.weights[(int)Util.randomRange(0, weights.length)] += Util.randomRange(-1,1);	
		}
		return output;
	}
	
	public NNGenome crossover(NNGenome other) {
		NNGenome output = new NNGenome(this);
		boolean otherFlag = false;
		
		int crossCount = (int)Util.randomRange(1, 10);
		
		HashSet<Integer> crosses = new HashSet<Integer>();
		for (int i= 0; i< crossCount; i++) {
			crosses.add((int)Util.randomRange(0, weights.length));
		}
		
		for (int i = 0; i < output.weights.length; i++) {
			if (crosses.contains(i)) {
				otherFlag = ! otherFlag;
			}
			if (otherFlag) {
				output.weights[i] = other.weights[i];
			}
		}
		
		return output;
	}

	@Override
	public int compareTo(NNGenome o) {
		if (this.fitness > o.fitness) {
			return 1;
		}
		else if (this.fitness < o.fitness) {
			return -1;
		}
		else {
			return 0;
		}
	}
	
	
}
