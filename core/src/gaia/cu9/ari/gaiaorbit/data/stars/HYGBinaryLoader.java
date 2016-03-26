package gaia.cu9.ari.gaiaorbit.data.stars;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import gaia.cu9.ari.gaiaorbit.data.ISceneGraphLoader;
import gaia.cu9.ari.gaiaorbit.scenegraph.Particle;
import gaia.cu9.ari.gaiaorbit.scenegraph.Star;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Loads the HYG catalog in binary (own) format. The format is defined as follows
 *
 * - 32 bits (int) with the number of stars, starNum
 * repeat the following starNum times (for each star)
 * - 32 bits (int) - The the length of the name, or nameLength
 * - 16 bits * nameLength (chars) - The name of the star
 * - 32 bits (float) - appmag
 * - 32 bits (float) - absmag
 * - 32 bits (float) - colorbv
 * - 32 bits (float) - ra
 * - 32 bits (float) - dec
 * - 32 bits (float) - distance
 * - 64 bits (long) - id
 *
 * @author Toni Sagrista
 *
 */
public class HYGBinaryLoader extends AbstractCatalogLoader implements ISceneGraphLoader {

    @Override
    public List<Particle> loadData() throws FileNotFoundException {
        List<Particle> stars = new ArrayList<Particle>();
        for (String f : files) {
            FileHandle file = Gdx.files.internal(f);
            InputStream data = file.read();
            DataInputStream data_in = new DataInputStream(data);

            Logger.info(this.getClass().getSimpleName(), I18n.bundle.format("notif.limitmag", GlobalConf.data.LIMIT_MAG_LOAD));
            Logger.info(this.getClass().getSimpleName(), I18n.bundle.format("notif.catalog.loading", file.name()));

            try {
                // Read size of stars
                int size = data_in.readInt();
                for (int idx = 0; idx < size; idx++) {
                    try {
                        // name_length, name, appmag, absmag, colorbv, ra, dec, dist
                        int nameLength = data_in.readInt();
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < nameLength; i++) {
                            sb.append(data_in.readChar());
                        }
                        String name = sb.toString();
                        float appmag = data_in.readFloat();
                        float absmag = data_in.readFloat();
                        float colorbv = data_in.readFloat();
                        float ra = data_in.readFloat();
                        float dec = data_in.readFloat();
                        float dist = data_in.readFloat();
                        long id = data_in.readInt();
                        int hip = data_in.readInt();
                        if (appmag < GlobalConf.data.LIMIT_MAG_LOAD) {
                            Vector3d pos = Coordinates.sphericalToCartesian(Math.toRadians(ra), Math.toRadians(dec), dist, new Vector3d());

                            Star s = new Star(pos, appmag, absmag, colorbv, name, ra, dec, id, hip, (byte) 2);
                            if (runFiltersAnd(s))
                                stars.add(s);
                        }
                    } catch (EOFException eof) {
                        Logger.error(eof, HYGBinaryLoader.class.getSimpleName());
                    }
                }

            } catch (IOException e) {
                Logger.error(e, HYGBinaryLoader.class.getSimpleName());
            } finally {
                try {
                    data_in.close();
                } catch (IOException e) {
                    Logger.error(e);
                }

            }
        }

        Logger.info(this.getClass().getSimpleName(), I18n.bundle.format("notif.catalog.init", stars.size()));
        return stars;
    }

}
