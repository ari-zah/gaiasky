/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.shader;

import com.badlogic.gdx.files.FileHandle;
import gaia.cu9.ari.gaiaorbit.assets.ShaderTemplatingLoader;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntRenderable;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.provider.DefaultIntShaderProvider;

public class GroundShaderProvider extends DefaultIntShaderProvider {
    public final GroundShader.Config config;

    public GroundShaderProvider(final GroundShader.Config config) {
        this.config = (config == null) ? new GroundShader.Config() : config;
    }

    public GroundShaderProvider(final String vertexShader, final String fragmentShader) {
        this(new GroundShader.Config(vertexShader, fragmentShader));
    }

    public GroundShaderProvider(final FileHandle vertexShader, final FileHandle fragmentShader) {
        this(ShaderTemplatingLoader.load(vertexShader), ShaderTemplatingLoader.load(fragmentShader));
    }

    public GroundShaderProvider() {
        this(null);
    }

    @Override
    protected IntShader createShader(final IntRenderable renderable) {
        return new GroundShader(renderable, config);
    }
}
