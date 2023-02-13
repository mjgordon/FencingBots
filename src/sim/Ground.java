package sim;

import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;

import processing.core.PGraphics;

public class Ground {
	
	public DBox groundBox;
	
	public final float diameter = 10;
	
	
	public Ground(DWorld dWorld, DSpace dSpace) {		
		groundBox = OdeHelper.createBox(dSpace, diameter + 6, diameter + 6,2);
		groundBox.setPosition(0,0,-groundBox.getLengths().get2() / 2 );
	}
	
	
	public DVector3 getPosition() {
		return(new DVector3(0,0,0));
	}
	
	
	public void render(PGraphics g) {
		DVector3C pos = groundBox.getPosition();
		DVector3C size = groundBox.getLengths();
		g.pushMatrix();
		{
			g.stroke(255);
			g.noFill();
			g.circle(0,0,diameter);
					
			g.translate((float)pos.get0(), (float)pos.get1(), (float)pos.get2());
			g.stroke(50);
			g.fill(100);
			g.box((float)size.get0(), (float)size.get1(), (float)size.get2());
		}
		g.popMatrix();
	}
}
