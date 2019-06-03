/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public abstract class ModelCreator {
    public interface IFace {
        public int[] v();

        public int[] n();

        public void setNormals(int... n);
    }

    /**
     * Contains the index info for a face.
     * @author Toni Sagrista
     *
     */
    public class Face implements IFace {
        /** This stores the indices for both the vertices and the UV coordinates **/
        public int[] v;

        /** This stores the indices for the normals **/
        public int[] n;

        /**
         * Constructs a face with the indeces of the vertices.
         * @param v Indeces of the vertices.
         */
        public Face(int... v) {
            this.v = v;
        }

        /**
         * Sets the normal indeces.
         * @param n Indeces of the normals.
         */
        public void setNormals(int... n) {
            this.n = n;
        }

        @Override
        public int[] v() {
            return v;
        }

        @Override
        public int[] n() {
            return n;
        }
    }

    protected void addFace(List<IFace> faces, boolean flipNormals, int... v) {
        if (flipNormals) {
            faces.add(new Face(flip(v, 1)));
        } else {
            faces.add(new Face(v));
        }
    }

    protected int[] flip(int[] v, int startIndex) {
        for (int i = startIndex; i < v.length / 2; i++) {
            int temp = v[i];
            v[i] = v[v.length - i + startIndex - 1];
            v[v.length - i + startIndex - 1] = temp;
        }
        return v;
    }

    public String name;
    public List<Vector3> vertices;
    public List<Vector3> normals;
    public List<Vector2> uv;
    public List<IFace> faces;
    protected int index;
    protected boolean flipNormals;
    protected boolean hardEdges;

    public ModelCreator() {
        this.vertices = new ArrayList<Vector3>();
        this.normals = new ArrayList<Vector3>();
        this.uv = new ArrayList<Vector2>();
        this.faces = new ArrayList<IFace>();
        this.index = 1;
    }

    /**
     * Exports the model to the .obj (Wavefront) format in the given output stream. 
     * @param os The output stream.
     * @throws IOException
     */
    public void dumpObj(OutputStream os) throws IOException {
        INumberFormat nf = NumberFormatFactory.getFormatter("########0.000000");
        OutputStreamWriter osw = new OutputStreamWriter(os);
        osw.append("# Created by " + this.getClass().getSimpleName() + " - ARI - ZAH - Heidelberg Universitat\n");
        osw.append("o " + name + "\n");
        // Write vertices
        for (Vector3 vertex : vertices) {
            osw.append("v " + nf.format(vertex.x) + " " + nf.format(vertex.y) + " " + nf.format(vertex.z) + "\n");
        }

        // Write vertex normals
        for (Vector3 vertex : normals) {
            osw.append("vn " + nf.format(vertex.x) + " " + nf.format(vertex.y) + " " + nf.format(vertex.z) + "\n");
        }

        //osw.append("s 1\n");

        // Write faces
        for (IFace face : faces) {
            // All vertices of a face share the same normal
            osw.append("f ");
            int v[] = face.v();
            for (int i = 0; i < v.length; i++) {
                osw.append(idx(face.v()[i], face.n()[i]));
                if (i != v.length - 1) {
                    osw.append(" ");
                }
            }
            osw.append("\n");
        }

        osw.flush();
        osw.close();
    }

    /**
     * Constructs the face string for the given vertex.
     * @param vi The vertex index.
     * @param ni The normal index
     * @return
     */
    private String idx(int vi, int ni) {
        return vi + "//" + ni;
    }
}
