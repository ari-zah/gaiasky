/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import net.jafama.FastMath;
import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;

import java.io.Serializable;

/**
 * Vector of arbitrary precision floating point numbers using ApFloat.
 *
 * @author Toni Sagrista
 */
public class Vector3b implements Serializable {
    private static final long serialVersionUID = 3840054589595372522L;
    // Number of digits of precision
    private static final int prec = 50;

    /** the x-component of this vector **/
    public Apfloat x;
    /** the y-component of this vector **/
    public Apfloat y;
    /** the z-component of this vector **/
    public Apfloat z;

    public final static Vector3b X = new Vector3b(1, 0, 0);
    public final static Vector3b Y = new Vector3b(0, 1, 0);
    public final static Vector3b Z = new Vector3b(0, 0, 1);


    /** Constructs a vector at (0,0,0) */
    public Vector3b() {
        this.x = Apfloat.ZERO;
        this.y = Apfloat.ZERO;
        this.z = Apfloat.ZERO;
    }

    /**
     * Creates a vector with the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     */
    public Vector3b(Apfloat x, Apfloat y, Apfloat z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Creates a vector with the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     */
    public Vector3b(double x, double y, double z) {
        this.x = new Apfloat(x, prec);
        this.y = new Apfloat(y, prec);
        this.z = new Apfloat(z, prec);
    }

    /**
     * Creates a vector from the given vector
     *
     * @param vector The vector
     */
    public Vector3b(final Vector3b vector) {
        this.set(vector);
    }

    /**
     * Creates a vector from the given array. The array must have at least 3
     * elements.
     *
     * @param values The array
     */
    public Vector3b(final double[] values) {
        this.set(values[0], values[1], values[2]);
    }

    public double x() {
        return x.doubleValue();
    }

    public Apfloat xb() {
        return x;
    }

    public double y() {
        return y.doubleValue();
    }

    public Apfloat yb() {
        return y;
    }

    public double z() {
        return z.doubleValue();
    }

    public Apfloat zb() {
        return z;
    }

    /**
     * Sets the vector to the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @return this vector for chaining
     */
    public Vector3b set(float x, float y, float z) {
        this.x = new Apfloat(x, prec);
        this.y = new Apfloat(y, prec);
        this.z = new Apfloat(z, prec);
        return this;
    }

    /**
     * Sets the vector to the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @return this vector for chaining
     */
    public Vector3b set(double x, double y, double z) {
        this.x = new Apfloat(x, prec);
        this.y = new Apfloat(y, prec);
        this.z = new Apfloat(z, prec);
        return this;
    }

    /**
     * Sets the vector to the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @return this vector for chaining
     */
    public Vector3b set(Apfloat x, Apfloat y, Apfloat z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3b set(final Vector3b vec) {
        if (vec != null)
            return this.set(vec.x, vec.y, vec.z);
        return this;
    }

    public Vector3b set(final Vector3d vec) {
        if (vec != null)
            return this.set(vec.x, vec.y, vec.z);
        return this;
    }

    public Vector3b set(final Vector3 vec) {
        if (vec != null)
            return this.set(vec.x, vec.y, vec.z);
        return this;
    }

    public Vector3 put(final Vector3 vec) {
        return vec.set(this.x.floatValue(), this.y.floatValue(), this.z.floatValue());
    }

    public Vector3 tov3() {
        return new Vector3(this.x.floatValue(), this.y.floatValue(), this.z.floatValue());
    }

    public Vector3d put(final Vector3d vec) {
        return vec.set(this.x.doubleValue(), this.y.doubleValue(), this.z.doubleValue());
    }

    public Vector3d tov3d() {
        return new Vector3d(this.x.floatValue(), this.y.floatValue(), this.z.floatValue());
    }

    public Vector3b put(final Vector3b vec) {
        return vec.set(this.x, this.y, this.z);
    }

    /**
     * Sets the components from the array. The array must have at least 3 elements
     *
     * @param vals The array
     * @return this vector for chaining
     */
    public Vector3b set(final double[] vals) {
        return this.set(vals[0], vals[1], vals[2]);
    }

    /**
     * Sets the components from the array. The array must have at least 3 elements
     *
     * @param vals The array
     * @return this vector for chaining
     */
    public Vector3b set(final float[] vals) {
        return this.set(vals[0], vals[1], vals[2]);
    }

    /**
     * Sets the components from the given spherical coordinate
     *
     * @param azimuthalAngle The angle between x-axis in radians [0, 2pi]
     * @param polarAngle     The angle between z-axis in radians [0, pi]
     * @return This vector for chaining
     */
    public Vector3b setFromSpherical(double azimuthalAngle, double polarAngle) {
        double cosPolar = MathUtilsd.cos(polarAngle);
        double sinPolar = MathUtilsd.sin(polarAngle);

        double cosAzim = MathUtilsd.cos(azimuthalAngle);
        double sinAzim = MathUtilsd.sin(azimuthalAngle);

        return this.set(cosAzim * sinPolar, sinAzim * sinPolar, cosPolar);
    }

    public Vector3b setToRandomDirection() {
        double u = MathUtilsd.random();
        double v = MathUtilsd.random();

        double theta = MathUtilsd.PI2 * u; // azimuthal angle
        double phi = Math.acos(2f * v - 1f); // polar angle

        return this.setFromSpherical(theta, phi);
    }

    public Vector3b cpy() {
        return new Vector3b(this);
    }

    public Vector3b add(final Vector3b vec) {
        this.x = this.x.add(vec.x);
        this.y = this.y.add(vec.y);
        this.z = this.z.add(vec.z);
        return this;
    }

    public Vector3b add(final Vector3d vec) {
        this.x = this.x.add(new Apfloat(vec.x));
        this.y = this.y.add(new Apfloat(vec.y));
        this.z = this.z.add(new Apfloat(vec.z));
        return this;
    }

    public Vector3b add(final Vector3 vec) {
        this.x = this.x.add(new Apfloat(vec.x));
        this.y = this.y.add(new Apfloat(vec.y));
        this.z = this.z.add(new Apfloat(vec.z));
        return this;
    }

    /**
     * Adds the given vector to this component
     *
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return This vector for chaining.
     */
    public Vector3b add(double x, double y, double z) {
        this.x = this.x.add(new Apfloat(x));
        this.y = this.y.add(new Apfloat(y));
        this.z = this.z.add(new Apfloat(z));
        return this;
    }

    /**
     * Adds the given vector to this component
     *
     * @param vals The 3-value double vector.
     * @return This vector for chaining.
     */
    public Vector3b add(double... vals) {
        assert vals.length == 3 : "vals must contain 3 values";
        this.x = this.x.add(new Apfloat(vals[0]));
        this.y = this.y.add(new Apfloat(vals[1]));
        this.z = this.z.add(new Apfloat(vals[2]));
        return this;
    }

    /**
     * Adds the given value to all three components of the vector.
     *
     * @param value The value
     * @return This vector for chaining
     */
    public Vector3b add(double value) {
        var val = new Apfloat(value);
        x = x.add(val);
        y = y.add(val);
        z = z.add(val);
        return this;
    }

    public Vector3b sub(final Vector3b vec) {
        x = x.subtract(vec.x);
        y = y.subtract(vec.y);
        z = z.subtract(vec.z);
        return this;
    }

    public Vector3b sub(final Vector3 a_vec) {
        return this.sub(a_vec.x, a_vec.y, a_vec.z);
    }

    public Vector3b sub(final Vector3d a_vec) {
        return this.sub(a_vec.x, a_vec.y, a_vec.z);
    }

    /**
     * Subtracts the other vector from this vector.
     *
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return This vector for chaining
     */
    public Vector3b sub(double x, double y, double z) {
        this.x = this.x.subtract(new Apfloat(x));
        this.y = this.y.subtract(new Apfloat(y));
        this.z = this.z.subtract(new Apfloat(z));
        return this;
    }

    /**
     * Subtracts the given value from all components of this vector
     *
     * @param value The value
     * @return This vector for chaining
     */
    public Vector3b sub(double value) {
        var val = new Apfloat(value);
        x = x.subtract(val);
        y = y.subtract(val);
        z = z.subtract(val);
        return this;
    }

    public Vector3b scl(Apfloat scl) {
        x = x.multiply(scl);
        y = y.multiply(scl);
        z = z.multiply(scl);
        return this;
    }

    public Vector3b scl(double scalar) {
        var scl = new Apfloat(scalar);
        x = x.multiply(scl);
        y = y.multiply(scl);
        z = z.multiply(scl);
        return this;
    }

    public Vector3b scl(final Vector3b vec) {
        x = x.multiply(vec.x);
        y = y.multiply(vec.y);
        z = z.multiply(vec.z);
        return this;
    }

    /**
     * Scales this vector by the given values
     *
     * @param x X value
     * @param y Y value
     * @param z Z value
     * @return This vector for chaining
     */
    public Vector3b scl(double x, double y, double z) {
        this.x = this.x.multiply(new Apfloat(x));
        this.y = this.y.multiply(new Apfloat(y));
        this.z = this.z.multiply(new Apfloat(z));
        return this;
    }

    public Vector3b mulAdd(Vector3b vec, double scalar) {
        Apfloat scl = new Apfloat(scalar);
        x = x.add(vec.x.multiply(scl));
        y = y.add(vec.y.multiply(scl));
        z = z.add(vec.z.multiply(scl));
        return this;
    }

    public Vector3b mulAdd(Vector3b vec, Vector3b mulVec) {
        x = x.add(vec.x.multiply(mulVec.x));
        y = y.add(vec.y.multiply(mulVec.y));
        z = z.add(vec.z.multiply(mulVec.z));
        return this;
    }

    public Vector3b mul(Vector3b vec) {
        x = x.multiply(vec.x);
        y = y.multiply(vec.y);
        z = z.multiply(vec.z);
        return this;
    }

    public Vector3b div(Vector3b vec) {
        x = x.divide(vec.x);
        y = y.divide(vec.y);
        z = z.divide(vec.z);
        return this;
    }

    /** @return The euclidian length */
    public static double len(final double x, final double y, final double z) {
        return FastMath.sqrt(x * x + y * y + z * z);
    }

    public double lend() {
        return this.len().doubleValue();
    }

    public Apfloat len() {
        Apfloat sumSq = x.multiply(x).add(y.multiply(y)).add(z.multiply(z));
        return ApfloatMath.sqrt(sumSq);
    }

    /** @return The squared euclidian length */
    public static double len2(final double x, final double y, final double z) {
        return x * x + y * y + z * z;
    }

    public double len2d() {
        return this.len2().doubleValue();
    }

    public Apfloat len2() {
        return x.multiply(x).add(y.multiply(y)).add(z.multiply(z));
    }

    /**
     * @param vec The other vector
     * @return Wether this and the other vector are equal
     */
    public boolean idt(final Vector3b vec) {
        return x == vec.x && y == vec.y && z == vec.z;
    }

    public double dstd(final Vector3b vec) {
        return dst(vec).doubleValue();
    }

    public Apfloat dst(final Vector3b vec) {
        Apfloat a = vec.x.subtract(this.x);
        Apfloat b = vec.y.subtract(this.y);
        Apfloat c = vec.z.subtract(this.z);
        return ApfloatMath.sqrt(a.multiply(a).add(b.multiply(b)).add(c.multiply(c)));
    }

    public double dstd(double x, double y, double z) {
        return dst(x, y, z).doubleValue();
    }

    /** @return the distance between this point and the given point */
    public Apfloat dst(double x, double y, double z) {
        Apfloat a = new Apfloat(x).subtract(this.x);
        Apfloat b = new Apfloat(y).subtract(this.y);
        Apfloat c = new Apfloat(z).subtract(this.z);
        return ApfloatMath.sqrt(a.multiply(a).add(b.multiply(b)).add(c.multiply(c)));
    }

    public double dst2d(Vector3b vec) {
        return this.dst2(vec).doubleValue();
    }

    public Apfloat dst2(Vector3b vec) {
        Apfloat a = vec.x.subtract(this.x);
        Apfloat b = vec.y.subtract(this.y);
        Apfloat c = vec.z.subtract(this.z);
        return a.multiply(a).add(b.multiply(b)).add(c.multiply(c));
    }

    public double dst2d(double x, double y, double z) {
        return dst2(x, y, z).doubleValue();
    }

    /**
     * Returns the squared distance between this point and the given point
     *
     * @param x The x-component of the other point
     * @param y The y-component of the other point
     * @param z The z-component of the other point
     * @return The squared distance
     */
    public Apfloat dst2(double x, double y, double z) {
        Apfloat a = new Apfloat(x).subtract(this.x);
        Apfloat b = new Apfloat(y).subtract(this.y);
        Apfloat c = new Apfloat(z).subtract(this.z);
        return a.multiply(a).add(b.multiply(b)).add(c.multiply(c));
    }

    public Vector3b nor() {
        final Apfloat len2 = this.len2();
        final double len2d = len2.doubleValue();
        if (len2d == 0f || len2d == 1f)
            return this;
        return this.scl(Apfloat.ONE.divide(ApfloatMath.sqrt(len2)));
    }

    public double dotd(final Vector3b vec) {
        return this.dot(vec).doubleValue();
    }

    public Apfloat dot(final Vector3b vec) {
        return this.x.multiply(vec.x).add(this.y.multiply(vec.y)).add(this.z.multiply(vec.z));
    }

    public double dotd(double x, double y, double z) {
        return this.dot(new Apfloat(x), new Apfloat(y), new Apfloat(z)).doubleValue();
    }

    /**
     * Returns the dot product between this and the given vector.
     *
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return The dot product
     */
    public Apfloat dot(Apfloat x, Apfloat y, Apfloat z) {
        return this.x.multiply(x).add(this.y.multiply(y)).add(this.z.multiply(z));
    }

    /**
     * Sets this vector to the cross product between it and the other vector.
     *
     * @param vec The other vector
     * @return This vector for chaining
     */
    public Vector3b crs(final Vector3b vec) {
        return this.set(this.y.multiply(vec.z).subtract(this.z.multiply(vec.y)), this.z.multiply(vec.x).subtract(this.x.multiply(vec.z)), this.x.multiply(vec.y).subtract(this.y.multiply(vec.x)));
    }

    /**
     * Calculates the outer product of two given vectors <code>v</code> and
     * <code>w</code> and returns the result as a new <code>GVector3d</code>.
     *
     * @param v left operand
     * @param w right operand
     * @return outer product of <code>v</code> and <code>w</code>
     */
    static public Vector3b crs(final Vector3b v, final Vector3b w) {
        final Vector3b res = new Vector3b(v);

        return res.crs(w);
    }

    /**
     * Sets this vector to the cross product between it and the other vector.
     *
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return This vector for chaining
     */
    public Vector3b crs(double x, double y, double z) {
        Apfloat vx = new Apfloat(x);
        Apfloat vy = new Apfloat(y);
        Apfloat vz = new Apfloat(z);
        return this.set(this.y.multiply(vz).subtract(this.z.multiply(vy)), this.z.multiply(vx).subtract(this.x.multiply(vz)), this.x.multiply(vy).subtract(this.y.multiply(vx)));
    }

    /**
     * Sets the matrix aux to a translation matrix using this vector
     *
     * @param aux
     * @return The matrix aux
     */
    public Matrix4 getMatrix(Matrix4 aux) {
        return aux.idt().translate(x.floatValue(), y.floatValue(), z.floatValue());
    }
    /**
     * Sets the matrix aux to a translation matrix using this vector
     *
     * @param aux
     * @return The matrix aux
     */
    public Matrix4d getMatrix(Matrix4d aux) {
        return aux.idt().translate(x.doubleValue(), y.doubleValue(), z.doubleValue());
    }

    public boolean isUnit() {
        return isUnit(0.000000001);
    }

    public boolean isUnit(final double margin) {
        return Math.abs(len2d() - 1f) < margin;
    }

    public boolean isZero() {
        return x.doubleValue() == 0 && y.doubleValue() == 0 && z.doubleValue() == 0;
    }

    public boolean isZero(final double margin) {
        return len2d() < margin;
    }

    public String toString() {
        return x + "," + y + "," + z;
    }

    public Vector3b setLength(double len) {
        return setLength2(len * len);
    }

    public Vector3b setLength2(double len2) {
        double oldLen2 = len2d();
        return (oldLen2 == 0 || oldLen2 == len2) ? this : scl(Math.sqrt(len2 / oldLen2));
    }

    public Vector3b clamp(double min, double max) {
        final double l2 = len2d();
        if (l2 == 0f)
            return this;
        if (l2 > max * max)
            return nor().scl(max);
        if (l2 < min * min)
            return nor().scl(min);
        return this;
    }

    public Apfloat[] values() {
        return new Apfloat[] { x, y, z };
    }

    public double[] valuesd() {
        return new double[] { x.doubleValue(), y.doubleValue(), z.doubleValue() };
    }

    public float[] valuesf() {
        return new float[] { x.floatValue(), y.floatValue(), z.floatValue() };
    }

    public float[] valuesf(float[] vec) {
        vec[0] = x.floatValue();
        vec[1] = y.floatValue();
        vec[2] = z.floatValue();
        return vec;
    }

    /**
     * Scales a given vector with a scalar and add the result to this one, i.e.
     * <code>this = this + s*v</code>.
     *
     * @param s scalar scaling factor
     * @param v vector to scale
     * @return vector modified in place
     */
    public Vector3b scaleAdd(final double s, final Vector3b v) {
        return this.add(v.scl(s));
    }

    /**
     * Returns a vector3 representation of this vector by casting the doubles to
     * floats. This creates a new object
     *
     * @return The vector3 representation of this vector3d
     */
    public Vector3 toVector3() {
        return tov3();
    }

    /**
     * Returns a vector3d representation of this vector by casting the Apfloats to
     * doubles. This creates a new object
     *
     * @return The vector3d representation of this vector3b
     */
    public Vector3d toVector3d() {
        return tov3d();
    }

    /**
     * Returns set v to this vector by casting doubles to floats.
     *
     * @return The float vector v.
     */
    public Vector3 setVector3(Vector3 v) {
        return v.set(x.floatValue(), y.floatValue(), z.floatValue());
    }

    /**
     * Returns set v to this vector by casting Apfloats to doubles.
     *
     * @return The double vector v.
     */
    public Vector3d setVector3d(Vector3d v) {
        return v.set(x.doubleValue(), y.doubleValue(), z.doubleValue());
    }

    /** Gets the angle in degrees between the two vectors **/
    public double angle(Vector3b v) {
        return MathUtilsd.radiansToDegrees * FastMath.acos(this.dotd(v) / (this.lend() * v.lend()));
    }

    /** Gets the angle in degrees between the two vectors **/
    public double anglePrecise(Vector3b v) {
        return MathUtilsd.radiansToDegrees * Math.acos(this.dotd(v) / (this.lend() * v.lend()));
    }

    @Override
    public int hashCode() {
        final long prime = 31;
        long result = 1;
        result = prime * result + x.hashCode();
        result = prime * result + y.hashCode();
        result = prime * result + z.hashCode();
        return (int) result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Vector3b other = (Vector3b) obj;
        if (x.hashCode() != other.x.hashCode())
            return false;
        if (y.hashCode() != other.y.hashCode())
            return false;
        if (z.hashCode() != other.z.hashCode())
            return false;
        return true;
    }

    public Vector3b setZero() {
        this.x = Apfloat.ZERO;
        this.y = Apfloat.ZERO;
        this.z = Apfloat.ZERO;
        return this;
    }

}
