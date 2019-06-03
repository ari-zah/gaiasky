/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.component;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import gaia.cu9.ari.gaiaorbit.data.AssetBean;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModelInstance;

import java.util.Map;

/**
 * A basic component that contains the info on the textures.
 * 
 * @author Toni Sagrista
 *
 */
public class TextureComponent {
    /** Default texture parameters **/
    protected static final TextureParameter textureParams;
    static {
        textureParams = new TextureParameter();
        textureParams.genMipMaps = true;
        textureParams.magFilter = TextureFilter.Linear;
        textureParams.minFilter = TextureFilter.MipMapLinearNearest;
    }

    public String base, specular, normal, night, ring;
    public String baseT, specularT, normalT, nightT, ringT;
    public Texture baseTex;
    /** Add also color even if texture is present **/
    public boolean coloriftex = false;

    public TextureComponent() {

    }

    public void initialize(AssetManager manager) {
        // Add textures to load
        baseT = addToLoad(base, manager);
        normalT = addToLoad(normal, manager);
        specularT = addToLoad(specular, manager);
        nightT = addToLoad(night, manager);
        ringT = addToLoad(ring, manager);
    }

    public void initialize() {
        // Add textures to load
        baseT = addToLoad(base);
        normalT = addToLoad(normal);
        specularT = addToLoad(specular);
        nightT = addToLoad(night);
        ringT = addToLoad(ring);
    }

    public boolean isFinishedLoading(AssetManager manager) {
        return isFL(baseT, manager) && isFL(normalT, manager) && isFL(specularT, manager) && isFL(nightT, manager) && isFL(ringT, manager);
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
     * @return The actual loaded texture path
     */
    private String addToLoad(String tex, AssetManager manager) {
        if (tex == null)
            return null;

        tex = unpackTexName(tex);
        manager.load(tex, Texture.class, textureParams);

        return tex;
    }

    /**
     * Adds the texture to load and unpacks any star (*) with the current
     * quality setting.
     * 
     * @param tex
     * @return The actual loaded texture path
     */
    private String addToLoad(String tex) {
        if (tex == null)
            return null;

        tex = unpackTexName(tex);
        AssetBean.addAsset(tex, Texture.class, textureParams);

        return tex;
    }

    private String unpackTexName(String tex) {
        if (tex.contains("*")) {
            // Try to figure out which is it
            String suffix = getQualitySuffix();
            String texSuffix = tex.replace("*", suffix);
            if (GlobalConf.data.dataFileHandle(texSuffix).exists()) {
                tex = texSuffix;
            } else {
                tex = tex.replace("*", "");
            }
        }
        return tex;
    }

    private String getQualitySuffix() {
        switch (GlobalConf.scene.GRAPHICS_QUALITY) {
        case 0:
            return "-high";
        case 1:
            return "-med";
        case 2:
            return "-low";
        default:
            return "";
        }
    }

    public Material initMaterial(AssetManager manager, IntModelInstance instance, float[] cc, boolean culling) {
        Material material = instance.materials.get(0);
        if (base != null) {
            baseTex = manager.get(baseT, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Diffuse, baseTex));
        }
        if (cc != null && (coloriftex || base == null)) {
            // Add diffuse colour
            material.set(new ColorAttribute(ColorAttribute.Diffuse, cc[0], cc[1], cc[2], cc[3]));
        }

        if (normal != null) {
            Texture tex = manager.get(normalT, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Normal, tex));
        }
        if (specular != null) {
            Texture tex = manager.get(specularT, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Specular, tex));
            // Control amount of specularity
            material.set(new ColorAttribute(ColorAttribute.Specular, 0.5f, 0.5f, 0.5f, 1f));
        }
        if (night != null) {
            Texture tex = manager.get(nightT, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Emissive, tex));
        }
        if (instance.materials.size > 1) {
            // Ring material
            Material ringMat = instance.materials.get(1);
            Texture tex = manager.get(ringT, Texture.class);
            ringMat.set(new TextureAttribute(TextureAttribute.Diffuse, tex));
            ringMat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
            if (!culling)
                ringMat.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
        }
        if (!culling) {
            material.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
        }

        return material;
    }

    /**
     * Initialises the materials by binding the necessary textures to them.
     * 
     * @param manager
     *            The asset manager.
     * @param materials
     *            A map with at least one material under the key "base".
     * @param cc
     *            Plain color used if there is no texture.
     */
    public void initMaterial(AssetManager manager, Map<String, Material> materials, float[] cc, boolean culling) {
        Material material = materials.get("base");
        if (base != null) {
            baseTex = manager.get(baseT, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Diffuse, baseTex));
        }
        if (cc != null && (coloriftex || base == null)) {
            // Add diffuse colour
            material.set(new ColorAttribute(ColorAttribute.Diffuse, cc[0], cc[1], cc[2], cc[3]));
        }

        if (normal != null) {
            Texture tex = manager.get(normalT, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Normal, tex));
        }
        if (specular != null) {
            Texture tex = manager.get(specularT, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Specular, tex));
            // Control amount of specularity
            material.set(new ColorAttribute(ColorAttribute.Specular, 0.5f, 0.5f, 0.5f, 1f));
        }
        if (night != null) {
            Texture tex = manager.get(nightT, Texture.class);
            material.set(new TextureAttribute(TextureAttribute.Emissive, tex));
        }
        if (materials.containsKey("ring")) {
            // Ring material
            Material ringMat = materials.get("ring");
            Texture tex = manager.get(ringT, Texture.class);
            ringMat.set(new TextureAttribute(TextureAttribute.Diffuse, tex));
            ringMat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
            if (!culling)
                ringMat.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
        }
        if (!culling) {
            material.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
        }
    }

    public void setBase(String base) {
        this.base = GlobalConf.data.dataFile(base);
    }

    public void setSpecular(String specular) {
        this.specular = GlobalConf.data.dataFile(specular);
    }

    public void setNormal(String normal) {
        this.normal = GlobalConf.data.dataFile(normal);
    }

    public void setNight(String night) {
        this.night = GlobalConf.data.dataFile(night);
    }

    public void setRing(String ring) {
        this.ring = GlobalConf.data.dataFile(ring);
    }

    public void setColoriftex(Boolean coloriftex) {
        this.coloriftex = coloriftex;
    }


    /** Disposes all currently loaded textures **/
    public void disposeTextures(AssetManager manager) {
        if (base != null && manager.containsAsset(baseT)) {
            manager.unload(baseT);
            baseT = null;
        }
        if (normal != null && manager.containsAsset(normalT)) {
            manager.unload(normalT);
            normalT = null;
        }
        if (specular != null && manager.containsAsset(specularT)) {
            manager.unload(specularT);
            specularT = null;
        }
        if (night != null && manager.containsAsset(nightT)) {
            manager.unload(nightT);
            nightT = null;
        }
        if (ring != null && manager.containsAsset(ringT)) {
            manager.unload(ringT);
            ringT = null;
        }
    }
}
