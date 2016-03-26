package gaia.cu9.ari.gaiaorbit.data.orbit;

import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.Calendar;

public class FileDataLoaderEclipticJulianTime 
{
    public FileDataLoaderEclipticJulianTime() 
    {
        super();
    }

    /**
     * Loads the data in the input stream into an OrbitData object.
     * @param data
     * @param referenceFrame
     * @throws Exception
     */
    public OrbitData load(InputStream data) throws Exception 
    {
        OrbitData orbitData = new OrbitData();

        BufferedReader br = new BufferedReader( new InputStreamReader( data ) );
        String line;

        Timestamp last = new Timestamp( 0 );
        while( ( line = br.readLine() ) != null ) 
        {
            if( !line.isEmpty() && !line.startsWith( "#" ) ) 
            {
                // Read line
                String[] tokens = line.split("\\s+");
                if( tokens.length >= 4 ) 
                {
                    // Valid data line
                    Timestamp t = new Timestamp( getTime( tokens[ 0 ] ) );
                    Matrix4d transform = new Matrix4d();
                    transform.scl( Constants.KM_TO_U );
                    if( !t.equals( last ) ) 
                    {
                        orbitData.time.add( t );

                        Vector3d pos = new Vector3d( parsed( tokens[ 1 ] ), parsed( tokens[ 2 ] ), parsed( tokens[ 3 ] ) );
                        pos.mul( transform );
                        orbitData.x.add( pos.y );
                        orbitData.y.add( pos.z );
                        orbitData.z.add( pos.x );
                        last.setTime( t.getTime() );
                    }
                }
            }
        }

        br.close();

        return orbitData;
    }

    protected float parsef(String str) {
        return Float.valueOf(str);
    }

    protected double parsed(String str) {
        return Double.valueOf(str);
    }

    protected int parsei(String str) {
        return Integer.valueOf(str);
    }

    private long getTime( String jds ) 
    {
        double jd = Double.valueOf( jds );
        int[] dt = AstroUtils.getCalendarDay( jd );
        Calendar cld = Calendar.getInstance();
        cld.set( dt[ 0 ], dt[ 1 ], dt[ 2 ], dt[ 3 ], dt[ 4 ], dt[ 5 ] );
        return cld.getTimeInMillis();
    }
}
