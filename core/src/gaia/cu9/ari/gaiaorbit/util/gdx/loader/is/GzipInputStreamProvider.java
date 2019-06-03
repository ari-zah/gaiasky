/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.loader.is;

import com.badlogic.gdx.files.FileHandle;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class GzipInputStreamProvider implements InputStreamProvider {
    @Override
    public InputStream getInputStream(FileHandle f) throws IOException {
        return new GZIPInputStream(f.read());
    }
}
