/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.octreegen.generator;

import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.math.BoundingBoxd;
import gaiasky.util.math.Vector3d;
import gaiasky.util.tree.OctreeNode;

import java.util.Iterator;
import java.util.List;

/**
 * Interface for octree generators.
 */
public interface IOctreeGenerator {
    Log logger = Logger.getLogger(IOctreeGenerator.class);

    OctreeNode generateOctree(List<IParticleRecord> catalog);

    int getDiscarded();

    /**
     * Computes the maximum axis-aligned bounding box containing
     * all the particles in the catalog, and returns it as the root
     * octree node.
     * @param catalog The incoming catalog
     * @param params The octree generation parameters
     * @return The root octree node
     */
    static OctreeNode startGeneration(List<IParticleRecord> catalog, OctreeGeneratorParams params) {

        logger.info("Starting generation of octree");

        // Minimum and maximum positions
        Vector3d min = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        Vector3d max = new Vector3d(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE);

        Iterator<IParticleRecord> it = catalog.iterator();
        while (it.hasNext()) {
            IParticleRecord s = it.next();
            // Min
            if (s.x() < min.x) {
                min.x = s.x();
            }
            if (s.y() < min.y) {
                min.y = s.y();
            }
            if (s.z() < min.z) {
                min.z = s.z();
            }

            // Max
            if (s.x() > max.x) {
                max.x = s.x();
            }
            if (s.y() > max.y) {
                max.y = s.y();
            }
            if (s.z() > max.z) {
                max.z = s.z();
            }
        }

        BoundingBoxd box = new BoundingBoxd(min, max);
        double halfSize = Math.max(Math.max(box.getDepth(), box.getHeight()), box.getWidth()) / 2d;
        OctreeNode root = new OctreeNode(box.getCenterX(), box.getCenterY(), box.getCenterZ(), halfSize, halfSize, halfSize, 0);
        return root;
    }
}
