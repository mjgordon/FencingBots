package sim;

import org.ode4j.math.DMatrix3;
import org.ode4j.math.DVector3;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DContactBuffer;
import org.ode4j.ode.DContactJoint;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DGeom.DNearCallback;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;

import nn.NNGenome;

import org.ode4j.ode.OdeConstants;

import processing.core.PGraphics;
import processing.core.PVector;

public class World {
	public Ground ground;
	public Bot botA;
	public Bot botB;

	public DWorld dWorld;
	public DSpace dSpace;

	public DJointGroup contactGroup;

	private boolean drawAxes = false;

	private int matchLifespan = 1000;
	private int matchTime = 0;

	public static final float STEPSIZE = 0.01f;
	private static final int MAX_CONTACTS = 8;

	public boolean isFinished = false;

	public int scoreA = 0;
	public int scoreB = 0;


	public World(NNGenome a, NNGenome b) {
		dWorld = OdeHelper.createWorld();
		dSpace = OdeHelper.createHashSpace();
		dWorld.setERP(0.2);
		dWorld.setCFM(1e-10);
		dWorld.setGravity(new DVector3(0, 0, -9.81));

		contactGroup = OdeHelper.createJointGroup();
		
		double d = 0.25;
		double dxA = Util.randomRange(-d,d);
		double dyA = Util.randomRange(-d,d);
		double dxB = Util.randomRange(-d,d);
		double dyB = Util.randomRange(-d,d);

		this.ground = new Ground(dWorld, dSpace);
		this.botA = new Bot(a, dWorld, dSpace, new DMatrix3(1, 0, 0, 0, 1, 0, 0, 0, 1), -3 + dxA, 0 + dyA, 0.5);
		this.botB = new Bot(b, dWorld, dSpace, new DMatrix3(-1, 0, 0, 0, -1, 0, 0, 0, 1), 3 + dxB, 0 + dyB, 0.5);

	}


	public void update() {
		botA.update(this, botB);
		botB.update(this, botA);

		dSpace.collide(null, nearCallback);
		// dWorld.step(STEPSIZE);
		dWorld.quickStep(STEPSIZE); // Apparently this keeps it from exploding?
		contactGroup.empty();

		matchTime += 1;

		

		// Check for out of bounds
		if (botA.nn.currentValues[0][0] > 1) {
			scoreB = 1;
		}
		if (botB.nn.currentValues[0][0] > 1) {
			scoreA = 1;
		}
		
		// Check for facing away
		double facingA = Math.abs(botA.transform(botB.partBase.getPosition()).heading() / Math.PI);
		double facingB = Math.abs(botB.transform(botA.partBase.getPosition()).heading() / Math.PI);
		
		//System.out.println(facingA + " : " + facingB);
		double maxAngle = 0.95;
		if (facingA > maxAngle) {
			scoreB = 1;
		}
		if (facingB > maxAngle) {
			scoreA = 1;
		}
		
		
		// Match ending conditions

		if (scoreA > 0 || scoreB > 0) {
			isFinished = true;
		}
		
		if (matchTime > matchLifespan) {
			isFinished = true;
		}

	}


	public void render(PGraphics g) {
		if (drawAxes) {
			drawAxes(g);
		}
		
		drawGrid(g);

		ground.render(g);
		botA.render(g, true);
		botB.render(g, false);
	}


	public double getTimeRatio() {
		return (1.0 * matchTime / matchLifespan);
	}


	private void drawAxes(PGraphics g) {
		g.stroke(255, 0, 0);
		g.line(0, 0, 0, 100, 0, 0);

		g.stroke(0, 255, 0);
		g.line(0, 0, 0, 0, 100, 0);

		g.stroke(0, 0, 255);
		g.line(0, 0, 0, 0, 0, 100);
	}


	private void drawGrid(PGraphics g) {
		g.stroke(200, 128);

		for (int n = -20; n <= 20; n += 2) {
			g.line(n, -20, n, 20);
			g.line(-20, n, 20, n);
		}

	}


	private DNearCallback nearCallback = new DNearCallback() {
		@Override
		public void call(Object data, DGeom o1, DGeom o2) {
			nearCallback(data, o1, o2);
		}
	};


	private void nearCallback(Object data, DGeom o1, DGeom o2) {
		int i;
		// if (o1->body && o2->body) return;

		// exit without doing anything if the two bodies are connected by a joint
		DBody b1 = o1.getBody();
		DBody b2 = o2.getBody();
		if (b1 != null && b2 != null && OdeHelper.areConnectedExcluding(b1, b2, DContactJoint.class)) {
			return;
		}

		// dContact[] contact=new dContact[MAX_CONTACTS]; // up to MAX_CONTACTS contacts
		// per box-box
		DContactBuffer contacts = new DContactBuffer(MAX_CONTACTS);
		for (DContact contact : contacts) {
			// contact.surface.mode = OdeConstants.dContactSoftCFM |
			// OdeConstants.dContactSoftERP | OdeConstants.dContactBounce;
			contact.surface.mode = OdeConstants.dContactBounce;
			// contact.surface.soft_cfm = 0.001;
			// contact.surface.soft_erp = 0.001;
			contact.surface.bounce = 0.1;
			contact.surface.bounce_vel = 0.1;

			// contact.surface.mu = OdeConstants.dInfinity;
			contact.surface.mu = 0.1;
			contact.surface.mu2 = 0;

		}
		// if (int numc = dCollide (o1,o2,MAX_CONTACTS,&contact[0].geom,
		// sizeof(dContact))) {
		int numc = OdeHelper.collide(o1, o2, MAX_CONTACTS, contacts.getGeomBuffer());// , sizeof(dContact));
		if (numc != 0) {
			DMatrix3 RI = new DMatrix3();
			RI.setIdentity();
			// final DVector3 ss = new DVector3(0.02, 0.02, 0.02);
			for (i = 0; i < numc; i++) {
				DJoint c = OdeHelper.createContactJoint(dWorld, contactGroup, contacts.get(i));
				c.attach(b1, b2);
				/*
				 * if (showContacts) { dsSetColor(0,0,1); dsDrawBox
				 * (contacts.get(i).geom.pos,RI,ss); }
				 */

			}

			// Check for falling over
			if (o1.equals(ground.groundBox)) {
				if (botA.isGroundable(o2)) {
					scoreB = 1;
				}
				else if (botB.isGroundable(o2)) {
					scoreA = 1;
				}
			}
			else if (o2.equals(ground.groundBox)) {
				if (botA.isGroundable(o1)) {
					scoreB = 1;
				}
				else if (botB.isGroundable(o1)) {
					scoreA = 1;
				}
			}

			// Check for touches
			if (o1.equals(botA.partBlade.geom)) {
				if (botB.isScorable(o2)) {
					scoreA = 2;
				}
			}
			if (o1.equals(botB.partBlade.geom)) {
				if (botA.isScorable(o2)) {
					scoreB = 2;
				}
			}
			
			if (o2.equals(botA.partBlade.geom)) {
				if (botB.isScorable(o1)) {
					scoreA = 2;
				}
			}
			if (o2.equals(botB.partBlade.geom)) {
				if (botA.isScorable(o1)) {
					scoreB = 2;
				}
			}


		}
	}
}
