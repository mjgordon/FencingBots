package util;

public class DoubleAveraged {
	double[] internal;
	
	public int counter = 0;
	
	public DoubleAveraged() {
		internal = new double[10];
		
		for (int i = 0; i < internal.length; i++) {
			internal[i] = 0;
		}
	}
	
	public void add(double d) {
		internal[counter] = d;
		counter = (counter + 1) % internal.length;
	}
	
	public double get() {
		double total = 0;
		for (double d : internal) {
			total += d;
		}
		total /= internal.length;
		return total;
	}
}
