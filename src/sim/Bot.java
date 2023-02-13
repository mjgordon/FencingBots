package sim;

import java.util.ArrayList;

import org.ode4j.math.DMatrix3;
import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DFixedJoint;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DHinge2Joint;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DSphere;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;

import nn.NNGenome;
import nn.NeuralNetwork;
import processing.core.PGraphics;
import processing.core.PMatrix3D;
import processing.core.PVector;
import util.DoubleAveraged;

public class Bot {
	Part[] partFeet;
	Part partBase;
	Part partTorso;
	Part partHead;
	Part partArm;
	Part partBlade;

	DHinge2Joint torsoHead;
	DHinge2Joint torsoArm;
	DHinge2Joint armBlade;

	public static final float DENSITY = 5;

	public NeuralNetwork nn;
	
	private DoubleAveraged[] averageMemory;
	
	public static final int[] nnLayerSizes = { 42, 10, 10, 10, 9 };
	
	public static final int botAColor = 0xFF00FF00;
	public static final int botBColor = 0xFFFFAA00;


	public Bot(NNGenome genome, DWorld dWorld, DSpace dSpace, DMatrix3 rotation, double px, double py, double pz) {
		DVector3 offset = new DVector3(px, py, pz);

		partFeet = new Part[4];
		double o = 0.3;
		partFeet[0] = new Part(dWorld, dSpace, 0.2, o, o, 0, rotation, offset);
		partFeet[1] = new Part(dWorld, dSpace, 0.2, -o, o, 0, rotation, offset);
		partFeet[2] = new Part(dWorld, dSpace, 0.2, -o, -o, 0, rotation, offset);
		partFeet[3] = new Part(dWorld, dSpace, 0.2, o, -o, 0, rotation, offset);

		partBase = new Part(dWorld, dSpace, 0.8, 0.8, 0.125, 0, 0, 0, rotation, offset, 1, false);
		partTorso = new Part(dWorld, dSpace, 0.25, 0.25, 2, 0, 0, 1.02, rotation, offset, 1, false);
		partHead = new Part(dWorld, dSpace, 0.3, 0.3, 0.6, 0, 0, 2.3, rotation, offset, 1, false);
		partArm = new Part(dWorld, dSpace, 1, 0.1, 0.1, 0.5, 0, 1.8, rotation, offset, 1, true);
		partBlade = new Part(dWorld, dSpace, 1, 0.1, 0.1, 1.5, 0, 1.8, rotation, offset, 0.1, true);

		DFixedJoint baseTorso = OdeHelper.createFixedJoint(dWorld);
		baseTorso.attach(partBase.body, partTorso.body);
		baseTorso.setFixed();

		DFixedJoint[] feetBase = new DFixedJoint[4];
		for (int i = 0; i < 4; i++) {
			feetBase[i] = OdeHelper.createFixedJoint(dWorld);
			feetBase[i].attach(partFeet[i].body, partBase.body);
			feetBase[i].setFixed();
		}

		DVector3 rotationY = new DVector3(rotation.get01(), rotation.get11(), rotation.get21());

		torsoHead = OdeHelper.createHinge2Joint(dWorld);
		torsoHead.attach(partTorso.body, partHead.body);
		torsoHead.setAnchor(transform(0, 0, 2, rotation, offset));
		torsoHead.setAxis1(0, 0, 1);
		torsoHead.setAxis2(rotationY);
		torsoHead.setParamLoStop(-Math.PI / 2);
		torsoHead.setParamHiStop(Math.PI / 2);

		torsoArm = OdeHelper.createHinge2Joint(dWorld);
		torsoArm.attach(partTorso.body, partArm.body);
		torsoArm.setAnchor(transform(0, 0, 1.8, rotation, offset));
		torsoArm.setAxis1(0, 0, 1);
		torsoArm.setAxis2(rotationY);
		torsoArm.setParamLoStop(-Math.PI / 2);
		torsoArm.setParamHiStop(Math.PI / 2);

		armBlade = OdeHelper.createHinge2Joint(dWorld);
		armBlade.attach(partArm.body, partBlade.body);
		armBlade.setAnchor(transform(1, 0, 1.8, rotation, offset));
		armBlade.setAxis1(0, 0, 1);
		armBlade.setAxis2(rotationY);
		armBlade.setParamLoStop(-Math.PI / 2);
		armBlade.setParamHiStop(Math.PI / 2);

		
		if (genome == null) {
			System.out.println("Null genome supplied");
			System.exit(1);
		}
		
		nn = new NeuralNetwork(nnLayerSizes , genome);
		
		
		averageMemory = new DoubleAveraged[9];
		for (int i = 0; i < averageMemory.length; i++) {
			averageMemory[i] = new DoubleAveraged();
		}
	}


	public void update(World world, Bot opponent) {
		double[] output = nn.calculate(assembleInput(world, opponent));

		double f = 3;
		partBase.body.addRelForce(output[0] * f, output[1] * f, 0);
		partBase.body.addRelTorque(0, 0, output[2]);

		double hf = 1;
		double headDiff1 = (output[3] * (Math.PI / 2)) - torsoHead.getAngle1();
		double headDiff2 = (output[4] * (Math.PI / 2)) - torsoHead.getAngle2();
		torsoHead.addTorques(headDiff1 * hf, -headDiff2 * hf);

		double af = 1.5;
		double armDiff1 = (output[5] * (Math.PI / 2)) - torsoArm.getAngle1();
		double armDiff2 = (output[6] * (Math.PI / 2)) - torsoArm.getAngle2();
		torsoArm.addTorques(armDiff1 * af, -armDiff2 * af);

		double bf = 1;
		double bladeDiff1 = (output[7] * (Math.PI / 2)) - armBlade.getAngle1();
		double bladeDiff2 = (output[8] * (Math.PI / 2)) - armBlade.getAngle2();
		armBlade.addTorques(bladeDiff1 * bf, -bladeDiff2 * bf);

	}


	public void render(PGraphics g, boolean isBotA) {
		g.fill(255);
		g.stroke(0);
		renderShape(g, partFeet[0]);
		renderShape(g, partFeet[1]);
		renderShape(g, partFeet[2]);
		renderShape(g, partFeet[3]);
		renderShape(g, partBase);
		renderShape(g, partTorso);
		renderShape(g, partHead);
		renderShape(g, partArm);

		if (isBotA) {
			g.fill(botAColor);	
		}
		else {
			g.fill(botBColor);
		}
		
		renderShape(g, partBlade);
	}
	
	public boolean isGroundable(DGeom in) {
		return(in.equals(partTorso.geom)|| in.equals(partHead.geom) || in.equals(partArm.geom));
	}
	
	public boolean isScorable(DGeom in) {
		return (in.equals(partTorso.geom) || in.equals(partHead.geom));
		
	}


	private void renderShape(PGraphics g, Part part) {
		DVector3C pos = part.geom.getPosition();
		DMatrix3C rot = part.geom.getRotation();

		g.pushMatrix();
		{
			PMatrix3D m = new PMatrix3D((float) rot.get00(), (float) rot.get01(), (float) rot.get02(), (float) pos.get0(), (float) rot.get10(), (float) rot.get11(),
					(float) rot.get12(), (float) pos.get1(), (float) rot.get20(), (float) rot.get21(), (float) rot.get22(), (float) pos.get2(), 0, 0, 0, 1);
			g.applyMatrix(m);

			if (part.box) {

				DBox box = (DBox) part.geom;
				DVector3C size = box.getLengths();
				g.box((float) size.get0(), (float) size.get1(), (float) size.get2());
			}
			else {
				DSphere sphere = (DSphere) part.geom;
				double radius = sphere.getRadius();
				g.sphereDetail(10);
				g.sphere((float) radius);
			}

		}
		g.popMatrix();

	}


	private DVector3 transform(double x, double y, double z, DMatrix3 rotation, DVector3 offset) {
		DVector3 output = new DVector3(x, y, z);
		output.eqProd(rotation, output);
		output.add(offset);
		return output;
	}


	private double[] assembleInput(World world, Bot opponent) {
		ArrayList<Double> output = new ArrayList<Double>();
		// double[] output = new double[48];

		DVector3C groundPos = world.ground.getPosition();

		// 00 : Radius from center
		output.add(partBase.body.getPosition().distance(groundPos) / (world.ground.diameter / 2));

		// 01 : Absolute angle from center (mapping abs(0-1) to abs(0-PI))
		PVector basePosition = new PVector((float) partBase.body.getPosition().get0(), (float) partBase.body.getPosition().get1(), (float) partBase.body.getPosition().get2());
		PVector groundPosition = new PVector((float) world.ground.getPosition().get0(), (float) world.ground.getPosition().get1(), (float) world.ground.getPosition().get2());

		output.add(Math.abs(PVector.sub(groundPosition, basePosition).heading()) / Math.PI);

		// 02 : Body pitch
		DMatrix3C torsoRotation = partTorso.body.getRotation();
		DVector3 torsoX = new DVector3();
		torsoRotation.getColumn0(torsoX);
		output.add(Util.cartesianToSpherical(torsoX).get1() / Math.PI);

		// 03 : Body roll
		DVector3 torsoY = new DVector3();
		torsoRotation.getColumn1(torsoY);
		output.add(Util.cartesianToSpherical(torsoY).get1() / Math.PI);

		// 04-18 : Opponent Positions
		PMatrix3D inverter = Util.pmFromOM(partTorso.body.getRotation(), partTorso.body.getPosition());
		inverter.invert();

		PVector oppBasePosRel = inverter.mult(opponent.partBase.getPosition(), new PVector()).div(world.ground.diameter);
		PVector oppTorsoPosRel = inverter.mult(opponent.partTorso.getPosition(), new PVector()).div(world.ground.diameter);
		PVector oppHeadPosRel = inverter.mult(opponent.partHead.getPosition(), new PVector()).div(world.ground.diameter);
		PVector oppArmPosRel = inverter.mult(opponent.partArm.getPositionXEnd(), new PVector()).div(world.ground.diameter);
		PVector oppBladePosRel = inverter.mult(opponent.partBlade.getPositionXEnd(), new PVector()).div(world.ground.diameter);

		addVectorXY(output, oppBasePosRel);
		addVectorXY(output, oppTorsoPosRel);
		addVector(output, oppHeadPosRel);
		addVector(output, oppArmPosRel);
		addVector(output, oppBladePosRel);

		// 19-24 : Current relative wrist and blade tip to body
		PVector wristPosRel = inverter.mult(partArm.getPositionXEnd(), new PVector()).div(world.ground.diameter);
		PVector bladPosRel = inverter.mult(partBlade.getPositionXEnd(), new PVector()).div(world.ground.diameter);

		addVector(output, wristPosRel);
		addVector(output, bladPosRel);

		// 25-35 : Current goal
		double[] outputLayer = nn.getOutputLayer();
		for (int i = 0; i < outputLayer.length;i++) {
			output.add(outputLayer[i]);
		}

		// 36-46 : Averaged goal(currently a dummy)
		for (int i = 0; i < outputLayer.length; i++) {
			averageMemory[i].add(outputLayer[i]);
			output.add(averageMemory[i].get());
			//output.add(outputLayer[i]);
		}

		// 47 : Time
		output.add(world.getTimeRatio());

		return (output.stream().mapToDouble(Double::doubleValue).toArray());
	}


	private void addVector(ArrayList<Double> inputs, PVector v) {
		inputs.add((double) v.x);
		inputs.add((double) v.y);
		inputs.add((double) v.z);
	}
	
	private void addVectorXY(ArrayList<Double> inputs, PVector v) {
		inputs.add((double) v.x);
		inputs.add((double) v.y);
	}


	public class Part {
		public DGeom geom;
		public DBody body;
		public DMass mass;

		public boolean box;


		public Part(DWorld dWorld, DSpace dSpace, double r, double px, double py, double pz, DMatrix3 rotation, DVector3 offset) {
			body = OdeHelper.createBody(dWorld);
			body.setPosition(transform(px, py, pz, rotation, offset));
			body.setRotation(rotation);

			geom = OdeHelper.createSphere(dSpace, r);
			geom.setBody(body);
			mass = OdeHelper.createMass();
			mass.setSphere(DENSITY, ((DSphere) geom).getRadius());
			body.setMass(mass);

			box = false;
		}


		public Part(DWorld dWorld, DSpace dSpace, double lx, double ly, double lz, double px, double py, double pz, DMatrix3 rotation, DVector3 offset, double densityF, boolean damping) {
			body = OdeHelper.createBody(dWorld);
			body.setPosition(transform(px, py, pz, rotation, offset));
			body.setRotation(rotation);

			geom = OdeHelper.createBox(dSpace, lx, ly, lz);
			geom.setBody(body);
			mass = OdeHelper.createMass();
			mass.setBox(DENSITY * densityF, ((DBox) geom).getLengths());
			body.setMass(mass);

			box = true;
			
			if (damping) {
				body.setAngularDamping(0.2);
				body.setAngularDampingThreshold(0.3);
			}

		}


		PVector getPosition() {
			return Util.pVFromOV(body.getPosition());
		}


		PVector getPositionXEnd() {
			if (box) {
				PMatrix3D m = Util.pmFromOM(body.getRotation(), body.getPosition());
				PVector output = m.mult(new PVector((float) ((DBox) this.geom).getLengths().get0() / 2, 0, 0), new PVector());
				return (output);
			}
			else {
				PMatrix3D m = Util.pmFromOM(body.getRotation(), body.getPosition());
				PVector output = m.mult(new PVector((float) ((DSphere) this.geom).getRadius(), 0, 0), new PVector());
				return (output);
			}

		}
	}
	
	

}
