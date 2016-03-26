package gaia.cu9.ari.gaiaorbit.data.orbit;

import gaia.cu9.ari.gaiaorbit.data.orbit.OrbitDataLoader.OrbitDataLoaderParameter;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/**
 * Reads an orbit file into an OrbitData object.
 * @author Toni Sagrista
 *
 */
public class OrbitFileDataEclipticJulianTimeProvider implements IOrbitDataProvider {
    OrbitData data;

    @Override
    public void load(String file, OrbitDataLoaderParameter parameter) {
        FileDataLoaderEclipticJulianTime odl = new FileDataLoaderEclipticJulianTime();
        try {
            FileHandle f = Gdx.files.internal(file);
            data = odl.load(f.read());
            EventManager.instance.post(Events.ORBIT_DATA_LOADED, data, file);
        } catch (Exception e) {
            Gdx.app.error(OrbitFileDataEclipticJulianTimeProvider.class.getName(), e.getMessage());
        }
    }

    public OrbitData getData() {
        return data;
    }

}
