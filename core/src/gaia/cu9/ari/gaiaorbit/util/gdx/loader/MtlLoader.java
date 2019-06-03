/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.loader;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial;
import com.badlogic.gdx.graphics.g3d.model.data.ModelTexture;
import com.badlogic.gdx.utils.Array;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MtlLoader {
    public Array<ModelMaterial> materials = new Array<>();

    /** loads .mtl file */
    public void load(FileHandle file) {
        String line;
        String[] tokens;
        String curMatName = "default";
        Color difcolor = Color.WHITE;
        Color speccolor = Color.WHITE;
        Color emicolor = Color.WHITE;
        float opacity = 1.f;
        float shininess = 0.f;
        String texDiffuseFilename = null;
        String texEmissiveFilename = null;
        String texNormalFilename = null;

        if (file == null || file.exists() == false)
            return;

        BufferedReader reader = new BufferedReader(new InputStreamReader(file.read()), 4096);
        try {
            while ((line = reader.readLine()) != null) {

                if (line.length() > 0 && line.charAt(0) == '\t')
                    line = line.substring(1).trim();

                tokens = line.split("\\s+");

                if (tokens[0].length() == 0) {
                    continue;
                } else if (tokens[0].charAt(0) == '#')
                    continue;
                else {
                    final String key = tokens[0].toLowerCase();
                    if (key.equals("newmtl")) {
                        addCurrentMat(curMatName, difcolor, speccolor, emicolor, opacity, shininess, texDiffuseFilename, texEmissiveFilename, texNormalFilename, materials);

                        if (tokens.length > 1) {
                            curMatName = tokens[1];
                            curMatName = curMatName.replace('.', '_');
                        } else
                            curMatName = "default";

                        difcolor = Color.WHITE;
                        speccolor = Color.WHITE;
                        emicolor = Color.WHITE;
                        opacity = 1.f;
                        shininess = 0.f;
                    } else if (key.equals("kd") || key.equals("ks") || key.equals("ke")) // diffuse, specular or emissive
                    {
                        float r = Float.parseFloat(tokens[1]);
                        float g = Float.parseFloat(tokens[2]);
                        float b = Float.parseFloat(tokens[3]);
                        float a = 1;
                        if (tokens.length > 4)
                            a = Float.parseFloat(tokens[4]);

                        if (tokens[0].toLowerCase().equals("kd")) {
                            difcolor = new Color();
                            difcolor.set(r, g, b, a);
                        } else if (tokens[0].toLowerCase().equals("ks")) {
                            speccolor = new Color();
                            speccolor.set(r, g, b, a);
                        } else if (tokens[0].toLowerCase().equals("ke")) {
                            emicolor = new Color();
                            emicolor.set(r, g, b, a);
                        }
                    } else if (key.equals("tr") || key.equals("d")) {
                        opacity = Float.parseFloat(tokens[1]);
                    } else if (key.equals("ns")) {
                        shininess = Float.parseFloat(tokens[1]);
                    } else if (key.equals("map_kd")) {
                        texDiffuseFilename = file.parent().child(tokens[1]).path();
                    } else if (key.equals("map_ke")) {
                        texEmissiveFilename = file.parent().child(tokens[1]).path();
                    } else if (key.equals("map_kn") || key.equals("map_bump")) {
                        texNormalFilename = file.parent().child(tokens[1]).path();
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            return;
        }

        // last material
        addCurrentMat(curMatName, difcolor, speccolor, emicolor, opacity, shininess, texDiffuseFilename, texEmissiveFilename, texNormalFilename, materials);

        return;
    }

    private void addCurrentMat(String curMatName, Color difcolor, Color speccolor, Color emicolor, float opacity, float shininess, String texDiffuseFilename, String texEmissiveFilename, String texNormalFilename, Array<ModelMaterial> materials) {
        ModelMaterial mat = new ModelMaterial();
        mat.id = curMatName;
        mat.diffuse = new Color(difcolor);
        mat.specular = new Color(speccolor);
        mat.emissive = new Color(emicolor);
        mat.opacity = opacity;
        mat.shininess = shininess;
        if (texDiffuseFilename != null) {
            ModelTexture tex = new ModelTexture();
            tex.usage = ModelTexture.USAGE_DIFFUSE;
            tex.fileName = new String(texDiffuseFilename);
            if (mat.textures == null)
                mat.textures = new Array<ModelTexture>(1);
            mat.textures.add(tex);
        }
        if (texEmissiveFilename != null) {
            ModelTexture tex = new ModelTexture();
            tex.usage = ModelTexture.USAGE_EMISSIVE;
            tex.fileName = new String(texEmissiveFilename);
            if (mat.textures == null)
                mat.textures = new Array<ModelTexture>(1);
            mat.textures.add(tex);
        }
        if (texNormalFilename != null) {
            ModelTexture tex = new ModelTexture();
            tex.usage = ModelTexture.USAGE_NORMAL;
            tex.fileName = new String(texNormalFilename);
            if (mat.textures == null)
                mat.textures = new Array<ModelTexture>(1);
            mat.textures.add(tex);
        }
        materials.add(mat);
    }

    public ModelMaterial getMaterial(final String name) {
        for (final ModelMaterial m : materials)
            if (m.id.equals(name))
                return m;
        ModelMaterial mat = new ModelMaterial();
        mat.id = name;
        mat.diffuse = new Color(Color.WHITE);
        materials.add(mat);
        return mat;
    }
}
