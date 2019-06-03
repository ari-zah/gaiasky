/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import gaia.cu9.ari.gaiaorbit.data.AssetBean;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModel;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModelInstance;

import java.util.HashMap;
import java.util.Map;

public class ModelComponent implements Disposable, IObserver {
    private static final Log logger = Logger.getLogger(ModelComponent.class);

    public boolean forceinit = false;
    private static ColorAttribute ambient;
    /**
     * Light never changes; set fixed ambient light for this model
     */
    private Boolean staticLight = false;
    /** Ambient light level for static light objects **/
    private float staticLightLevel = 0.6f;
    /** Flag **/
    private boolean updateStaticLight = false;

    static {
        ambient = new ColorAttribute(ColorAttribute.AmbientLight, (float) GlobalConf.scene.AMBIENT_LIGHT, (float) GlobalConf.scene.AMBIENT_LIGHT, (float) GlobalConf.scene.AMBIENT_LIGHT, 1f);
    }

    public static void toggleAmbientLight(boolean on) {
        if (on) {
            ambient.color.set(.7f, .7f, .7f, 1f);
        } else {
            ambient.color.set((float) GlobalConf.scene.AMBIENT_LIGHT, (float) GlobalConf.scene.AMBIENT_LIGHT, (float) GlobalConf.scene.AMBIENT_LIGHT, 1f);
        }
    }

    /**
     * Sets the ambient light
     *
     * @param level Ambient light level between 0 and 1
     */
    public static void setAmbientLight(float level) {
        ambient.color.set(level, level, level, 1f);
    }

    public IntModelInstance instance;
    public Environment env;
    /** Directional light **/
    public DirectionalLight dlight;

    public Map<String, Object> params;

    public String type, modelFile;

    public double scale = 1d;
    public boolean culling = true;
    private boolean texInitialised, texLoading;
    private boolean modelInitialised, modelLoading;
    private boolean useColor = true;

    private AssetManager manager;
    private float[] cc;

    /**
     * COMPONENTS
     */
    // Texture
    public TextureComponent tc;
    // Relativistic effects
    public RelativisticEffectsComponent rec;

    public ModelComponent() {
        this(true);
    }

    public ModelComponent(Boolean initEnvironment) {
        if (initEnvironment) {
            env = new Environment();
            env.set(ambient);
            // Direction from Sun to Earth
            dlight = new DirectionalLight();
            dlight.color.set(1f, 0f, 0f, 1f);
            env.add(dlight);
        }
    }

    public void initialize() {
        this.initialize(false);
    }

    public void initialize(boolean mesh) {
        FileHandle model = modelFile != null ? GlobalConf.data.dataFileHandle(modelFile) : null;
        if (mesh) {
            if (!GlobalConf.scene.LAZY_MESH_INIT && modelFile != null && model.exists()) {
                AssetBean.addAsset(GlobalConf.data.dataFile(modelFile), IntModel.class);
            }
        } else {
            if (modelFile != null && model.exists()) {
                AssetBean.addAsset(GlobalConf.data.dataFile(modelFile), IntModel.class);
            }
        }

        if ((forceinit || !GlobalConf.scene.LAZY_TEXTURE_INIT) && tc != null) {
            tc.initialize();
        }

        rec = new RelativisticEffectsComponent();
    }

    public void doneLoading(AssetManager manager, Matrix4 localTransform, float[] cc) {
        doneLoading(manager, localTransform, cc, false);
    }

    public void doneLoading(AssetManager manager, Matrix4 localTransform, float[] cc, boolean mesh) {
        this.manager = manager;
        this.cc = cc;

        IntModel model = null;
        Map<String, Material> materials = null;

        if (staticLight) {
            // Remove dir and global ambient. Add ambient
            //env.remove(dlight);
            // Ambient

            // If lazy texture init, we turn off the lights until the texture is loaded
            float level = GlobalConf.scene.LAZY_TEXTURE_INIT ? 0f : staticLightLevel;
            ColorAttribute alight = new ColorAttribute(ColorAttribute.AmbientLight, level, level, level, 1f);
            env.set(alight);
            updateStaticLight = GlobalConf.scene.LAZY_TEXTURE_INIT;
        }

        if (!mesh || !GlobalConf.scene.LAZY_MESH_INIT) {
            Pair<IntModel, Map<String, Material>> modmat = initModelFile();
            model = modmat.getFirst();
            materials = modmat.getSecond();
        }

        // INITIALIZE MATERIAL
        if ((forceinit || !GlobalConf.scene.LAZY_TEXTURE_INIT) && tc != null) {
            tc.initMaterial(manager, materials, cc, culling);
        }

        // CREATE MAIN MODEL INSTANCE
        if (!mesh || !GlobalConf.scene.LAZY_MESH_INIT) {
            instance = new IntModelInstance(model, localTransform);
        }

        // COLOR IF NO TEXTURE
        if (tc == null && instance != null) {
            addColorToMat();
        }
        // Subscribe to new graphics quality setting event
        EventManager.instance.subscribe(this, Events.GRAPHICS_QUALITY_UPDATED);
        // Initialised
        texInitialised = !GlobalConf.scene.LAZY_TEXTURE_INIT;
        // Loading
        texLoading = false;

        modelInitialised = !GlobalConf.scene.LAZY_MESH_INIT;
        modelLoading = false;
    }

    private Pair<IntModel, Map<String, Material>> initModelFile() {
        IntModel model = null;
        Map<String, Material> materials = null;
        if (modelFile != null && manager.isLoaded(GlobalConf.data.dataFile(modelFile))) {
            // Model comes from file (probably .obj or .g3db)
            model = manager.get(GlobalConf.data.dataFile(modelFile), IntModel.class);
            materials = new HashMap<>();
            if (model.materials.size == 0) {
                Material material = new Material();
                model.materials.add(material);
                materials.put("base", material);
            } else {
                if (model.materials.size > 1)
                    for (int i = 0; i < model.materials.size; i++) {
                        materials.put("base" + i, model.materials.get(i));
                    }
                else
                    materials.put("base0", model.materials.first());
            }

        } else if (type != null) {
            // We create the model
            Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel(type, params, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
            model = pair.getFirst();
            materials = pair.getSecond();
        } else {
            // Data error!
            logger.error(new RuntimeException("The 'model' element must contain either a 'type' or a 'model' attribute"));
        }
        // Clear base material
        if (materials.containsKey("base"))
            materials.get("base").clear();

        return new Pair<>(model, materials);
    }

    public void touch() {
        touch(null);
    }

    /**
     * Initialises the texture if it is not initialised yet
     */
    public void touch(Matrix4 localTransform) {
        if (GlobalConf.scene.LAZY_TEXTURE_INIT && !texInitialised) {

            if (tc != null) {
                if (!texLoading) {
                    logger.info(I18n.bundle.format("notif.loading", tc.base));
                    tc.initialize(manager);
                    // Set to loading
                    texLoading = true;
                } else if (tc.isFinishedLoading(manager)) {
                    Gdx.app.postRunnable(() -> {
                        tc.initMaterial(manager, instance, cc, culling);
                    });

                    // Set to initialised
                    updateStaticLight();
                    texInitialised = true;
                    texLoading = false;
                }
            } else if (localTransform == null) {
                // Use color if necessary
                addColorToMat();
                // Set to initialised
                updateStaticLight();
                texInitialised = true;
                texLoading = false;
            }

        }

        if (localTransform != null && GlobalConf.scene.LAZY_MESH_INIT && !modelInitialised) {
            if (!modelLoading) {
                logger.info(I18n.bundle.format("notif.loading", modelFile));
                AssetBean.addAsset(GlobalConf.data.dataFile(modelFile), IntModel.class);
                modelLoading = true;
            } else if (manager.isLoaded(GlobalConf.data.dataFile(modelFile))) {
                IntModel model;
                Pair<IntModel, Map<String, Material>> modMat = initModelFile();
                model = modMat.getFirst();
                instance = new IntModelInstance(model, localTransform);

                updateStaticLight();

                // COLOR IF NO TEXTURE
                if (tc == null && instance != null) {
                    addColorToMat();
                }

                modelInitialised = true;
                modelLoading = false;
            }
        }
    }

    private void updateStaticLight() {
        Gdx.app.postRunnable(()-> {
            // Update static
            if (updateStaticLight) {
                ColorAttribute ambient = (ColorAttribute) env.get(ColorAttribute.AmbientLight);
                if (ambient != null)
                    ambient.color.set(staticLightLevel, staticLightLevel, staticLightLevel, 1.0f);
                updateStaticLight = false;
            }
        });
    }

    public void addColorToMat() {
        if (cc != null && useColor) {
            // Regular mesh, we use the color
            int n = instance.materials.size;
            for (int i = 0; i < n; i++) {
                Material material = instance.materials.get(i);
                if (material.get(TextureAttribute.Ambient) == null && material.get(TextureAttribute.Diffuse) == null) {
                    material.set(new ColorAttribute(ColorAttribute.Diffuse, cc[0], cc[1], cc[2], cc[3]));
                    material.set(new ColorAttribute(ColorAttribute.Ambient, cc[0], cc[1], cc[2], cc[3]));
                    if (!culling)
                        material.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
                }
            }
        }
    }

    public void addDirectionalLight(float r, float g, float b, float x, float y, float z) {
        DirectionalLight dl = new DirectionalLight();
        dl.set(r, g, b, x, y, z);
        env.add(dl);
    }

    public void dispose() {
        if (instance != null && instance.model != null)
            instance.model.dispose();
    }

    public void setTransparency(float alpha, int src, int dst) {
        if (instance != null) {
            int n = instance.materials.size;
            for (int i = 0; i < n; i++) {
                Material mat = instance.materials.get(i);
                BlendingAttribute ba;
                if (mat.has(BlendingAttribute.Type)) {
                    ba = (BlendingAttribute) mat.get(BlendingAttribute.Type);
                    ba.destFunction = dst;
                    ba.sourceFunction = src;
                } else {
                    ba = new BlendingAttribute(src, dst);
                    mat.set(ba);
                }
                ba.opacity = alpha;
            }
        }
    }

    public void setTransparency(float alpha) {
        setTransparency(alpha, GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void setTransparencyColor(float alpha) {
        if (instance != null) {
            int n = instance.materials.size;
            for (int i = 0; i < n; i++) {
                ((ColorAttribute) instance.materials.get(i).get(ColorAttribute.Diffuse)).color.a = alpha;
            }
        }
    }

    /**
     * Sets the type of the model to construct.
     *
     * @param type The type. Currently supported types are
     *             sphere|cylinder|ring|disc.
     */
    public void setType(String type) {
        this.type = type;
    }

    public void setTexture(TextureComponent tc) {
        this.tc = tc;
    }

    /**
     * Sets the model file path (this must be a .g3db, .g3dj or .obj).
     *
     * @param model
     */
    public void setModel(String model) {
        this.modelFile = model;
    }

    public void setStaticlight(String staticLight) {
        setStaticlight(Boolean.valueOf(staticLight));
    }

    public void setStaticlight(Boolean staticLight) {
        this.staticLight = staticLight;
    }

    public void setStaticlight(Double lightLevel) {
        this.staticLight = true;
        this.staticLightLevel = lightLevel.floatValue();
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public void setScale(Double scale) {
        this.scale = scale;
    }

    public void setScale(Long scale) {
        this.scale = scale;
    }

    public void setCulling(String culling) {
        try {
            this.culling = Boolean.parseBoolean(culling);
        } catch (Exception e) {
        }
    }

    public void setCulling(Boolean culling) {
        this.culling = culling;
    }

    public void setUsecolor(String usecolor) {
        try {
            this.useColor = Boolean.parseBoolean(usecolor);
        } catch (Exception e) {
        }
    }

    public void updateRelativisticEffects(ICamera camera) {
        updateRelativisticEffects(camera, -1);
    }

    public void updateRelativisticEffects(ICamera camera, float vc) {
        for (Material mat : instance.materials) {
            updateRelativisticEffects(mat, camera, vc);
        }
    }

    public void updateRelativisticEffects(Material mat, ICamera camera) {
        updateRelativisticEffects(mat, camera, -1);
    }

    public void updateRelativisticEffects(Material mat, ICamera camera, float vc) {
        if (GlobalConf.runtime.RELATIVISTIC_ABERRATION) {
            rec.updateRelativisticEffectsMaterial(mat, camera, vc);
        } else if (rec.hasRelativisticEffects(mat)) {
            rec.removeRelativisticEffectsMaterial(mat);
        }

        if (GlobalConf.runtime.GRAVITATIONAL_WAVES) {
            rec.updateGravitationalWavesMaterial(mat);
        } else if (rec.hasGravitationalWaves(mat)) {
            rec.removeGravitationalWavesMaterial(mat);
        }
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case GRAPHICS_QUALITY_UPDATED:
            if (texInitialised) {
                // Remove current textures
                // TODO
                texInitialised = false;
                if (tc != null)
                    tc.disposeTextures(this.manager);

            }
            break;
        default:
            break;
        }
    }

}
