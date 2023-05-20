package fencingBotsSim;

/*
 * TODO
 * - make base not square?
 * - try angular damping on blade ( / arm ?)
 */

import org.ode4j.ode.OdeHelper;

import processing.core.PApplet;
import processing.core.PGraphics;

public class FencingBotsSim extends PApplet {

	PGraphics worldWindow;
	PGraphics uiWindow;

	private EvolutionEnvironment evolutionEnvironment;
	
	private static int TICKS = 20;
	private static boolean DRAW_SIM = true;


	public void settings() {
		size(1920, 1080, P3D);
	}


	public void setup() {
		Bridge.p = this;
		
		worldWindow = createGraphics(width * 2 / 3, height, P3D);
		uiWindow = createGraphics(width / 3, height);

		OdeHelper.initODE2(0);

		this.evolutionEnvironment = new EvolutionEnvironment();
		
		frameRate(200);
	}


	public void draw() {
		for (int i = 0; i < TICKS; i++) {
			evolutionEnvironment.update();	
		}
		
		if (DRAW_SIM) {
			worldWindow.beginDraw();
			worldWindow.background(0);
			worldWindow.camera(10, 10, 10, 0, 0, 0, 0, 0, -1);
			worldWindow.perspective(PI / 3.0f, width / height, 1, 500);
			evolutionEnvironment.renderWorld(worldWindow);
			worldWindow.endDraw();	
		}
		

		uiWindow.beginDraw();
		uiWindow.background(128);
		uiWindow.fill(0);

		evolutionEnvironment.renderUI(this, uiWindow);

		uiWindow.endDraw();

		image(worldWindow, 0, 0);
		image(uiWindow, width * 2 / 3, 0);
	}


	public void stop() {
		OdeHelper.closeODE();
	}
	
	public void keyPressed() {
		if (key == 'd') {
			DRAW_SIM = ! DRAW_SIM;
		}
		
		if (key == 's') {
			if (TICKS == 20) {
				TICKS = 1;
			}
			else if (TICKS == 1) {
				TICKS = 20;
			}
		}
		
	}


	public static void main(String[] args) {
		PApplet.main(new String[] { fencingBotsSim.FencingBotsSim.class.getName() });
	}
}
