/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import gaiasky.util.coord.AstroUtils;

import java.time.Instant;

/**
 * Orbit on the ecliptic plane.
 *
 * @author tsagrista
 */
public class EclipticOrbit extends Orbit {
    double angle;

    public EclipticOrbit() {
        super();
    }

    /**
     * Update the local transform with the transform and the rotations/scales necessary.
     * Override if your model contains more than just the position and size.
     */
    @Override
    protected void updateLocalTransform(Instant date) {
        translation.getMatrix(localTransformD).rotate(0, 0, 1, AstroUtils.obliquity(AstroUtils.getJulianDate(date)));
    }
}