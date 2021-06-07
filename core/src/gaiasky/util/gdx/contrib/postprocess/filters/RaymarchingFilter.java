/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gaiasky.util.Logger;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

/**
 * Raymarching filter.
 *
 
 */
public final class RaymarchingFilter extends Filter3<RaymarchingFilter> {
    private final Vector2 viewport;
    private final Vector2 zfark;
    private final Vector3 pos;
    private final float[] additional;
    private Matrix4 frustumCorners;
    private final Matrix4 camInvView;
    private final Matrix4 modelView;
    private float timeSecs;
    /**
     * Default depth buffer texture. In our case, it contains the logarithmic
     * depth buffer data.
     */
    private Texture depthTexture;

    /**
     * Additional texture, to be used for any purpose
     */
    private Texture additionalTexture;

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        TextureDepth("u_texture1", 0),
        TextureAdditional("u_texture2", 0),
        Time("u_time", 1),
        Viewport("u_viewport", 2),
        ZfarK("u_zfark", 2),
        Pos("u_pos", 3),
        CamInvView("u_camInvViewTransform", 16),
        ModelView("u_modelView", 16),
        FrustumCorners("u_frustumCorners", 16),
        Additional("u_additional", 4);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String mnemonic, int arrayElementSize) {
            this.mnemonic = mnemonic;
            this.elementSize = arrayElementSize;
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


    /**
     * Creates a filter with the given viewport size
     *
     * @param fragmentShader The name of the fragment shader file, without extension
     * @param viewportWidth  The viewport width in pixels.
     * @param viewportHeight The viewport height in pixels.
     */
    public RaymarchingFilter(String fragmentShader, int viewportWidth, int viewportHeight) {
        this(fragmentShader, new Vector2((float) viewportWidth, (float) viewportHeight));
    }

    /**
     * Creates a filter with the given viewport size.
     *
     * @param fragmentShader Name of fragment shader file without extension
     * @param viewportSize   The viewport size in pixels.
     */
    public RaymarchingFilter(String fragmentShader, Vector2 viewportSize) {
        super(ShaderLoader.fromFile("raymarching/screenspace", fragmentShader));
        this.viewport = viewportSize;
        this.zfark = new Vector2();
        this.pos = new Vector3();
        this.frustumCorners = new Matrix4();
        this.camInvView = new Matrix4();
        this.modelView = new Matrix4();
        this.additional = new float[4];
        rebind();
    }

    public void setFrustumCorners(Matrix4 fc) {
        this.frustumCorners = fc;
        setParam(Param.FrustumCorners, this.frustumCorners);
    }

    public void setCaminvView(Matrix4 civ) {
        this.camInvView.set(civ);
        setParam(Param.CamInvView, this.camInvView);
    }

    public void setModelView(Matrix4 mv) {
        this.modelView.set(mv);
        setParam(Param.ModelView, this.modelView);
    }

    public void setPos(Vector3 pos) {
        this.pos.set(pos);
        setParam(Param.Pos, this.pos);
    }

    public void setTime(float seconds) {
        this.timeSecs = seconds;
        setParam(Param.Time, timeSecs);
    }

    public void setViewportSize(float width, float height) {
        this.viewport.set(width, height);
        setParam(Param.Viewport, this.viewport);
    }

    public void setZfarK(float zfar, float k) {
        this.zfark.set(zfar, k);
        setParam(Param.ZfarK, this.zfark);
    }

    public void setDepthTexture(Texture tex) {
        this.depthTexture = tex;
        setParam(Param.TextureDepth, u_texture1);
    }

    public void setAdditionalTexture(Texture tex) {
        this.additionalTexture = tex;
        setParam(Param.TextureAdditional, u_texture2);
    }

    public void setAdditional(float[] additional) {
        int len = Math.min(additional.length, 4);
        System.arraycopy(additional, 0, this.additional, 0, len);
        setParamv(Param.Additional, this.additional, 0, 4);
    }

    public void setAdditional(float a, float b, float c, float d) {
        this.additional[0] = a;
        this.additional[1] = b;
        this.additional[2] = c;
        this.additional[3] = d;
        setParamv(Param.Additional, this.additional, 0, 4);
    }

    public void setAdditional(int index, float value) {
        if (index >= 0 && index < 4) {
            this.additional[index] = value;
            setParamv(Param.Additional, this.additional, 0, 4);
        } else {
            Logger.getLogger(RaymarchingFilter.class).error("Additional index must be in [0, 3]: " + index);
        }
    }

    public Vector2 getViewportSize() {
        return viewport;
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.TextureDepth, u_texture1);
        setParams(Param.TextureAdditional, u_texture2);
        setParams(Param.Viewport, viewport);
        setParams(Param.ZfarK, zfark);
        setParams(Param.FrustumCorners, frustumCorners);
        setParams(Param.CamInvView, camInvView);
        setParams(Param.ModelView, this.modelView);
        setParams(Param.Pos, this.pos);
        setParams(Param.Time, timeSecs);
        setParamsv(Param.Additional, this.additional, 0, 4);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        if (inputTexture != null && depthTexture != null) {
            inputTexture.bind(u_texture0);
            depthTexture.bind(u_texture1);
        }
        if (additionalTexture != null)
            additionalTexture.bind(u_texture2);
    }
}
