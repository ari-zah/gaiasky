/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.shader;

import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntRenderable;

public class GroundShader extends RelativisticShader {

    public static class Inputs extends RelativisticShader.Inputs {
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
    }

    public static class Setters extends RelativisticShader.Setters {
        public final static Setter alpha = new Setter() {
            @Override
            public boolean isGlobal(BaseIntShader shader, int inputID) {
                return false;
            }

            @Override
            public void set(BaseIntShader shader, int inputID, IntRenderable renderable, Attributes combinedAttributes) {
                if (combinedAttributes.has(AtmosphereAttribute.Alpha))
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
                if (combinedAttributes.has(AtmosphereAttribute.CameraHeight))
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
                if (combinedAttributes.has(AtmosphereAttribute.CameraHeight2))
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
                if (combinedAttributes.has(AtmosphereAttribute.OuterRadius))
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
                if (combinedAttributes.has(AtmosphereAttribute.OuterRadius2))
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
                if (combinedAttributes.has(AtmosphereAttribute.InnerRadius))
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
                if (combinedAttributes.has(AtmosphereAttribute.InnerRadius2))
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
                if (combinedAttributes.has(AtmosphereAttribute.KrESun))
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
                if (combinedAttributes.has(AtmosphereAttribute.KmESun))
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
                if (combinedAttributes.has(AtmosphereAttribute.Kr4PI))
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
                if (combinedAttributes.has(AtmosphereAttribute.Km4PI))
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
                if (combinedAttributes.has(AtmosphereAttribute.Scale))
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
                if (combinedAttributes.has(AtmosphereAttribute.ScaleDepth))
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
                if (combinedAttributes.has(AtmosphereAttribute.ScaleOverScaleDepth))
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
                if (combinedAttributes.has(AtmosphereAttribute.nSamples))
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
                if (combinedAttributes.has(AtmosphereAttribute.fSamples))
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
                if (combinedAttributes.has(AtmosphereAttribute.G))
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
                if (combinedAttributes.has(AtmosphereAttribute.G2))
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
                if (combinedAttributes.has(Vector3Attribute.PlanetPos))
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
                if (combinedAttributes.has(Vector3Attribute.CameraPos))
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
                if (combinedAttributes.has(Vector3Attribute.LightPos))
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
                if (combinedAttributes.has(Vector3Attribute.InvWavelength))
                    shader.set(inputID, ((Vector3Attribute) (combinedAttributes.get(Vector3Attribute.InvWavelength))).value);
            }
        };

    }

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

    public GroundShader(final IntRenderable renderable) {
        this(renderable, new Config());
    }

    public GroundShader(final IntRenderable renderable, final Config config) {
        this(renderable, config, createPrefix(renderable, config));
    }

    public GroundShader(final IntRenderable renderable, final Config config, final String prefix) {
        this(renderable, config, prefix, config.vertexShader != null ? config.vertexShader : getDefaultVertexShader(), config.fragmentShader != null ? config.fragmentShader : getDefaultFragmentShader());
    }

    public GroundShader(final IntRenderable renderable, final Config config, final String prefix, final String vertexShader, final String fragmentShader) {
        this(renderable, config, new ShaderProgram(ShaderProgramProvider.getShaderCode(prefix, vertexShader), ShaderProgramProvider.getShaderCode(prefix, fragmentShader)));
    }

    public GroundShader(final IntRenderable renderable, final Config config, final ShaderProgram shaderProgram) {
        super(renderable, config, shaderProgram);

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

    }

    @Override
    public boolean canRender(final IntRenderable renderable) {
        return super.canRender(renderable) && this.shadowMap == (renderable.environment.shadowMap != null);
    }

    public static String createPrefix(final IntRenderable renderable, final Config config) {
        String prefix = RelativisticShader.createPrefix(renderable, config);
        final long mask = renderable.material.getMask();
        // Atmosphere ground only if camera height is set
        if ((mask & AtmosphereAttribute.CameraHeight) == AtmosphereAttribute.CameraHeight)
            prefix += "#define atmosphereGround\n";
        return prefix;
    }
}
