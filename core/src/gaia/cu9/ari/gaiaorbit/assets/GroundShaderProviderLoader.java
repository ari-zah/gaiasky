/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.GroundShaderProvider;

public class GroundShaderProviderLoader<T extends GroundShaderProviderLoader.GroundShaderProviderParameter> extends AsynchronousAssetLoader<GroundShaderProvider, T> {

    GroundShaderProvider shaderProvider;

    public GroundShaderProviderLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, T parameter) {
        shaderProvider = new GroundShaderProvider(Gdx.files.internal(parameter.vertexShader), Gdx.files.internal(parameter.fragmentShader));
    }

    @Override
    public GroundShaderProvider loadSync(AssetManager manager, String fileName, FileHandle file, GroundShaderProviderParameter parameter) {
        return shaderProvider;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, GroundShaderProviderParameter parameter) {
        return null;
    }

    public static class GroundShaderProviderParameter extends AssetLoaderParameters<GroundShaderProvider> {
        String vertexShader;
        String fragmentShader;

        public GroundShaderProviderParameter(String vertexShader, String fragmentShader) {
            super();
            this.vertexShader = vertexShader;
            this.fragmentShader = fragmentShader;
        }

    }

}
