/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;

public interface IQuadRenderable extends IRenderable {

    /**
     * Renders the renderable as a quad using the star shader.
     *
     * @param shader
     * @param alpha
     * @param mesh
     * @param camera
     */
    void render(ShaderProgram shader, float alpha, IntMesh mesh, ICamera camera);
}
