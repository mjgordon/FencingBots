package fencingBotsSim;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import nn.NNGenome;
import nn.NeuralNetwork;
import processing.core.PApplet;
import processing.core.PGraphics;
import sim.Bot;
import sim.World;

public class EvolutionEnvironment {
	public NNGenome[] genomes;

	World currentWorld;

	private boolean running = true;

	public int[] matchupIdsA;
	public int[] matchupIdsB;

	public int matchup = 0;
	
	public static final int GENERATION_SIZE = 30;


	public EvolutionEnvironment() {
		// randomizeGeneration();
		loadGeneration(false);

		matchupIdsA = new int[genomes.length * genomes.length];
		matchupIdsB = new int[genomes.length * genomes.length];
		int n = 0;
		for (int i = 0; i < genomes.length; i++) {
			for (int j = 0; j < genomes.length; j++) {
				matchupIdsA[n] = i;
				matchupIdsB[n] = j;
				n += 1;
			}
		}

		this.currentWorld = new World(genomes[matchupIdsA[matchup]], genomes[matchupIdsB[matchup]]);

		// saveGeneration();
	}


	public void update() {
		if (running) {
			currentWorld.update();
			if (currentWorld.isFinished) {
				running = false;
			}
		}
		else {

			if (currentWorld.scoreA > 0) {
				genomes[matchupIdsA[matchup]].fitness += currentWorld.scoreA;
			}

			if (currentWorld.scoreB > 0) {
				genomes[matchupIdsB[matchup]].fitness += currentWorld.scoreB;
			}

			matchup += 1;

			if (matchup >= matchupIdsA.length) {
				createGeneration(GENERATION_SIZE);
			}

			running = true;
			this.currentWorld = new World(genomes[matchupIdsA[matchup]], genomes[matchupIdsB[matchup]]);

			System.out.println("New match : " + matchupIdsA[matchup] + " : " + matchupIdsB[matchup]);

		}

	}


	public void createGeneration(int count) {
		// Q is 1/4 the size of the new generation
		int quarter = count / 4;
		
		ArrayList<NNGenome> genomesNew = new ArrayList<NNGenome>();

		ArrayList<NNGenome> genomesPool = new ArrayList<NNGenome>();
		for (NNGenome g : genomes) {
			genomesPool.add(g);
		}
		Collections.sort(genomesPool);
		Collections.reverse(genomesPool);
		System.out.println("1st place fitness : " + genomesPool.get(0).fitness);
		System.out.println("2nd place fitness : " + genomesPool.get(1).fitness);

		// Include the Q best current genomes
		for (int i = 0; i < quarter; i++) {
			NNGenome g = genomesPool.get(0);
			g.parentTypeA = g.type;
			g.parentTypeB = null;
			g.type = "F";
			genomesNew.add(g);
			genomesPool.remove(0);
		}

		// Include Q random remaining genomes, weighted towards the front
		for (int i = 0; i < quarter; i++) {
			int r = genomesPool.size();
			int total = r * (r - 1) / 2 + r;
			int n = (int) (Math.random() * total);
			int counter = 0;
			for (int j = r; j > 0; j--) {
				if (n <= j) {
					NNGenome g = genomesPool.get(counter);
					g.parentTypeA = g.type;
					g.parentTypeB = null;
					g.type = "P";
					genomesNew.add(g);
					genomesPool.remove(counter);
					break;
				}
				else {
					n -= j;
					counter += 1;
				}
			}

		}

		// Include Q mutations of the already added genomes
		for (int i = 0; i < quarter; i++) {
			int n = (int) (Math.random() * (quarter * 2));
			NNGenome g = genomesNew.get(n).mutate();
			g.parentTypeA = genomesNew.get(n).type;
			g.parentTypeB = null;
			g.type = "M";
			genomesNew.add(g);
			
		}

		// Include Q-1 crossovers of the already added genomes
		for (int i = 0; i < (quarter - 1); i++) {
			int a = (int) (Math.random() * (quarter * 2));
			int b = (int) (Math.random() * (quarter * 2));
			while (a == b) {
				b = (int) (Math.random() * (quarter * 2));
			}
			
			NNGenome g = genomesNew.get(a).crossover(genomesNew.get(b));
			g.parentTypeA = genomesNew.get(a).type;
			g.parentTypeB = genomesNew.get(b).type;
			g.type = "C";
			genomesNew.add(g);
		}

		// Include at least 1, totally random genome, more if necessary to reach count due to rounding
		while (genomesNew.size() < count) {
			genomesNew.add(new NNGenome(genomesNew.get(0).weights.length).mutateInitial());
			genomesNew.get(genomesNew.size() - 1).type = "R";
		}
		
		genomes = new NNGenome[genomesNew.size()];
		genomes = genomesNew.toArray(genomes);

		for (NNGenome g : genomes) {
			g.fitness = 0; // Just to make sure
		}

		matchup = 0;

		System.out.println("New generation");

		saveGeneration();
	}


	public void renderWorld(PGraphics g) {
		currentWorld.render(g);
	}


	public void renderUI(PApplet p, PGraphics g) {
		currentWorld.botB.nn.render(g, 0, 400, g.height - 400);

		for (int i = 0; i < genomes.length; i++) {
			String gText = String.format("%2s", i) + " : " + String.format("%3s", (int) genomes[i].fitness) + " : " + genomes[i].type;
			
			if (genomes[i].parentTypeA != null) {
				gText += "(" + genomes[i].parentTypeA;
				if (genomes[i].parentTypeB != null) {
					gText += "," + genomes[i].parentTypeB;
				}
				
				gText += ")";
				
			}
			
			g.text( gText, 20, 20 + (i * 12));	
			
		}

		g.stroke(0);
		g.fill(Bot.botAColor);
		g.ellipse(10, (matchupIdsA[matchup] * 12) + 15, 10, 10);

		g.fill(Bot.botBColor);
		g.ellipse(100, (matchupIdsB[matchup] * 12) + 15, 10, 10);

		g.fill(0);
		int x = g.width - 100;
		int y = 20;

		g.text(p.frameRate, g.width - 100, y);
		y += 12;

		g.text(currentWorld.getTimeRatio() + "", x, y);
		y += 12;

		g.text("Match " + matchup + "/" + matchupIdsA.length + "", x, y);
		y += 12;

		g.text(matchupIdsA[matchup] + " vs " + matchupIdsB[matchup], x, y);
		y += 12;

		g.text(currentWorld.scoreA + " : " + currentWorld.scoreB, x, y);
		y += 12;

	}


	public void randomizeGeneration() {
		NeuralNetwork baseNetwork = new NeuralNetwork(Bot.nnLayerSizes);
		NNGenome base = new NNGenome(baseNetwork.getWeightSum());

		this.genomes = new NNGenome[20];
		for (int i = 0; i < genomes.length; i++) {
			genomes[i] = base.mutateInitial();
		}
	}


	public void saveGeneration() {
		String[] output = new String[genomes.length];
		for (int i = 0; i < output.length; i++) {
			output[i] = "";
			for (double d : genomes[i].weights) {
				output[i] += d + ",";
			}
		}
		PApplet.saveStrings(new File("generation_out.csv"), output);
	}


	public void loadGeneration(boolean mutate) {
		String[] input = PApplet.loadStrings(new File("generation_out.csv"));
		this.genomes = new NNGenome[input.length];
		for (int i = 0; i < input.length; i++) {
			String s = input[i];
			String[] parts = s.split(",");
			double[] d = new double[parts.length];
			for (int j = 0; j < d.length; j++) {
				d[j] = Double.valueOf(parts[j]);
			}
			
			genomes[i] = new NNGenome(d);
			if (mutate) {
				genomes[i] = genomes[i].mutate();
			}
		}
	}
	
	
	/**
	 * Helper for fixing genome when loading from a file with a different config
	 * @param size
	 * @return
	 */
	@SuppressWarnings("unused")
	private static double[] getZeros(int size) {
		double[] output = new double[size];
		for (int i = 0; i < size; i++) {
			output[i] = 0;
		}
		return output;
	}
	
	
	/**
	 * Helper for fixing genome when loading from a file with a different config
	 * @param arrayMain
	 * @param arrayAdd
	 * @param position
	 * @return
	 */
	@SuppressWarnings("unused")
	private static double[] arrayInsert(double[] arrayMain, double arrayAdd[], int position) {
		double[] output = new double[arrayMain.length + arrayAdd.length];
		
		System.arraycopy(arrayMain, 0, output, 0, position);
		System.arraycopy(arrayAdd, 0, output, position, arrayAdd.length);
		System.arraycopy(arrayMain, position, output, position + arrayAdd.length, arrayMain.length - position);
		
		return output;
	}

}
