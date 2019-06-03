/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.shader;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.utils.NumberUtils;

public class Vector4Attribute extends Attribute {
    public Vector4Attribute(long type) {
        super(type);
    }

    public Vector4Attribute(long type, float[] value) {
        super(type);
        this.value = value;
    }

    public float[] value;

    public static final String HtermsAlias = "hterms";
    public static final long Hterms = register(HtermsAlias);

    @Override
    public Attribute copy() {
        return new Vector4Attribute(type, value);
    }

    @Override
    public int hashCode() {
        int result = (int) type;
        result = 977 * result + NumberUtils.floatToRawIntBits(value[0]) + NumberUtils.floatToRawIntBits(value[1]) + NumberUtils.floatToRawIntBits(value[2]) + NumberUtils.floatToRawIntBits(value[3]);
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
