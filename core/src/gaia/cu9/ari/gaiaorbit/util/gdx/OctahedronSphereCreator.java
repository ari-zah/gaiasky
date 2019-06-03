/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.*;

public class OctahedronSphereCreator extends ModelCreator {

    private Map<Long, Integer> middlePointIndexCache;

    /**
     * Adds a vertex and its UV mapping.
     * @param p The point.
     * @param radius The radius.
     * @return
     */
    protected int addVertex(Vector3 p, float radius) {
        p.nor();

        // Vertex is p times the radius
        vertices.add(p.scl(radius));

        return index++;
    }

    /**
     * Implements the spherical UV mapping
     */
    protected void addUV(Set<Integer> seam) {
        int idx = 0;
        for (Vector3 vertex : vertices) {
            Vector3 p = new Vector3(vertex);
            p.nor();
            // UV
            float u = 0.5f + (float) (Math.atan2(p.z, p.x) / (Math.PI * 2.0));
            float v = 0.5f - (float) (Math.asin(p.y) / Math.PI);

            if (seam.contains(idx + 1))
                v = 1f;

            uv.add(new Vector2(u, v));

            idx++;
        }
    }

    private void addNormals() {
        for (IFace face : faces) {
            // Calculate normals
            if (hardEdges) {
                // Calculate face normal, shared amongst all vertices
                Vector3 a = vertices.get(face.v()[1] - 1).cpy().sub(vertices.get(face.v()[0] - 1));
                Vector3 b = vertices.get(face.v()[2] - 1).cpy().sub(vertices.get(face.v()[1] - 1));
                normals.add(a.crs(b).nor());

                // Add index to face
                int idx = normals.size();
                face.setNormals(idx, idx, idx);

            } else {
                // Just add the vertex normal
                normals.add(vertices.get(face.v()[0] - 1).cpy().nor());
                normals.add(vertices.get(face.v()[1] - 1).cpy().nor());
                normals.add(vertices.get(face.v()[2] - 1).cpy().nor());

                // Add indices to face
                int idx = normals.size();
                face.setNormals(idx - 2, idx - 1, idx);
            }
        }
    }

    // return index of point in the middle of p1 and p2
    private int getMiddlePoint(int p1, int p2, float radius, Set<Integer> seam) {
        boolean inSeam = seam.contains(p1) && seam.contains(p2);

        // first check if we have it already
        boolean firstIsSmaller = p1 < p2;
        Long smallerIndex = (long) (firstIsSmaller ? p1 : p2);
        Long greaterIndex = (long) (firstIsSmaller ? p2 : p1);
        Long key = (smallerIndex << 32) + greaterIndex;

        if (!inSeam && this.middlePointIndexCache.containsKey(key)) {
            return middlePointIndexCache.get(key);
        }

        // not in cache, calculate it
        Vector3 point1 = this.vertices.get(p1 - 1);
        Vector3 point2 = this.vertices.get(p2 - 1);
        Vector3 middle = new Vector3((point1.x + point2.x) / 2.0f, (point1.y + point2.y) / 2.0f, (point1.z + point2.z) / 2.0f);

        middle.nor();
        // add vertex makes sure point is on unit sphere
        int i = addVertex(middle, radius);

        if (inSeam) {
            seam.add(i);
        } else {
            // store it only if not in seam, return index
            this.middlePointIndexCache.put(key, i);
        }
        return i;
    }

    public OctahedronSphereCreator create(float radius, int divisions, boolean flipNormals, boolean hardEdges) {
        if (divisions < 0 || divisions > 6) //-V6007
            throw new AssertionError("Divisions must be in [0..6]");
        this.flipNormals = flipNormals;
        this.hardEdges = hardEdges;
        this.middlePointIndexCache = new HashMap<Long, Integer>();

        // Add four top points (0,1,2,3)
        for (int i = 0; i < 4; i++)
            addVertex(new Vector3(0, 1, 0), radius);

        // Add five middle vertices, +z +x -z -x +z (4,5,6,7,8)
        addVertex(new Vector3(0, 0, 1), radius);
        addVertex(new Vector3(1, 0, 0), radius);
        addVertex(new Vector3(0, 0, -1), radius);
        addVertex(new Vector3(-1, 0, 0), radius);
        addVertex(new Vector3(0, 0, 1), radius);

        // Add four bottom points  (9,10,11,12)
        for (int i = 0; i < 4; i++)
            addVertex(new Vector3(0, -1, 0), radius);

        /**
         *  Now the 8 faces are:
         *  
         *  TOP HALF
         *  1-4-5
         *  2-5-6
         *  3-6-7
         *  0-7-8
         *  
         *  BOTTOM HALF
         *  ?
         */

        // SEAM (+1): 1-5-13
        Set<Integer> seam = new HashSet<Integer>();
        seam.add(1);
        seam.add(9);

        List<IFace> faces = new ArrayList<IFace>();

        // 4 top faces
        addFace(faces, flipNormals, 2,5,6);
        addFace(faces, flipNormals, 3,6,7);
        addFace(faces, flipNormals, 4,7,8);
        addFace(faces, flipNormals, 1,8,9);

        // 4 bottom faces
        //        addFace(faces, flipNormals, 13, 5, 6);
        //        addFace(faces, flipNormals, 12, 8, 9);
        //        addFace(faces, flipNormals, 11, 7, 8);
        //        addFace(faces, flipNormals, 10, 6, 7);

        // refine triangles
        for (int i = 0; i < divisions; i++) {
            List<IFace> faces2 = new ArrayList<IFace>();
            for (IFace face : faces) {
                // replace triangle by 4 triangles
                int f0 = face.v()[0];
                int f1 = face.v()[1];
                int f2 = face.v()[2];
                int a = getMiddlePoint(f0, f1, radius, seam);
                int b = getMiddlePoint(f1, f2, radius, seam);
                int c = getMiddlePoint(f2, f0, radius, seam);

                addFace(faces2, flipNormals, face.v()[0], a, c);
                addFace(faces2, flipNormals, face.v()[1], b, a);
                addFace(faces2, flipNormals, face.v()[2], c, b);
                addFace(faces2, flipNormals, a, b, c);
            }
            faces = faces2;
        }
        this.faces = faces;

        addNormals();

        addUV(seam);

        return this;
    }
}
