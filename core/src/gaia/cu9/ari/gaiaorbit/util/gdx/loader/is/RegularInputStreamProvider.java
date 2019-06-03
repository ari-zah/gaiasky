/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.loader.is;

import com.badlogic.gdx.files.FileHandle;

import java.io.InputStream;

public class RegularInputStreamProvider implements InputStreamProvider {
    @Override
    public InputStream getInputStream(FileHandle f) {
        return f.read();
    }
}
