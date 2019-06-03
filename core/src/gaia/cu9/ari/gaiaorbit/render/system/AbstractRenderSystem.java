/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;

import java.util.Comparator;

public abstract class AbstractRenderSystem implements IRenderSystem {
    /**
     * When this is true, new point information is available, so new data is
     * streamed to the GPU
     **/
    public static boolean POINT_UPDATE_FLAG = true;

    protected ShaderProgram[] programs;
    private RenderGroup group;
    protected float[] alphas;
    /** Comparator of renderables, in case of need **/
    protected Comparator<IRenderable> comp;
    public RenderingContext rc;
    protected Vector3 aux;

    protected RenderSystemRunnable preRunnable, postRunnable;

    protected AbstractRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] programs) {
        super();
        this.group = rg;
        this.alphas = alphas;
        this.programs = programs;
        this.aux = new Vector3();
    }

    @Override
    public RenderGroup getRenderGroup() {
        return group;
    }


    @Override
    public void render(Array<IRenderable> renderables, ICamera camera, double t, RenderingContext rc) {
        if (renderables != null && renderables.size != 0) {
            this.rc = rc;
            run(preRunnable, renderables, camera);
            renderStud(renderables, camera, t);
            run(postRunnable, renderables, camera);
        }
    }

    public abstract void renderStud(Array<IRenderable> renderables, ICamera camera, double t);

    public void setPreRunnable(RenderSystemRunnable r) {
        preRunnable = r;
    }

    public void setPostRunnable(RenderSystemRunnable r) {
        postRunnable = r;
    }

    protected void run(RenderSystemRunnable runnable, Array<IRenderable> renderables, ICamera camera) {
        if (runnable != null) {
            runnable.run(this, renderables, camera);
        }
    }

    /**
     * Computes the alpha opacity value of a given renderable using its
     * component types
     * 
     * @param renderable
     *            The renderable
     * @return The alpha value as the product of all the alphas of its component
     *         types.
     */
    public float getAlpha(IRenderable renderable) {
        return getAlpha(renderable.getComponentType());
    }

    public float getAlpha(ComponentTypes ct) {
        int idx = -1;
        float alpha = 1f;
        while ((idx = ct.nextSetBit(idx + 1)) >= 0) {
            alpha *= alphas[idx];
        }
        return alpha;
    }


    @Override
    public void resize(int w, int h) {
        // Empty, to override in subclasses if needed
    }

    @Override
    public void updateBatchSize(int w, int h) {
        // Empty by default
    }

    public interface RenderSystemRunnable {
        void run(AbstractRenderSystem renderSystem, Array<IRenderable> renderables, ICamera camera);
    }

    protected void addEffectsUniforms(ShaderProgram shaderProgram, ICamera camera) {
        addRelativisticUniforms(shaderProgram, camera);
        addGravWaveUniforms(shaderProgram);
    }

    protected void addRelativisticUniforms(ShaderProgram shaderProgram, ICamera camera) {
        if (GlobalConf.runtime.RELATIVISTIC_ABERRATION) {
            RelativisticEffectsManager rem = RelativisticEffectsManager.getInstance();
            shaderProgram.setUniformf("u_velDir", rem.velDir);
            shaderProgram.setUniformf("u_vc", rem.vc);
        }
    }

    protected void addGravWaveUniforms(ShaderProgram shaderProgram) {
        if (GlobalConf.runtime.GRAVITATIONAL_WAVES) {
            RelativisticEffectsManager rem = RelativisticEffectsManager.getInstance();
            // Time in seconds - use simulation time
            shaderProgram.setUniformf("u_ts", rem.gwtime);
            // Wave frequency
            shaderProgram.setUniformf("u_omgw", rem.omgw);
            // Coordinates of wave (cartesian)
            shaderProgram.setUniformf("u_gw", rem.gw);
            // Transformation matrix 
            shaderProgram.setUniformMatrix("u_gwmat3", rem.gwmat3);
            // H terms - hpluscos, hplussin, htimescos, htimessin
            shaderProgram.setUniform4fv("u_hterms", rem.hterms, 0, 4);
        }
    }

    protected ShaderProgram getShaderProgram() {
        try {
            if (GlobalConf.runtime.RELATIVISTIC_ABERRATION && GlobalConf.runtime.GRAVITATIONAL_WAVES)
                return programs[3];
            else if (GlobalConf.runtime.RELATIVISTIC_ABERRATION)
                return programs[1];
            else if (GlobalConf.runtime.GRAVITATIONAL_WAVES)
                return programs[2];
        } catch (Exception e) {
        }
        return programs[0];
    }

}
