/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.shader;

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.GdxRuntimeException;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntRenderable;

public class AtmosphereShader extends BaseIntShader {
    public static class Config {
        /** The uber vertex shader to use, null to use the default vertex shader. */
        public String vertexShader = null;
        /** The uber fragment shader to use, null to use the default fragment shader. */
        public String fragmentShader = null;
        /** */
        public boolean ignoreUnimplemented = true;
        /** Set to 0 to disable culling, -1 to inherit from {@link AtmosphereShader#defaultCullFace} */
        public int defaultCullFace = -1;
        /** Set to 0 to disable depth test, -1 to inherit from {@link AtmosphereShader#defaultDepthFunc} */
        public int defaultDepthFunc = -1;

        public Config() {
        }

        public Config(final String vertexShader, final String fragmentShader) {
            this.vertexShader = vertexShader;
            this.fragmentShader = fragmentShader;
        }
    }

    public static class Inputs {
        public final static Uniform projTrans = new Uniform("u_projTrans");
        public final static Uniform viewTrans = new Uniform("u_viewTrans");
        public final static Uniform projViewTrans = new Uniform("u_projViewTrans");
        public final static Uniform cameraPosition = new Uniform("u_cameraPosition");
        public final static Uniform cameraDirection = new Uniform("u_cameraDirection");
        public final static Uniform cameraUp = new Uniform("u_cameraUp");

        public final static Uniform worldTrans = new Uniform("u_worldTrans");
        public final static Uniform viewWorldTrans = new Uniform("u_viewWorldTrans");
        public final static Uniform projViewWorldTrans = new Uniform("u_projViewWorldTrans");
        public final static Uniform normalMatrix = new Uniform("u_normalMatrix");

        public final static Uniform alpha = new Uniform("fAlpha");
        public final static Uniform cameraHeight = new Uniform("fCameraHeight");
        public final static Uniform cameraHeight2 = new Uniform("fCameraHeight2");
        public final static Uniform outerRadius = new Uniform("fOuterRadius");
        public final static Uniform outerRadius2 = new Uniform("fOuterRadius2");
        public final static Uniform innerRadius = new Uniform("fInnerRadius");
        public final static Uniform innerRadius2 = new Uniform("fInnerRadius2");
        public final static Uniform krESun = new Uniform("fKrESun");
        public final static Uniform kmESun = new Uniform("fKmESun");
        public final static Uniform kr4PI = new Uniform("fKr4PI");
        public final static Uniform km4PI = new Uniform("fKm4PI");
        public final static Uniform scale = new Uniform("fScale");
        public final static Uniform scaleDepth = new Uniform("fScaleDepth");
        public final static Uniform scaleOverScaleDepth = new Uniform("fScaleOverScaleDepth");
        public final static Uniform nSamples = new Uniform("nSamples");
        public final static Uniform fSamples = new Uniform("fSamples");
        public final static Uniform g = new Uniform("g");
        public final static Uniform g2 = new Uniform("g2");

        public final static Uniform planetPos = new Uniform("v3PlanetPos");
        public final static Uniform lightPos = new Uniform("v3LightPos");
        public final static Uniform cameraPos = new Uniform("v3CameraPos");
        public final static Uniform invWavelength = new Uniform("v3InvWavelength");

        // Since atmosphere shader does not extend default shader, we need the relativsitic and gravwaves parameters here too
        public final static Uniform vc = new Uniform("u_vc");
        public final static Uniform velDir = new Uniform("u_velDir");
        public final static Uniform hterms = new Uniform("u_hterms");
        public final static Uniform gw = new Uniform("u_gw");
        public final static Uniform gwmat3 = new Uniform("u_gwmat3");
        public final static Uniform ts = new Uniform("u_ts");
        public final static Uniform omgw = new Uniform("u_omgw");

    }

    public static class Setters {
        public final static Setter projTrans = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return true;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.projection);
            }
        };
        public final static Setter viewTrans = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return true;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.view);
            }
        };
        public final static Setter projViewTrans = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return true;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.combined);
            }
        };
        public final static Setter cameraPosition = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return true;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.position.x, shader.camera.position.y, shader.camera.position.z, 1.1881f / (shader.camera.far * shader.camera.far));
            }
        };
        public final static Setter cameraDirection = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return true;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.direction);
            }
        };
        public final static Setter cameraUp = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return true;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, shader.camera.up);
            }
        };
        public final static Setter worldTrans = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, renderable.worldTransform);
            }
        };
        public final static Setter viewWorldTrans = new Setter() {
            final Matrix4 temp = new Matrix4();

            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, temp.set(shader.camera.view).mul(renderable.worldTransform));
            }
        };
        public final static Setter projViewWorldTrans = new Setter() {
            final Matrix4 temp = new Matrix4();

            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, temp.set(shader.camera.combined).mul(renderable.worldTransform));
            }
        };
        public final static Setter normalMatrix = new Setter() {
            private final Matrix3 tmpM = new Matrix3();

            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, tmpM.set(renderable.worldTransform).inv().transpose());
            }
        };

        public final static Setter alpha = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.Alpha))).value);
            }
        };

        public final static Setter cameraHeight = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.CameraHeight))).value);
            }
        };

        public final static Setter cameraHeight2 = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.CameraHeight2))).value);
            }
        };

        public final static Setter outerRadius = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.OuterRadius))).value);
            }
        };

        public final static Setter outerRadius2 = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.OuterRadius2))).value);
            }
        };

        public final static Setter innerRadius = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.InnerRadius))).value);
            }
        };

        public final static Setter innerRadius2 = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.InnerRadius2))).value);
            }
        };

        public final static Setter krESun = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.KrESun))).value);
            }
        };

        public final static Setter kmESun = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.KmESun))).value);
            }
        };

        public final static Setter kr4PI = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.Kr4PI))).value);
            }
        };

        public final static Setter km4PI = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.Km4PI))).value);
            }
        };

        public final static Setter scale = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.Scale))).value);
            }
        };

        public final static Setter scaleDepth = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.ScaleDepth))).value);
            }
        };

        public final static Setter scaleOverScaleDepth = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.ScaleOverScaleDepth))).value);
            }
        };

        public final static Setter nSamples = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, (int) ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.nSamples))).value);
            }
        };

        public final static Setter fSamples = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.fSamples))).value);
            }
        };

        public final static Setter g = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.G))).value);
            }
        };

        public final static Setter g2 = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((AtmosphereAttribute) (combinedAttributes.get(AtmosphereAttribute.G2))).value);
            }
        };
        public final static Setter planetPos = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((Vector3Attribute) (combinedAttributes.get(Vector3Attribute.PlanetPos))).value);
            }
        };
        public final static Setter cameraPos = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((Vector3Attribute) (combinedAttributes.get(Vector3Attribute.CameraPos))).value);
            }
        };
        public final static Setter lightPos = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((Vector3Attribute) (combinedAttributes.get(Vector3Attribute.LightPos))).value);
            }
        };
        public final static Setter invWavelength = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                shader.set(inputID, ((Vector3Attribute) (combinedAttributes.get(Vector3Attribute.InvWavelength))).value);
            }
        };

        public final static Setter vc = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(RelativisticEffectFloatAttribute.Vc))
                    shader.set(inputID, ((RelativisticEffectFloatAttribute) (combinedAttributes.get(RelativisticEffectFloatAttribute.Vc))).value);
            }
        };

        public final static Setter velDir = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(Vector3Attribute.VelDir))
                    shader.set(inputID, ((Vector3Attribute) (combinedAttributes.get(Vector3Attribute.VelDir))).value);
            }
        };

        public final static Setter hterms = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(Vector4Attribute.Hterms)) {
                    float[] val = ((Vector4Attribute) (combinedAttributes.get(Vector4Attribute.Hterms))).value;
                    shader.set(inputID, val[0], val[1], val[2], val[3]);
                }
            }
        };

        public final static Setter gw = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(Vector3Attribute.Gw))
                    shader.set(inputID, ((Vector3Attribute) (combinedAttributes.get(Vector3Attribute.Gw))).value);
            }
        };

        public final static Setter gwmat3 = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(Matrix3Attribute.Gwmat3))
                    shader.set(inputID, ((Matrix3Attribute) (combinedAttributes.get(Matrix3Attribute.Gwmat3))).value);
            }
        };

        public final static Setter ts = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(RelativisticEffectFloatAttribute.Ts))
                    shader.set(inputID, ((RelativisticEffectFloatAttribute) (combinedAttributes.get(RelativisticEffectFloatAttribute.Ts))).value);
            }
        };

        public final static Setter omgw = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(RelativisticEffectFloatAttribute.Omgw))
                    shader.set(inputID, ((RelativisticEffectFloatAttribute) (combinedAttributes.get(RelativisticEffectFloatAttribute.Omgw))).value);
            }
        };

    }

    private static String defaultVertexShader = null;

    public static String getDefaultVertexShader() {
        if (defaultVertexShader == null)
            defaultVertexShader = Gdx.files.internal("shader/atm.vertex.glsl").readString();
        return defaultVertexShader;
    }

    private static String defaultFragmentShader = null;

    public static String getDefaultFragmentShader() {
        if (defaultFragmentShader == null)
            defaultFragmentShader = Gdx.files.internal("shader/atm.fragment.glsl").readString();
        return defaultFragmentShader;
    }

    protected static long implementedFlags = BlendingAttribute.Type | TextureAttribute.Diffuse | ColorAttribute.Diffuse | ColorAttribute.Specular | FloatAttribute.Shininess;

    /** @deprecated Replaced by {@link Config#defaultCullFace} Set to 0 to disable culling */
    @Deprecated
    public static int defaultCullFace = GL20.GL_BACK;
    /** @deprecated Replaced by {@link Config#defaultDepthFunc} Set to 0 to disable depth test */
    @Deprecated
    public static int defaultDepthFunc = GL20.GL_LEQUAL;

    // Global uniforms
    public final int u_projTrans;
    public final int u_viewTrans;
    public final int u_projViewTrans;
    public final int u_cameraPosition;
    public final int u_cameraDirection;
    public final int u_cameraUp;
    // Object uniforms
    public final int u_worldTrans;
    public final int u_viewWorldTrans;
    public final int u_projViewWorldTrans;
    public final int u_normalMatrix;
    // Material uniforms
    public final int fAlpha;
    public final int fCameraHeight;
    public final int fCameraHeight2;
    public final int fOuterRadius;
    public final int fOuterRadius2;
    public final int fInnerRadius;
    public final int fInnerRadius2;
    public final int fKrESun;
    public final int fKmESun;
    public final int fKr4PI;
    public final int fKm4PI;
    public final int fScale;
    public final int fScaleDepth;
    public final int fScaleOverScaleDepth;

    public final int nSamples;
    public final int fSamples;

    public final int g;
    public final int g2;

    public final int v3PlanetPos;
    public final int v3LightPos;
    public final int v3CameraPos;
    public final int v3InvWavelength;

    // Special relativity
    public final int u_vc;
    public final int u_velDir;
    // Gravitational waves
    public final int u_hterms;
    public final int u_gw;
    public final int u_gwmat3;
    public final int u_ts;
    public final int u_omgw;

    /** The renderable used to create this shader, invalid after the call to init */
    private IntRenderable renderable;
    /** The attributes that this shader supports */
    protected final long attributesMask;
    private final long vertexMask;
    protected final Config config;
    /** Material attributes which are not required but always supported. */
    private final static long optionalAttributes = IntAttribute.CullFace | DepthTestAttribute.Type;

    public AtmosphereShader(final IntRenderable renderable, final Config config) {
        this(renderable, config, createPrefix(renderable));
    }

    public AtmosphereShader(final IntRenderable renderable, final Config config, final String prefix) {
        this(renderable, config, prefix, config.vertexShader != null ? config.vertexShader : getDefaultVertexShader(), config.fragmentShader != null ? config.fragmentShader : getDefaultFragmentShader());
    }

    public AtmosphereShader(final IntRenderable renderable, final Config config, final String prefix, final String vertexShader, final String fragmentShader) {
        this(renderable, config, new ShaderProgram(ShaderProgramProvider.getShaderCode(prefix, vertexShader), ShaderProgramProvider.getShaderCode(prefix, fragmentShader)));
    }

    public AtmosphereShader(final IntRenderable renderable, final Config config, final ShaderProgram shaderProgram) {
        final Attributes attributes = combineAttributes(renderable);
        this.config = config;
        this.program = shaderProgram;
        this.renderable = renderable;
        attributesMask = attributes.getMask() | optionalAttributes;
        vertexMask = renderable.meshPart.mesh.getVertexAttributes().getMaskWithSizePacked();

        if (!config.ignoreUnimplemented && (implementedFlags & attributesMask) != attributesMask)
            throw new GdxRuntimeException("Some attributes not implemented yet (" + attributesMask + ")");

        // Global uniforms
        u_projTrans = register(Inputs.projTrans, Setters.projTrans);
        u_viewTrans = register(Inputs.viewTrans, Setters.viewTrans);
        u_projViewTrans = register(Inputs.projViewTrans, Setters.projViewTrans);
        u_cameraPosition = register(Inputs.cameraPosition, Setters.cameraPosition);
        u_cameraDirection = register(Inputs.cameraDirection, Setters.cameraDirection);
        u_cameraUp = register(Inputs.cameraUp, Setters.cameraUp);

        // Object uniforms
        u_worldTrans = register(Inputs.worldTrans, Setters.worldTrans);
        u_viewWorldTrans = register(Inputs.viewWorldTrans, Setters.viewWorldTrans);
        u_projViewWorldTrans = register(Inputs.projViewWorldTrans, Setters.projViewWorldTrans);
        u_normalMatrix = register(Inputs.normalMatrix, Setters.normalMatrix);

        fAlpha = register(Inputs.alpha, Setters.alpha);
        fCameraHeight = register(Inputs.cameraHeight, Setters.cameraHeight);
        fCameraHeight2 = register(Inputs.cameraHeight2, Setters.cameraHeight2);
        fOuterRadius = register(Inputs.outerRadius, Setters.outerRadius);
        fOuterRadius2 = register(Inputs.outerRadius2, Setters.outerRadius2);
        fInnerRadius = register(Inputs.innerRadius, Setters.innerRadius);
        fInnerRadius2 = register(Inputs.innerRadius2, Setters.innerRadius2);
        fKrESun = register(Inputs.krESun, Setters.krESun);
        fKmESun = register(Inputs.kmESun, Setters.kmESun);
        fKr4PI = register(Inputs.kr4PI, Setters.kr4PI);
        fKm4PI = register(Inputs.km4PI, Setters.km4PI);
        fScale = register(Inputs.scale, Setters.scale);
        fScaleDepth = register(Inputs.scaleDepth, Setters.scaleDepth);
        fScaleOverScaleDepth = register(Inputs.scaleOverScaleDepth, Setters.scaleOverScaleDepth);
        nSamples = register(Inputs.nSamples, Setters.nSamples);
        fSamples = register(Inputs.fSamples, Setters.fSamples);

        g = register(Inputs.g, Setters.g);
        g2 = register(Inputs.g2, Setters.g2);

        v3PlanetPos = register(Inputs.planetPos, Setters.planetPos);
        v3CameraPos = register(Inputs.cameraPos, Setters.cameraPos);
        v3LightPos = register(Inputs.lightPos, Setters.lightPos);
        v3InvWavelength = register(Inputs.invWavelength, Setters.invWavelength);

        u_vc = register(Inputs.vc, Setters.vc);
        u_velDir = register(Inputs.velDir, Setters.velDir);

        u_hterms = register(Inputs.hterms, Setters.hterms);
        u_gw = register(Inputs.gw, Setters.gw);
        u_gwmat3 = register(Inputs.gwmat3, Setters.gwmat3);
        u_ts = register(Inputs.ts, Setters.ts);
        u_omgw = register(Inputs.omgw, Setters.omgw);

    }

    @Override
    public void init() {
        final ShaderProgram program = this.program;
        this.program = null;
        init(program, renderable);
        renderable = null;

    }

    private final static Attributes tmpAttributes = new Attributes();

    // TODO: Perhaps move responsibility for combining attributes to IntRenderableProvider?
    private static final Attributes combineAttributes(final IntRenderable renderable) {
        tmpAttributes.clear();
        if (renderable.environment != null)
            tmpAttributes.set(renderable.environment);
        if (renderable.material != null)
            tmpAttributes.set(renderable.material);
        return tmpAttributes;
    }

    private static final long combineAttributeMasks(final IntRenderable renderable) {
        long mask = 0;
        if (renderable.environment != null)
            mask |= renderable.environment.getMask();
        if (renderable.material != null)
            mask |= renderable.material.getMask();
        return mask;
    }

    public static String createPrefix(final IntRenderable renderable) {
        final Attributes attributes = combineAttributes(renderable);
        String prefix = "";
        final long attributesMask = attributes.getMask();
        // Atmosphere ground only if camera height is set
        if ((attributesMask & RelativisticEffectFloatAttribute.Vc) == RelativisticEffectFloatAttribute.Vc)
            prefix += "#define relativisticEffects\n";
        // Gravitational waves
        if ((attributesMask & RelativisticEffectFloatAttribute.Omgw) == RelativisticEffectFloatAttribute.Omgw)
            prefix += "#define gravitationalWaves\n";
        return prefix;
    }

    @Override
    public boolean canRender(final IntRenderable renderable) {
        final long renderableMask = combineAttributeMasks(renderable);
        return (attributesMask == (renderableMask | optionalAttributes)) && (vertexMask == renderable.meshPart.mesh.getVertexAttributes().getMaskWithSizePacked());
    }

    @Override
    public int compareTo(IntShader other) {
        if (other == null)
            return -1;
        if (other == this)
            return 0;
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof AtmosphereShader) ? equals((AtmosphereShader) obj) : false;
    }

    public boolean equals(AtmosphereShader obj) {
        return (obj == this);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public void begin(final Camera camera, final RenderContext context) {
        super.begin(camera, context);

    }

    @Override
    public void render(final IntRenderable renderable) {
        if (!renderable.material.has(BlendingAttribute.Type))
            context.setBlending(false, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        bindMaterial(renderable);
        super.render(renderable);
    }

    @Override
    public void end() {
        currentMaterial = null;
        super.end();
    }

    Material currentMaterial;

    protected void bindMaterial(final IntRenderable renderable) {
        if (currentMaterial == renderable.material)
            return;

        int cullFace = config.defaultCullFace == -1 ? defaultCullFace : config.defaultCullFace;
        int depthFunc = config.defaultDepthFunc == -1 ? defaultDepthFunc : config.defaultDepthFunc;
        float depthRangeNear = 0f;
        float depthRangeFar = 1f;
        boolean depthMask = true;

        currentMaterial = renderable.material;
        for (final Attribute attr : currentMaterial) {
            final long t = attr.type;
            if (BlendingAttribute.is(t)) {
                context.setBlending(true, ((BlendingAttribute) attr).sourceFunction, ((BlendingAttribute) attr).destFunction);
            } else if ((t & IntAttribute.CullFace) == IntAttribute.CullFace)
                cullFace = ((IntAttribute) attr).value;
            else if ((t & DepthTestAttribute.Type) == DepthTestAttribute.Type) {
                DepthTestAttribute dta = (DepthTestAttribute) attr;
                depthFunc = dta.depthFunc;
                depthRangeNear = dta.depthRangeNear;
                depthRangeFar = dta.depthRangeFar;
                depthMask = dta.depthMask;
            } else if (!config.ignoreUnimplemented)
                throw new GdxRuntimeException("Unknown material attribute: " + attr.toString());
        }

        context.setCullFace(cullFace);
        context.setDepthTest(depthFunc, depthRangeNear, depthRangeFar);
        context.setDepthMask(depthMask);
    }

    @Override
    public void dispose() {
        program.dispose();
        super.dispose();
    }

    public int getDefaultCullFace() {
        return config.defaultCullFace == -1 ? defaultCullFace : config.defaultCullFace;
    }

    public void setDefaultCullFace(int cullFace) {
        config.defaultCullFace = cullFace;
    }

    public int getDefaultDepthFunc() {
        return config.defaultDepthFunc == -1 ? defaultDepthFunc : config.defaultDepthFunc;
    }

    public void setDefaultDepthFunc(int depthFunc) {
        config.defaultDepthFunc = depthFunc;
    }

}
