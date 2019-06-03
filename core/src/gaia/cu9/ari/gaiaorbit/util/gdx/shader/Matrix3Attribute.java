/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.shader;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.utils.NumberUtils;

public class Matrix3Attribute extends Attribute {
    public Matrix3Attribute(long type) {
        super(type);
        this.value = new Matrix3();
    }

    public Matrix3Attribute(long type, Matrix3 value) {
        super(type);
        this.value = value;
    }

    public Matrix3 value;

    public static final String Gwmat3Alias = "gwmat3";
    public static final long Gwmat3 = register(Gwmat3Alias);

    public void set(Matrix3 value) {
        this.value.set(value);
    }

    @Override
    public Attribute copy() {
        return new Matrix3Attribute(type, value);
    }

    @Override
    public int hashCode() {
        int result = (int) type;
        result = 977 * result + NumberUtils.floatToRawIntBits(value.val[Matrix3.M00]) + NumberUtils.floatToRawIntBits(value.val[Matrix3.M11]) + NumberUtils.floatToRawIntBits(value.val[Matrix3.M22]);
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

