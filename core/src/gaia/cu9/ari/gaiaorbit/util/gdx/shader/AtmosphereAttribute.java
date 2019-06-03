/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.shader;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;

public class AtmosphereAttribute extends FloatAttribute {
    public AtmosphereAttribute(long type) {
        super(type);
    }

    public AtmosphereAttribute(long type, float value) {
        super(type, value);
    }

    public static final String AlphaAlias = "alpha";
    public static final long Alpha = register(AlphaAlias);

    public static final String ColorOpacityAlias = "colorOpacity";
    public static final long ColorOpacity = register(ColorOpacityAlias);

    public static final String CameraHeightAlias = "cameraHeight";
    public static final long CameraHeight = register(CameraHeightAlias);

    public static final String CameraHeight2Alias = "cameraHeight2";
    public static final long CameraHeight2 = register(CameraHeight2Alias);

    public static final String OuterRadiusAlias = "outerRadius";
    public static final long OuterRadius = register(OuterRadiusAlias);

    public static final String OuterRadius2Alias = "outerRadius2";
    public static final long OuterRadius2 = register(OuterRadius2Alias);

    public static final String InnerRadiusAlias = "innerRadius";
    public static final long InnerRadius = register(InnerRadiusAlias);

    public static final String InnerRadius2Alias = "innerRadius2";
    public static final long InnerRadius2 = register(InnerRadius2Alias);

    public static final String KrESunAlias = "krESun";
    public static final long KrESun = register(KrESunAlias);

    public static final String KmESunAlias = "kmESun";
    public static final long KmESun = register(KmESunAlias);

    public static final String Kr4PIAlias = "kr4PI";
    public static final long Kr4PI = register(Kr4PIAlias);

    public static final String Km4PIAlias = "km4PI";
    public static final long Km4PI = register(Km4PIAlias);

    public static final String ScaleAlias = "scale";
    public static final long Scale = register(ScaleAlias);

    public static final String ScaleDepthAlias = "scaleDepth";
    public static final long ScaleDepth = register(ScaleDepthAlias);

    public static final String ScaleOverScaleDepthAlias = "scaleOverScaleDepth";
    public static final long ScaleOverScaleDepth = register(ScaleOverScaleDepthAlias);

    public static final String NSamplesAlias = "nSamples";
    public static final long nSamples = register(NSamplesAlias);

    public static final String FSamplesAlias = "fSamples";
    public static final long fSamples = register(FSamplesAlias);

    public static final String GAlias = "g";
    public static final long G = register(GAlias);

    public static final String G2Alias = "g2";
    public static final long G2 = register(G2Alias);

    @Override
    public Attribute copy() {
        return new AtmosphereAttribute(type, value);
    }

}
