/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.shader;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.NumberUtils;

public class Matrix4Attribute extends Attribute {
    public Matrix4Attribute(long type) {
        super(type);
        this.value = new Matrix4();
    }

    public Matrix4Attribute(long type, Matrix4 value) {
        super(type);
        this.value = new Matrix4(value);
    }

    public Matrix4 value;

    public static final String ShadowMapProjViewTransAlias = "shadowMapProjViewTrans";
    public static final long ShadowMapProjViewTrans = register(ShadowMapProjViewTransAlias);

    public void set(Matrix4 value) {
        this.value.set(value);
    }

    @Override
    public Attribute copy() {
        return new Matrix4Attribute(type, value);
    }

    @Override
    public int hashCode() {
        int result = (int) type;
        result = 977 * result + NumberUtils.floatToRawIntBits(value.val[Matrix4.M00]) + NumberUtils.floatToRawIntBits(value.val[Matrix4.M11]) + NumberUtils.floatToRawIntBits(value.val[Matrix4.M22]) + NumberUtils.floatToRawIntBits(value.val[Matrix4.M33]);
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
