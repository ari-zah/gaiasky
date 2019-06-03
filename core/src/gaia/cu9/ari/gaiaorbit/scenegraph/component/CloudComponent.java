/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaia.cu9.ari.gaiaorbit.data.AssetBean;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModel;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModelInstance;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.AtmosphereAttribute;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.util.Map;

public class CloudComponent {
    private static final Log logger = Logger.getLogger(CloudComponent.class);
        
    /** Default texture parameters **/
    protected static final TextureParameter textureParams;
    static {
        textureParams = new TextureParameter();
        textureParams.genMipMaps = true;
        textureParams.magFilter = TextureFilter.Linear;
        textureParams.minFilter = TextureFilter.MipMapLinearNearest;
    }
    private AssetManager manager;
    public int quality;
    public float size;
    public ModelComponent mc;
    public Matrix4 localTransform;

    public String cloud, cloudtrans;

    private boolean texInitialised, texLoading;
    // Model parameters
    public Map<String, Object> params;

    Vector3 aux;
    Vector3d aux3;

    public CloudComponent() {
        localTransform = new Matrix4();
        mc = new ModelComponent(false);
        mc.initialize();
        aux = new Vector3();
        aux3 = new Vector3d();
    }

    public void initialize(boolean force) {
        if (!GlobalConf.scene.LAZY_TEXTURE_INIT || force) {
            // Add textures to load
            addToLoad(cloud);
            addToLoad(cloudtrans);
        }
    }

    public boolean isFinishedLoading(AssetManager manager) {
        return isFL(cloud, manager) && isFL(cloudtrans, manager);
    }

    public boolean isFL(String tex, AssetManager manager) {
        if (tex == null)
            return true;
        return manager.isLoaded(tex);
    }


    /**
     * Adds the texture to load and unpacks any star (*) with the current
     * quality setting.
     * 
     * @param tex
     */
    private void addToLoad(String tex) {
        if (tex == null)
            return;
        AssetBean.addAsset(tex, Texture.class, textureParams);
    }

    public void doneLoading(AssetManager manager) {
        this.manager = manager;
        Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel("sphere", params, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
        IntModel cloudModel = pair.getFirst();
        Material material = pair.getSecond().get("base");
        material.clear();

        // CREATE CLOUD MODEL
        mc.instance = new IntModelInstance(cloudModel, this.localTransform);

        if (!GlobalConf.scene.LAZY_TEXTURE_INIT)
            initMaterial();

        // Initialised
        texInitialised = !GlobalConf.scene.LAZY_TEXTURE_INIT;
        // Loading
        texLoading = false;
    }

    public void touch() {
        if (GlobalConf.scene.LAZY_TEXTURE_INIT && !texInitialised) {

            if (!texLoading) {
                if (cloud != null)
                    logger.info(I18n.bundle.format("notif.loading", cloud));
                if (cloudtrans != null)
                    logger.info(I18n.bundle.format("notif.loading", cloudtrans));
                initialize(true);
                // Set to loading
                texLoading = true;
            } else if (isFinishedLoading(manager)) {
                Gdx.app.postRunnable(() -> {
                    initMaterial();
                });

                // Set to initialised
                texInitialised = true;
                texLoading = false;
            }
        }

    }

    public void update(Vector3d transform) {
        transform.getMatrix(localTransform).scl(size);
    }

    public void initMaterial() {
        Material material = mc.instance.materials.first();
        if (cloud != null && manager.isLoaded(cloud)) {
            Texture tex = manager.get(cloud, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Diffuse, tex));
        }
        if (cloudtrans != null && manager.isLoaded(cloudtrans)) {
            Texture tex = manager.get(cloudtrans, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Normal, tex));
        }
        material.set(new BlendingAttribute(1.0f));
    }

    public void removeAtmosphericScattering(Material mat) {
        mat.remove(AtmosphereAttribute.CameraHeight);
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

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public void setCloud(String cloud) {
        this.cloud = GlobalConf.data.dataFile(cloud);
    }

    public void setCloudtrans(String cloudtrans) {
        this.cloudtrans = cloudtrans;
    }

}
