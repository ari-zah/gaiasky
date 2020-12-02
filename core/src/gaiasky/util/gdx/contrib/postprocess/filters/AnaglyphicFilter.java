/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/**
 * Anaglyphic 3D red-cyan filter
 *
 * @author tsagrista
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public final class AnaglyphicFilter extends Filter<AnaglyphicFilter> {

    private Texture textureLeft, textureRight;

    public enum Param implements Parameter {
        // @formatter:off
        TextureLeft("u_texture0", 0),
        TextureRight("u_texture1", 0);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String m, int elementSize) {
            this.mnemonic = m;
            this.elementSize = elementSize;
        }

        @Override
        public String mnemonic() {
            return this.mnemonic;
        }

        @Override
        public int arrayElementSize() {
            return this.elementSize;
        }
    }

    public AnaglyphicFilter() {
        super(ShaderLoader.fromFile("screenspace", "anaglyphic"));
        rebind();
    }

    @Override
    protected void onBeforeRender() {
        //inputTexture.bind(u_texture0);
        if (textureLeft != null)
            textureLeft.bind(u_texture0);
        if (textureRight != null)
            textureRight.bind(u_texture1);
    }

    public void setTextureLeft(Texture tex) {
        textureLeft = tex;
        setParam(Param.TextureLeft, u_texture0);
    }

    public void setTextureRight(Texture tex) {
        textureRight = tex;
        setParam(Param.TextureRight, u_texture1);
    }

    @Override
    public void rebind() {
        setParams(Param.TextureLeft, u_texture0);
        setParams(Param.TextureRight, u_texture1);

        endParams();
    }
}
