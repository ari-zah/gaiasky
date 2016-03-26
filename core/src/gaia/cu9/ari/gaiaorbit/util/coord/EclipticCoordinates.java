package gaia.cu9.ari.gaiaorbit.util.coord;

import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.util.Date;

public class EclipticCoordinates extends OrbitLintCoordinates 
{
    @Override
    public Vector3d getEclipticSphericalCoordinates(Date date, Vector3d out) {
        return null;
    }

    @Override
    public Vector3d getEquatorialCartesianCoordinates( Date date, Vector3d out ) 
    {
        boolean inRange = data.loadPoint(out, date);
        out.rotate( AstroUtils.obliquity( AstroUtils.getJulianDate( date ) ), 0, 0, 1 );//.mul(Coordinates.equatorialToEcliptic());
        return inRange ? out : null;
    }

}
