/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.component;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaia.cu9.ari.gaiaorbit.scenegraph.Planet;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.ModelCache;
import gaia.cu9.ari.gaiaorbit.util.Pair;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModel;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModelInstance;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.AtmosphereAttribute;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.Vector3Attribute;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.vr.VRContext;

import java.util.Map;

public class AtmosphereComponent {

    public int quality;
    public float size;
    public float planetSize;
    public ModelComponent mc;
    public Matrix4 localTransform;
    public double[] wavelengths;
    public float m_fInnerRadius;
    public float m_fOuterRadius;
    public float m_fAtmosphereHeight;
    public float m_Kr, m_Km;
    public boolean correctGround;

    // Model parameters
    public Map<String, Object> params;

    Vector3 aux;
    Vector3d aux3, aux1;

    public AtmosphereComponent() {
        localTransform = new Matrix4();
        mc = new ModelComponent(false);
        mc.initialize();
        aux = new Vector3();
        aux3 = new Vector3d();
        aux1 = new Vector3d();
    }

    public void doneLoading(Material planetMat, float planetSize) {
        this.planetSize = planetSize;
        setUpAtmosphericScatteringMaterial(planetMat, correctGround ? true : false);

        Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel("sphere", params, Usage.Position | Usage.Normal);
        IntModel atmosphereModel = pair.getFirst();
        Material atmMat = pair.getSecond().get("base");
        atmMat.clear();
        setUpAtmosphericScatteringMaterial(atmMat, false);
        atmMat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));

        // CREATE ATMOSPHERE MODEL
        mc.instance = new IntModelInstance(atmosphereModel, this.localTransform);

    }

    public void update(Vector3d transform) {
        transform.getMatrix(localTransform).scl(size);
    }

    /**
     * Sets up the atmospheric scattering parameters to the given material
     * 
     * @param mat
     *            The material to set up.
     */
    public void setUpAtmosphericScatteringMaterial(Material mat, boolean ground) {
        float camHeight = 1f;
        float m_Kr4PI = m_Kr * 4.0f * (float) Math.PI;
        float m_Km4PI = m_Km * 4.0f * (float) Math.PI;
        float m_ESun = 10.0f; // Sun brightness (almost) constant
        float m_g = -0.95f; // The Mie phase asymmetry factor
        m_fInnerRadius = planetSize / 2f;
        m_fOuterRadius = ground ? (float) (this.size + 150 * Constants.KM_TO_U) : this.size;
        m_fAtmosphereHeight = m_fOuterRadius - m_fInnerRadius;
        float m_fScaleDepth = .20f;
        float m_fScale = 1.0f / (m_fAtmosphereHeight);
        float m_fScaleOverScaleDepth = m_fScale / m_fScaleDepth;
        int m_nSamples = 11;

        double[] m_fWavelength = wavelengths;
        float[] m_fWavelength4 = new float[3];
        m_fWavelength4[0] = (float) Math.pow(m_fWavelength[0], 4.0);
        m_fWavelength4[1] = (float) Math.pow(m_fWavelength[1], 4.0);
        m_fWavelength4[2] = (float) Math.pow(m_fWavelength[2], 4.0);

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.Alpha, 1f));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.ColorOpacity, 1f));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.CameraHeight, camHeight));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.CameraHeight2, camHeight * camHeight));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.OuterRadius, m_fOuterRadius));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.OuterRadius2, m_fOuterRadius * m_fOuterRadius));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.InnerRadius, m_fInnerRadius));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.InnerRadius2, m_fInnerRadius * m_fInnerRadius));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.KrESun, m_Kr * m_ESun));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.KmESun, m_Km * m_ESun));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.Kr4PI, m_Kr4PI));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.Km4PI, m_Km4PI));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.Scale, m_fScale));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.ScaleDepth, m_fScaleDepth));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.ScaleOverScaleDepth, m_fScaleOverScaleDepth));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.nSamples, m_nSamples));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.fSamples, (float) m_nSamples));

        mat.set(new AtmosphereAttribute(AtmosphereAttribute.G, m_g));
        mat.set(new AtmosphereAttribute(AtmosphereAttribute.G2, m_g * m_g));

        mat.set(new Vector3Attribute(Vector3Attribute.PlanetPos, new Vector3()));
        mat.set(new Vector3Attribute(Vector3Attribute.CameraPos, new Vector3()));
        mat.set(new Vector3Attribute(Vector3Attribute.LightPos, new Vector3()));
        mat.set(new Vector3Attribute(Vector3Attribute.InvWavelength, new Vector3(1.0f / m_fWavelength4[0], 1.0f / m_fWavelength4[1], 1.0f / m_fWavelength4[2])));
    }

    public void removeAtmosphericScattering(Material mat) {
        mat.remove(AtmosphereAttribute.CameraHeight);
    }

    /**
     * Updates the atmospheric scattering shader parameters
     * 
     * @param mat
     *            The material to update.
     * @param alpha
     *            The opacity value.
     * @param ground
     *            Whether it is the ground shader or the atmosphere.
     * @param planet
     *            The planet itself, holder of this atmosphere
     */
    public void updateAtmosphericScatteringParams(Material mat, float alpha, boolean ground, Planet planet, Vector3d vroffset) {
        Vector3d transform = planet.translation;
        RotationComponent rc = planet.rc;
        SceneGraphNode sol = planet.parent;
        transform.put(aux3);
        if (vroffset != null) {
            aux1.set(vroffset).scl(1 / VRContext.VROFFSET_FACTOR);
            aux3.sub(aux1);
        }

        // Distance to planet
        float camHeight = (float) (aux3.len());
        float m_ESun = 10f;
        float camHeightGr = camHeight - m_fInnerRadius;
        float atmFactor = (m_fAtmosphereHeight - camHeightGr) / m_fAtmosphereHeight;

        if (!ground && camHeightGr < m_fAtmosphereHeight) {
            // Camera inside atmosphere
            m_ESun += atmFactor * 100f;
        }

        // These are here to get the desired effect inside the atmosphere
        if (mat.has(AtmosphereAttribute.KrESun))
            ((AtmosphereAttribute) mat.get(AtmosphereAttribute.KrESun)).value = m_Kr * m_ESun;
        else
            mat.set(new AtmosphereAttribute(AtmosphereAttribute.KrESun, m_Kr * m_ESun));

        if (mat.has(AtmosphereAttribute.KmESun))
            ((AtmosphereAttribute) mat.get(AtmosphereAttribute.KmESun)).value = m_Km * m_ESun;
        else
            mat.set(new AtmosphereAttribute(AtmosphereAttribute.KmESun, m_Km * m_ESun));

        // Camera height
        if (mat.has(AtmosphereAttribute.CameraHeight))
            ((AtmosphereAttribute) mat.get(AtmosphereAttribute.CameraHeight)).value = camHeight;
        else
            mat.set(new AtmosphereAttribute(AtmosphereAttribute.CameraHeight, camHeight));

        // Camera height **2
        ((AtmosphereAttribute) mat.get(AtmosphereAttribute.CameraHeight2)).value = camHeight * camHeight;

        // Planet position
        if (ground) {
            // Camera position must be corrected using the rotation angle of the planet
            aux3.mul(Coordinates.getTransformD(planet.inverseRefPlaneTransform)).rotate(rc.ascendingNode, 0, 1, 0).rotate(-rc.inclination - rc.axialTilt, 0, 0, 1).rotate(-rc.angle, 0, 1, 0);
        }
        ((Vector3Attribute) mat.get(Vector3Attribute.PlanetPos)).value.set(aux3.put(aux));
        // CameraPos = -PlanetPos
        aux3.scl(-1f);

        ((Vector3Attribute) mat.get(Vector3Attribute.CameraPos)).value.set(aux3.put(aux));

        // Light position respect the earth: LightPos = SunPos - EarthPos
        aux3.add(sol.translation).nor();
        if (ground) {
            // Camera position must be corrected using the rotation angle of the planet
            aux3.mul(Coordinates.getTransformD(planet.inverseRefPlaneTransform)).rotate(rc.ascendingNode, 0, 1, 0).rotate(-rc.inclination - rc.axialTilt, 0, 0, 1).rotate(-rc.angle, 0, 1, 0);
        }
        ((Vector3Attribute) mat.get(Vector3Attribute.LightPos)).value.set(aux3.put(aux));

        // Alpha value
        ((AtmosphereAttribute) mat.get(AtmosphereAttribute.Alpha)).value = alpha;
    }

    public void setQuality(Long quality) {
        this.quality = quality.intValue();
    }

    public void setSize(Double size) {
        this.size = (float) (size * Constants.KM_TO_U);
    }

    public void setMc(ModelComponent mc) {
        this.mc = mc;
    }

    public void setLocalTransform(Matrix4 localTransform) {
        this.localTransform = localTransform;
    }

    public void setWavelengths(double[] wavelengths) {
        this.wavelengths = wavelengths;
    }

    public void setM_Kr(Double m_Kr) {
        this.m_Kr = m_Kr.floatValue();
    }

    public void setM_Km(Double m_Km) {
        this.m_Km = m_Km.floatValue();
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public void setCorrectground(Boolean correctGround) {
        this.correctGround = correctGround;
    }

}
