/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import gaiasky.scenegraph.ModelBody;
import gaiasky.scenegraph.Orbit;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.component.RotationComponent;
import gaiasky.util.Constants;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.time.Instant;

/**
 * A position that never changes
 * 
 * @author Toni Sagrista
 *
 */
public class StaticParentRotationCoordinates implements IBodyCoordinates {

    ModelBody parent;
    Vector3d position;
    Matrix4d trf;

    public StaticParentRotationCoordinates() {
        super();
        trf = new Matrix4d();
    }

    @Override
    public void doneLoading(Object... params) {
        SceneGraphNode me = (SceneGraphNode) params[1];
        if (me.parent != null && me.parent instanceof ModelBody) {
            parent = (ModelBody) me.parent;
        }
    }

    @Override
    public Vector3b getEclipticSphericalCoordinates(Instant date, Vector3b out) {
        return getEquatorialCartesianCoordinates(date, out);
    }

    @Override
    public Vector3b getEclipticCartesianCoordinates(Instant date, Vector3b out) {
        return getEquatorialCartesianCoordinates(date, out);
    }

    @Override
    public Vector3b getEquatorialCartesianCoordinates(Instant date, Vector3b out) {
        out.set(position);
        RotationComponent rc = parent.rc;
        if (rc != null) {
            out.rotate((float) rc.ascendingNode, 0, 1, 0).rotate((float) (rc.inclination + rc.axialTilt), 0, 0, 1).rotate((float) rc.angle, 0, 1, 0);
        }

        return out;
    }

    public void setPosition(double[] position) {
        this.position = new Vector3d(position[0] * Constants.KM_TO_U, position[1] * Constants.KM_TO_U, position[2] * Constants.KM_TO_U);
    }

    @Override
    public Orbit getOrbitObject() {
        return null;
    }

}
