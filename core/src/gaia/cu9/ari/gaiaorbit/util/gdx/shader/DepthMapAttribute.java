/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.shader;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;

public class DepthMapAttribute extends Attribute {
    public static final String ShadowTextureAlias = "shadowTexture";
    public static final long ShadowTexture = register(ShadowTextureAlias);

    public final TextureDescriptor<Texture> textureDescription;

    public DepthMapAttribute(long type) {
        super(type);
        textureDescription = new TextureDescriptor<>();
    }

    public DepthMapAttribute(long type, Texture tex) {
        super(type);
        textureDescription = new TextureDescriptor<>(tex);
    }

    public DepthMapAttribute(final DepthMapAttribute copyFrom) {
        this(copyFrom.type, copyFrom.textureDescription.texture);
    }

    public void set(Texture tex) {
        textureDescription.texture = tex;
    }

    @Override
    public Attribute copy() {
        return new DepthMapAttribute(this);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    /** TODO Implement this **/
    @Override
    public int compareTo(Attribute o) {
        return 0;
    }

}
