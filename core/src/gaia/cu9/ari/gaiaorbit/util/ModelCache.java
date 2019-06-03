/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntMeshPartBuilder.VertexInfo;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntModelBuilder;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModelCache {
    final Map<String, IntModel> modelCache;
    public IntModelBuilder mb;
    /** Model cache **/
    public static ModelCache cache = new ModelCache();

    public ModelCache() {
        modelCache = new HashMap<>();
        mb = new IntModelBuilder();
    }

    public Pair<IntModel, Map<String, Material>> getModel(String shape, Map<String, Object> params, int attributes) {

        String key = getKey(shape, params, attributes);
        IntModel model = null;
        Map<String, Material> materials = new HashMap<>();
        Material mat;
        if (modelCache.containsKey(key)) {
            model = modelCache.get(key);
            mat = model.materials.first();
        } else {
            mat = new Material();
            switch (shape) {
            case "sphere":
                Integer quality = ((Long) params.get("quality")).intValue();
                Float diameter = ((Double) params.get("diameter")).floatValue();
                Boolean flip = (Boolean) params.get("flip");
                model = mb.createSphere(diameter, quality, quality, flip, mat, attributes);
                modelCache.put(key, model);
                break;
            case "icosphere":
                Integer recursion = ((Long) params.get("recursion")).intValue();
                diameter = ((Double) params.get("diameter")).floatValue();
                flip = (Boolean) params.get("flip");
                model = mb.createIcoSphere(diameter / 2, recursion, flip, false, mat, attributes);
                modelCache.put(key, model);
                break;
            case "octahedronsphere":
                Integer divisions = ((Long) params.get("divisions")).intValue();
                diameter = ((Double) params.get("diameter")).floatValue();
                flip = (Boolean) params.get("flip");
                model = mb.createOctahedronSphere(diameter / 2, divisions, flip, false, mat, attributes);
                modelCache.put(key, model);
                break;
            case "disc":
                // Prepare model
                float diameter2 = ((Double) params.get("diameter")).floatValue() / 2f;
                // Initialize disc model

                // TOP VERTICES
                VertexInfo vt00 = new VertexInfo();
                vt00.setPos(-diameter2, 0, -diameter2);
                vt00.setNor(0, 1, 0);
                vt00.setUV(0, 0);
                VertexInfo vt01 = new VertexInfo();
                vt01.setPos(diameter2, 0, -diameter2);
                vt01.setNor(0, 1, 0);
                vt01.setUV(0, 1);
                VertexInfo vt11 = new VertexInfo();
                vt11.setPos(diameter2, 0, diameter2);
                vt11.setNor(0, 1, 0);
                vt11.setUV(1, 1);
                VertexInfo vt10 = new VertexInfo();
                vt10.setPos(-diameter2, 0, diameter2);
                vt10.setNor(0, 1, 0);
                vt10.setUV(1, 0);

                // BOTTOM VERTICES
                VertexInfo vb00 = new VertexInfo();
                vb00.setPos(-diameter2, 0, -diameter2);
                vb00.setNor(0, 1, 0);
                vb00.setUV(0, 0);
                VertexInfo vb01 = new VertexInfo();
                vb01.setPos(diameter2, 0, -diameter2);
                vb01.setNor(0, 1, 0);
                vb01.setUV(0, 1);
                VertexInfo vb11 = new VertexInfo();
                vb11.setPos(diameter2, 0, diameter2);
                vb11.setNor(0, 1, 0);
                vb11.setUV(1, 1);
                VertexInfo vb10 = new VertexInfo();
                vb10.setPos(-diameter2, 0, diameter2);
                vb10.setNor(0, 1, 0);
                vb10.setUV(1, 0);

                mb.begin();
                mb.part("up", GL20.GL_TRIANGLES, attributes, mat).rect(vt00, vt01, vt11, vt10);
                mb.part("down", GL20.GL_TRIANGLES, attributes, mat).rect(vb00, vb10, vb11, vb01);
                model = mb.end();
                break;
            case "twofacedbillboard":
                // Prepare model
                diameter2 = ((Double) params.get("diameter")).floatValue() / 2f;
                // Initialize disc model

                // TOP VERTICES
                 vt00 = new VertexInfo();
                vt00.setPos(-diameter2, -diameter2,0);
                vt00.setNor(0, 1, 0);
                vt00.setUV(1, 0);
                 vt01 = new VertexInfo();
                vt01.setPos(-diameter2,  diameter2, 0);
                vt01.setNor(0, 1, 0);
                vt01.setUV(1, 1);
                 vt11 = new VertexInfo();
                vt11.setPos(diameter2,  diameter2, 0);
                vt11.setNor(0, 1, 0);
                vt11.setUV(0, 1);
                 vt10 = new VertexInfo();
                vt10.setPos(diameter2, -diameter2, 0);
                vt10.setNor(0, 1, 0);
                vt10.setUV(0, 0);

                // BOTTOM VERTICES
                vb00 = new VertexInfo();
                vb00.setPos(-diameter2, -diameter2, 0);
                vb00.setNor(0, 1, 0);
                vb00.setUV(1, 0);
                 vb01 = new VertexInfo();
                vb01.setPos(-diameter2, diameter2, 0);
                vb01.setNor(0, 1, 0);
                vb01.setUV(1, 1);
                 vb11 = new VertexInfo();
                vb11.setPos(diameter2, diameter2, 0);
                vb11.setNor(0, 1, 0);
                vb11.setUV(0, 1);
                 vb10 = new VertexInfo();
                vb10.setPos(diameter2, -diameter2, 0);
                vb10.setNor(0, 1, 0);
                vb10.setUV(0, 0);

                mb.begin();
                mb.part("up", GL20.GL_TRIANGLES, attributes, mat).rect(vt00, vt01, vt11, vt10);
                mb.part("down", GL20.GL_TRIANGLES, attributes, mat).rect(vb00, vb10, vb11, vb01);
                model = mb.end();
                break;
            case "cylinder":
                // Use builder
                Float width = ((Double) params.get("width")).floatValue();
                Float height = ((Double) params.get("height")).floatValue();
                Float depth = ((Double) params.get("depth")).floatValue();
                divisions = ((Long) params.get("divisions")).intValue();
                flip = (Boolean) params.get("flip");

                model = mb.createCylinder(width, height, depth, divisions, flip, mat, attributes);

                break;
            case "ring":
                // Sphere with cylinder
                Material ringMat = new Material();
                materials.put("ring", ringMat);

                quality = ((Long) params.get("quality")).intValue();
                divisions = ((Long) params.get("divisions")).intValue();
                Float innerRad = ((Double) params.get("innerradius")).floatValue();
                Float outerRad = ((Double) params.get("outerradius")).floatValue();

                model = ModelCache.cache.mb.createSphereRing(1, quality, quality, innerRad, outerRad, divisions, mat, ringMat, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
                break;
            case "cone":
                width = ((Double) params.get("width")).floatValue();
                height = ((Double) params.get("height")).floatValue();
                depth = ((Double) params.get("depth")).floatValue();
                divisions = ((Long) params.get("divisions")).intValue();
                Integer hdivisions = 0;
                if (params.containsKey("hdivisions")) {
                    hdivisions = ((Long) params.get("hdivisions")).intValue();
                }

                if (hdivisions == 0)
                    model = mb.createCone(width, height, depth, divisions, mat, attributes);
                else
                    model = mb.createCone(width, height, depth, divisions, hdivisions, GL20.GL_TRIANGLES, mat, attributes);

                break;
            }
        }
        materials.put("base", mat);

        return new Pair<>(model, materials);
    }

    private String getKey(String shape, Map<String, Object> params, int attributes) {
        String key = shape + "-" + attributes;
        Set<String> keys = params.keySet();
        Object[] par = keys.toArray();
        for (int i = 0; i < par.length; i++) {
            key += "-" + params.get(par[i]);
        }
        return key;

    }

    public void dispose() {
        Collection<IntModel> models = modelCache.values();
        for (IntModel model : models) {
            try {
                model.dispose();
            } catch (Exception e) {
                // Do nothing
            }
        }
    }
}
