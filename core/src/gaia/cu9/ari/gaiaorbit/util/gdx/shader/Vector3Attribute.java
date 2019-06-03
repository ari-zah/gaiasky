/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.shader;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.NumberUtils;

public class Vector3Attribute extends Attribute {
    public Vector3Attribute(long type) {
        super(type);
    }

    public Vector3Attribute(long type, Vector3 value) {
        super(type);
        this.value = value;
    }

    public Vector3 value;

    public static final String PlanetPosAlias = "planetPos";
    public static final long PlanetPos = register(PlanetPosAlias);

    public static final String LightPosAlias = "lightPos";
    public static final long LightPos = register(LightPosAlias);

    public static final String LightPositionAlias = "lightPosition";
    public static final long LightPosition = register(LightPositionAlias);

    public static final String CameraPosAlias = "cameraPos";
    public static final long CameraPos = register(CameraPosAlias);

    public static final String InvWavelengthAlias = "invWavelength";
    public static final long InvWavelength = register(InvWavelengthAlias);

    public static final String CameraUpAlias = "cameraUp";
    public static final long CameraUp = register(CameraUpAlias);

    public static final String VelDirAlias = "velDir";
    public static final long VelDir = register(VelDirAlias);

    public static final String GwAlias = "gw";
    public static final long Gw = register(GwAlias);

    @Override
    public Attribute copy() {
        return new Vector3Attribute(type, value);
    }

    @Override
    public int hashCode() {
        int result = (int) type;
        result = 977 * result + NumberUtils.floatToRawIntBits(value.x) + NumberUtils.floatToRawIntBits(value.y) + NumberUtils.floatToRawIntBits(value.z);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int compareTo(Attribute o) {
        return 0;
    }
}
