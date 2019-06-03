/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;

/**
 * Helper generic class to create rings.
 * 
 * @author Toni Sagrista
 *
 */
public class RingCreator extends ModelCreator {

    float innerRadius, outerRadius, startAngle, endAngle;

    public RingCreator() {
        super();
        this.name = "Ring";
    }

    /**
     * Creates a new ring with the given parameters.
     * 
     * @param innerRadius
     *            The inner radius length.
     * @param outerRadius
     *            The outer radius length.
     * @param flipNormals
     *            Whether to flip normals or not.
     * @return This ring creator, for chaining
     */
    public RingCreator create(int divisions, float innerRadius, float outerRadius, boolean flipNormals) {
        return create(divisions, innerRadius, outerRadius, flipNormals, 0, 360);
    }

    /**
     * Creates a new ring with the given parameters
     * 
     * @param innerRadius
     *            The inner radius length
     * @param outerRadius
     *            The outer radius length
     * @param flipNormals
     *            Whether to flip normals or not
     * @return This ring creator, for chaining
     */
    public RingCreator create(int divisions, float innerRadius, float outerRadius, boolean flipNormals, float startAngle, float endAngle) {
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        float angleStep = (endAngle - startAngle) / divisions;

        float angle = startAngle;
        // Previous indices
        int prev1 = addVertex(new Vector3(1, 0, 0).scl(innerRadius).rotate(angle, 0, 1, 0), angle);
        int prev2 = addVertex(new Vector3(1, 0, 0).scl(outerRadius).rotate(angle, 0, 1, 0), angle);
        for (angle = startAngle + angleStep; angle <= endAngle; angle += angleStep) {
            int i1 = addVertex(new Vector3(1, 0, 0).scl(innerRadius).rotate(angle, 0, 1, 0), angle);
            int i2 = addVertex(new Vector3(1, 0, 0).scl(outerRadius).rotate(angle, 0, 1, 0), angle);

            // Add faces
            addFace(faces, flipNormals, prev1, prev2, i2, i1);

            prev1 = i1;
            prev2 = i2;

        }
        addNormals();
        return this;
    }

    protected int addVertex(Vector3 p, float angle) {
        addUV(p, angle);
        vertices.add(p);

        return index++;
    }

    protected void addUV(Vector3 p, float angle) {
        float u = MathUtilsd.lint(angle, startAngle, endAngle, 0, 1);
        float v = equals(p.len(), innerRadius) ? 0 : 1f;

        uv.add(new Vector2(u, v));

    }

    private boolean equals(float one, float two) {
        return Math.abs(one - two) < 0.0001f;
    }

    protected void addNormals() {
        // Each face has only one normal

        for (IFace face : faces) {
            // Calculate face normal, shared amongst all vertices
            Vector3 a = vertices.get(face.v()[1] - 1).cpy().sub(vertices.get(face.v()[0] - 1));
            Vector3 b = vertices.get(face.v()[2] - 1).cpy().sub(vertices.get(face.v()[1] - 1));
            normals.add(a.crs(b).nor());

            // Add index to face
            int idx = normals.size();
            face.setNormals(idx, idx, idx, idx);
        }
    }

}
