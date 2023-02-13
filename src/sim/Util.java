package sim;

import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DGeom;

import processing.core.PMatrix3D;
import processing.core.PVector;

public class Util {
	public static DVector3 sphericalToCartesian(DVector3 in) {
		return (sphericalToCartesian(in.get0(), in.get1(), in.get2()));
	}


	public static DVector3 sphericalToCartesian(double radius, double inclination, double azimuth) {
		double x = radius * Math.sin(inclination) * Math.cos(azimuth);
		double y = radius * Math.sin(inclination) * Math.sin(azimuth);
		double z = radius * Math.cos(inclination);

		return (new DVector3(x, y, z));
	}


	public static DVector3 cartesianToSpherical(DVector3 in) {
		return (cartesianToSpherical(in.get0(), in.get1(), in.get2()));
	}


	public static DVector3 cartesianToSpherical(double x, double y, double z) {
		double r = Math.sqrt((x * x) + (y * y) + (z * z));
		double i = Math.acos(z / r);
		double a = Math.atan2(y, x);
		return (new DVector3(r, i, a));
	}


	public static PMatrix3D pmFromOM(DMatrix3C m, DVector3C v) {
		return new PMatrix3D((float) m.get00(), (float) m.get01(), (float) m.get02(), (float) v.get0(), (float) m.get10(), (float) m.get11(), (float) m.get12(), (float) v.get1(),
				(float) m.get20(), (float) m.get21(), (float) m.get22(), (float) v.get2(), 0, 0, 0, 1);
	}


	public static PVector pVFromOV(DVector3C d) {
		return new PVector((float) d.get0(), (float) d.get1(), (float) d.get2());
	}
	
	public static double randomRange(double low, double high) {
		double diff = high - low;
		return Math.random() * diff + low;
	}
	
	public static boolean isEither(DGeom test, DGeom a, DGeom b) {
		return test.equals(a) || test.equals(b);
	}
}
