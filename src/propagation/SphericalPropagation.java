package propagation;

import pamMaths.PamVector;
import Array.ArrayManager;
import PamUtils.LatLong;

/**
 * Simple spherical propagation model to use with the simulated acquisition model. 
 * @author Doug Gillespie
 *
 */
public class SphericalPropagation extends LogLawPropagation {


	public SphericalPropagation() {
		super(20.);
	}

	@Override
	public String getName() {
		return "Spherical Propagation";
	}

}
