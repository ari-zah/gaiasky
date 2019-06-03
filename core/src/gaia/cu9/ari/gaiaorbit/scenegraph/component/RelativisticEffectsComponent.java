/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.component;

import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.Matrix3Attribute;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.RelativisticEffectFloatAttribute;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.Vector3Attribute;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.Vector4Attribute;

import java.util.Map;
import java.util.Set;

public class RelativisticEffectsComponent {

    public RelativisticEffectsComponent() {
        super();
    }

    public void doneLoading(Map<String, Material> materials) {
        Set<String> keys = materials.keySet();
        for (String key : keys) {
            Material mat = materials.get(key);
            setUpRelativisticEffectsMaterial(mat);
            setUpGravitationalWavesMaterial(mat);
        }
    }

    public void doneLoading(Material mat) {
        setUpRelativisticEffectsMaterial(mat);
        setUpGravitationalWavesMaterial(mat);
    }

    public void setUpRelativisticEffectsMaterial(Array<Material> materials) {
        for (Material material : materials) {
            setUpRelativisticEffectsMaterial(material);
        }
    }

    public void setUpRelativisticEffectsMaterial(Material mat) {
        mat.set(new RelativisticEffectFloatAttribute(RelativisticEffectFloatAttribute.Vc, 0f));
        mat.set(new Vector3Attribute(Vector3Attribute.VelDir, new Vector3()));
    }

    public void removeRelativisticEffectsMaterial(Array<Material> materials) {
        for (Material material : materials) {
            removeRelativisticEffectsMaterial(material);
        }
    }

    public void removeRelativisticEffectsMaterial(Material mat) {
        mat.remove(RelativisticEffectFloatAttribute.Vc);
        mat.remove(Vector3Attribute.VelDir);
    }

    public void setUpGravitationalWavesMaterial(Array<Material> materials) {
        for (Material material : materials) {
            setUpGravitationalWavesMaterial(material);
        }
    }

    public void setUpGravitationalWavesMaterial(Material mat) {
        mat.set(new Vector4Attribute(Vector4Attribute.Hterms, new float[4]));
        mat.set(new Vector3Attribute(Vector3Attribute.Gw, new Vector3()));
        mat.set(new Matrix3Attribute(Matrix3Attribute.Gwmat3, new Matrix3()));
        mat.set(new RelativisticEffectFloatAttribute(RelativisticEffectFloatAttribute.Ts, 0f));
        mat.set(new RelativisticEffectFloatAttribute(RelativisticEffectFloatAttribute.Omgw, 0f));
    }

    public void removeGravitationalWavesMaterial(Array<Material> materials) {
        for (Material material : materials) {
            removeGravitationalWavesMaterial(material);
        }
    }

    public void removeGravitationalWavesMaterial(Material mat) {
        mat.remove(Vector4Attribute.Hterms);
        mat.remove(Vector3Attribute.Gw);
        mat.remove(Matrix3Attribute.Gwmat3);
        mat.remove(RelativisticEffectFloatAttribute.Ts);
        mat.remove(RelativisticEffectFloatAttribute.Omgw);
    }

    public void updateRelativisticEffectsMaterial(Material material, ICamera camera) {
        updateRelativisticEffectsMaterial(material, camera, -1);
    }

    public void updateRelativisticEffectsMaterial(Material material, ICamera camera, float vc) {
        if (material.get(RelativisticEffectFloatAttribute.Vc) == null) {
            setUpRelativisticEffectsMaterial(material);
        }
        RelativisticEffectsManager rem = RelativisticEffectsManager.getInstance();
        if (vc != -1) {
            // v/c
            ((RelativisticEffectFloatAttribute) material.get(RelativisticEffectFloatAttribute.Vc)).value = vc;
        } else {

            // v/c
            ((RelativisticEffectFloatAttribute) material.get(RelativisticEffectFloatAttribute.Vc)).value = rem.vc;
        }
        // Velocity direction
        ((Vector3Attribute) material.get(Vector3Attribute.VelDir)).value.set(rem.velDir);
    }

    public void updateGravitationalWavesMaterial(Material material) {
        if (material.get(Vector4Attribute.Hterms) == null) {
            setUpGravitationalWavesMaterial(material);
        }
        RelativisticEffectsManager rem = RelativisticEffectsManager.getInstance();
        // hterms
        ((Vector4Attribute) material.get(Vector4Attribute.Hterms)).value = rem.hterms;

        // gw
        ((Vector3Attribute) material.get(Vector3Attribute.Gw)).value.set(rem.gw);

        // gwmat3
        ((Matrix3Attribute) material.get(Matrix3Attribute.Gwmat3)).value.set(rem.gwmat3);

        // ts
        ((RelativisticEffectFloatAttribute) material.get(RelativisticEffectFloatAttribute.Ts)).value = rem.gwtime;

        // omgw
        ((RelativisticEffectFloatAttribute) material.get(RelativisticEffectFloatAttribute.Omgw)).value = rem.omgw;
    }

    public boolean hasGravitationalWaves(Material mat) {
        return mat.get(Vector4Attribute.Hterms) != null;
    }

    public boolean hasRelativisticEffects(Material mat) {
        return mat.get(Vector3Attribute.VelDir) != null;
    }
}
