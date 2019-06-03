/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.shader;

import com.badlogic.gdx.files.FileHandle;
import gaia.cu9.ari.gaiaorbit.assets.ShaderTemplatingLoader;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntRenderable;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.provider.DefaultIntShaderProvider;

public class RelativisticShaderProvider extends DefaultIntShaderProvider {
    public final RelativisticShader.Config config;

    public RelativisticShaderProvider(final RelativisticShader.Config config) {
        this.config = (config == null) ? new RelativisticShader.Config() : config;
    }

    public RelativisticShaderProvider(final String vertexShader, final String fragmentShader) {
        this(new RelativisticShader.Config(vertexShader, fragmentShader));
    }

    public RelativisticShaderProvider(final FileHandle vertexShader, final FileHandle fragmentShader) {
        this(ShaderTemplatingLoader.load(vertexShader), ShaderTemplatingLoader.load(fragmentShader));
    }

    public RelativisticShaderProvider() {
        this(null);
    }

    @Override
    protected IntShader createShader(final IntRenderable renderable) {
        return new RelativisticShader(renderable, config);
    }
}
