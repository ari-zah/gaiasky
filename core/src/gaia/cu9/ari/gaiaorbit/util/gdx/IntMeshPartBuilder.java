/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaia.cu9.ari.gaiaorbit.util.gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool.Poolable;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.IntMeshPart;

public interface IntMeshPartBuilder {
    /** @return The {@link IntMeshPart} currently building. */
    IntMeshPart getMeshPart();

    /** @return The {@link VertexAttributes} available for building. */
    VertexAttributes getAttributes();

    /** Set the color used if no vertex color is provided, or null to not use a default color. */
    void setColor(final Color color);

    /** Set the color used if no vertex color is provided. */
    void setColor(float r, float g, float b, float a);

    /** Set range of texture coordinates used (default is 0,0,1,1). */
    void setUVRange(float u1, float v1, float u2, float v2);

    /** Set range of texture coordinates from the specified TextureRegion. */
    void setUVRange(TextureRegion r);

    /** Add one or more vertices, returns the index of the last vertex added. The length of values must a power of the vertex size. */
    int vertex(final float... values);

    /** Add a vertex, returns the index. Null values are allowed. Use {@link #getAttributes} to check which values are available. */
    int vertex(Vector3 pos, Vector3 nor, Color col, Vector2 uv);

    /** Add a vertex, returns the index. Use {@link #getAttributes} to check which values are available. */
    int vertex(final VertexInfo info);

    /** @return The index of the last added vertex. */
    int lastIndex();

    /** Add an index, IntMeshPartBuilder expects all meshes to be indexed. */
    void index(final int value);

    /** Add multiple indices, IntMeshPartBuilder expects all meshes to be indexed. */
    void index(int value1, int value2);

    /** Add multiple indices, IntMeshPartBuilder expects all meshes to be indexed. */
    void index(int value1, int value2, int value3);

    /** Add multiple indices, IntMeshPartBuilder expects all meshes to be indexed. */
    void index(int value1, int value2, int value3, int value4);

    /** Add multiple indices, IntMeshPartBuilder expects all meshes to be indexed. */
    void index(int value1, int value2, int value3, int value4, int value5, int value6);

    /** Add multiple indices, IntMeshPartBuilder expects all meshes to be indexed. */
    void index(int value1, int value2, int value3, int value4, int value5, int value6, int value7, int value8);

    /** Add a line by indices. Requires GL_LINES primitive type. */
    void line(int index1, int index2);

    /** Add a line. Requires GL_LINES primitive type. */
    void line(VertexInfo p1, VertexInfo p2);

    /** Add a line. Requires GL_LINES primitive type. */
    void line(Vector3 p1, Vector3 p2);

    /** Add a line. Requires GL_LINES primitive type. */
    void line(float x1, float y1, float z1, float x2, float y2, float z2);

    /** Add a line. Requires GL_LINES primitive type. */
    void line(Vector3 p1, Color c1, Vector3 p2, Color c2);

    /** Add a triangle by indices. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
    void triangle(int index1, int index2, int index3);

    /** Add a triangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
    void triangle(VertexInfo p1, VertexInfo p2, VertexInfo p3);

    /** Add a triangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
    void triangle(Vector3 p1, Vector3 p2, Vector3 p3);

    /** Add a triangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
    void triangle(Vector3 p1, Color c1, Vector3 p2, Color c2, Vector3 p3, Color c3);

    /** Add a rectangle by indices. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
    void rect(int corner00, int corner10, int corner11, int corner01);

    /** Add a rectangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
    void rect(VertexInfo corner00, VertexInfo corner10, VertexInfo corner11, VertexInfo corner01);

    /** Add a rectangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
    void rect(Vector3 corner00, Vector3 corner10, Vector3 corner11, Vector3 corner01, Vector3 normal);

    /** Add a rectangle Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
    void rect(float x00, float y00, float z00, float x10, float y10, float z10, float x11, float y11, float z11, float x01, float y01, float z01, float normalX, float normalY, float normalZ);

    /** Add a rectangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
    void patch(VertexInfo corner00, VertexInfo corner10, VertexInfo corner11, VertexInfo corner01, int divisionsU, int divisionsV);

    /** Add a rectangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
    void patch(Vector3 corner00, Vector3 corner10, Vector3 corner11, Vector3 corner01, Vector3 normal, int divisionsU, int divisionsV);

    /** Add a rectangle. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
    void patch(float x00, float y00, float z00, float x10, float y10, float z10, float x11, float y11, float z11, float x01, float y01, float z01, float normalX, float normalY, float normalZ, int divisionsU, int divisionsV);

    /** Add a box. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
    void box(VertexInfo corner000, VertexInfo corner010, VertexInfo corner100, VertexInfo corner110, VertexInfo corner001, VertexInfo corner011, VertexInfo corner101, VertexInfo corner111);

    /** Add a box. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
    void box(Vector3 corner000, Vector3 corner010, Vector3 corner100, Vector3 corner110, Vector3 corner001, Vector3 corner011, Vector3 corner101, Vector3 corner111);

    /** Add a box given the matrix. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
    void box(Matrix4 transform);

    /** Add a box with the specified dimensions. Requires GL_POINTS, GL_LINES or GL_TRIANGLES primitive type. */
    void box(float width, float height, float depth);

    /** Add a box at the specified location, with the specified dimensions */
    void box(float x, float y, float z, float width, float height, float depth);

    /** Add a circle */
    void circle(float radius, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ);

    /** Add a circle */
    void circle(float radius, int divisions, final Vector3 center, final Vector3 normal);

    /** Add a circle */
    void circle(float radius, int divisions, final Vector3 center, final Vector3 normal, final Vector3 tangent, final Vector3 binormal);

    /** Add a circle */
    void circle(float radius, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float tangentX, float tangentY, float tangentZ, float binormalX, float binormalY, float binormalZ);

    /** Add a circle */
    void circle(float radius, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float angleFrom, float angleTo);

    /** Add a circle */
    void circle(float radius, int divisions, final Vector3 center, final Vector3 normal, float angleFrom, float angleTo);

    /** Add a circle */
    void circle(float radius, int divisions, final Vector3 center, final Vector3 normal, final Vector3 tangent, final Vector3 binormal, float angleFrom, float angleTo);

    /** Add a circle */
    void circle(float radius, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float tangentX, float tangentY, float tangentZ, float binormalX, float binormalY, float binormalZ, float angleFrom, float angleTo);

    /** Add a circle */
    void ellipse(float width, float height, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ);

    /** Add a circle */
    void ellipse(float width, float height, int divisions, final Vector3 center, final Vector3 normal);

    /** Add a circle */
    void ellipse(float width, float height, int divisions, final Vector3 center, final Vector3 normal, final Vector3 tangent, final Vector3 binormal);

    /** Add a circle */
    void ellipse(float width, float height, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float tangentX, float tangentY, float tangentZ, float binormalX, float binormalY, float binormalZ);

    /** Add a circle */
    void ellipse(float width, float height, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float angleFrom, float angleTo);

    /** Add a circle */
    void ellipse(float width, float height, int divisions, final Vector3 center, final Vector3 normal, float angleFrom, float angleTo);

    /** Add a circle */
    void ellipse(float width, float height, int divisions, final Vector3 center, final Vector3 normal, final Vector3 tangent, final Vector3 binormal, float angleFrom, float angleTo);

    /** Add a circle */
    void ellipse(float width, float height, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float tangentX, float tangentY, float tangentZ, float binormalX, float binormalY, float binormalZ, float angleFrom, float angleTo);

    /** Add an ellipse */
    void ellipse(float width, float height, float innerWidth, float innerHeight, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float tangentX, float tangentY, float tangentZ, float binormalX, float binormalY, float binormalZ, float angleFrom, float angleTo);

    /** Add an ellipse */
    void ellipse(float width, float height, float innerWidth, float innerHeight, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ, float angleFrom, float angleTo);

    /** Add an ellipse */
    void ellipse(float width, float height, float innerWidth, float innerHeight, int divisions, float centerX, float centerY, float centerZ, float normalX, float normalY, float normalZ);

    /** Add an ellipse */
    void ellipse(float width, float height, float innerWidth, float innerHeight, int divisions, Vector3 center, Vector3 normal);

    /** Add a cylinder */
    void cylinder(float width, float height, float depth, int divisions);

    /** Add a cylinder */
    void cylinder(float width, float height, float depth, int divisions, float angleFrom, float angleTo);

    /** Add a cylinder */
    void cylinder(float width, float height, float depth, int divisions, float angleFrom, float angleTo, boolean close);

    /** Add a cone */
    void cone(float width, float height, float depth, int divisions);

    /** Add a cone */
    void cone(float width, float height, float depth, int divisions, float angleFrom, float angleTo);

    /** Add a cone */
    void cone(float width, float height, float depth, int divisions, int hdivisions, float angleFrom, float angleTo);

    /** Add a sphere */
    void sphere(float width, float height, float depth, int divisionsU, int divisionsV);

    /** Add a sphere */
    void sphere(final Matrix4 transform, float width, float height, float depth, int divisionsU, int divisionsV);

    /** Add a sphere */
    void sphere(float width, float height, float depth, int divisionsU, int divisionsV, float angleUFrom, float angleUTo, float angleVFrom, float angleVTo);

    /** Add a sphere */
    void sphere(final Matrix4 transform, float width, float height, float depth, int divisionsU, int divisionsV, float angleUFrom, float angleUTo, float angleVFrom, float angleVTo);

    /** Add an icosphere **/
    void icosphere(float radius, int divisions, boolean flipNormals, boolean smoothLighting);

    /** Add an icosphere **/
    void icosphere(float radius, int divisions, boolean flipNormals, boolean smoothLighting, int startFace, int nfaces);

    /** Add an octahedronsphere **/
    void octahedronsphere(float radius, int divisions, boolean flipNormals, boolean smoothLighting);

    /** Add an octahedronsphere **/
    void octahedronsphere(float radius, int divisions, boolean flipNormals, boolean hardEdges, int startFace, int nfaces);

    /** Add a capsule */
    void capsule(float radius, float height, int divisions);

    /** Add an arrow 
     * @param x1 source x
     * @param y1 source y
     * @param z1 source z
     * @param x2 destination x
     * @param y2 destination y
     * @param z2 destination z
     * @param capLength is the height of the cap in percentage, must be in (0,1) 
     * @param stemThickness is the percentage of stem diameter compared to cap diameter, must be in (0,1]
     * @param divisions the amount of vertices used to generate the cap and stem ellipsoidal bases */
    void arrow(float x1, float y1, float z1, float x2, float y2, float z2, float capLength, float stemThickness, int divisions);

    /** Get the current vertex transformation matrix. */
    Matrix4 getVertexTransform(Matrix4 out);

    /** Set the current vertex transformation matrix and enables vertex transformation. */
    void setVertexTransform(Matrix4 transform);

    /** Indicates whether vertex transformation is enabled. */
    boolean isVertexTransformationEnabled();

    /** Sets whether vertex transformation is enabled. */
    void setVertexTransformationEnabled(boolean enabled);

    /** Class that contains all vertex information the builder can use.
     * @author Xoppa */
    class VertexInfo implements Poolable {
        public final Vector3 position = new Vector3();
        public boolean hasPosition;
        public final Vector3 normal = new Vector3(0, 1, 0);
        public boolean hasNormal;
        public final Color color = new Color(1, 1, 1, 1);
        public boolean hasColor;
        public final Vector2 uv = new Vector2();
        public boolean hasUV;

        @Override
        public void reset() {
            position.set(0, 0, 0);
            normal.set(0, 1, 0);
            color.set(1, 1, 1, 1);
            uv.set(0, 0);
        }

        public VertexInfo set(Vector3 pos, Vector3 nor, Color col, Vector2 uv) {
            reset();
            if ((hasPosition = pos != null) == true)
                position.set(pos);
            if ((hasNormal = nor != null) == true)
                normal.set(nor);
            if ((hasColor = col != null) == true)
                color.set(col);
            if ((hasUV = uv != null) == true)
                this.uv.set(uv);
            return this;
        }

        public VertexInfo set(final VertexInfo other) {
            if (other == null)
                return set(null, null, null, null);
            hasPosition = other.hasPosition;
            position.set(other.position);
            hasNormal = other.hasNormal;
            normal.set(other.normal);
            hasColor = other.hasColor;
            color.set(other.color);
            hasUV = other.hasUV;
            uv.set(other.uv);
            return this;
        }

        public VertexInfo setPos(float x, float y, float z) {
            position.set(x, y, z);
            hasPosition = true;
            return this;
        }

        public VertexInfo setPos(Vector3 pos) {
            if ((hasPosition = pos != null) == true)
                position.set(pos);
            return this;
        }

        public VertexInfo setNor(float x, float y, float z) {
            normal.set(x, y, z);
            hasNormal = true;
            return this;
        }

        public VertexInfo setNor(Vector3 nor) {
            if ((hasNormal = nor != null) == true)
                normal.set(nor);
            return this;
        }

        public VertexInfo setCol(float r, float g, float b, float a) {
            color.set(r, g, b, a);
            hasColor = true;
            return this;
        }

        public VertexInfo setCol(Color col) {
            if ((hasColor = col != null) == true)
                color.set(col);
            return this;
        }

        public VertexInfo setUV(float u, float v) {
            uv.set(u, v);
            hasUV = true;
            return this;
        }

        public VertexInfo setUV(Vector2 uv) {
            if ((hasUV = uv != null) == true)
                this.uv.set(uv);
            return this;
        }

        public VertexInfo lerp(final VertexInfo target, float alpha) {
            if (hasPosition && target.hasPosition)
                position.lerp(target.position, alpha);
            if (hasNormal && target.hasNormal)
                normal.lerp(target.normal, alpha);
            if (hasColor && target.hasColor)
                color.lerp(target.color, alpha);
            if (hasUV && target.hasUV)
                uv.lerp(target.uv, alpha);
            return this;
        }
    }

    /** Add a cylinder */
    void cylinder(float width, float height, float depth, int divisions, float angleFrom, float angleTo, boolean close, boolean flipNormals);

    /** Add a sphere */
    void sphere(float width, float height, float depth, int divisionsU, int divisionsV, boolean flipNormals, float angleUFrom, float angleUTo, float angleVFrom, float angleVTo);

    /** Add a sphere **/
    void sphere(final Matrix4 transform, float width, float height, float depth, int divisionsU, int divisionsV, boolean flipNormals, float angleUFrom, float angleUTo, float angleVFrom, float angleVTo);

    /** Add a ring **/
    void ring(float innerRadius, float outerRadius, int divisions, boolean flipNormals);

    /** Add a ring **/
    void ring(Matrix4 transform, float innerRadius, float outerRadius, int divisions, boolean flipNormals);

    /** Add a ring **/
    void ring(Matrix4 transform, float innerRadius, float outerRadius, int divisions, boolean flipNormals, float angleStart, float angleEnd);
}
