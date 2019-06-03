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

package gaia.cu9.ari.gaiaorbit.util.gdx.model;

import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Pool;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntModelBatch;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntRenderable;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntRenderableProvider;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.IntShader;

/** An instance of a {@link IntModel}, allows to specify global transform and modify the materials, as it has a copy of the model's
 * materials. Multiple instances can be created from the same IntModel, all sharing the meshes and textures of the IntModel. The IntModel
 * owns the meshes and textures, to dispose of these, the IntModel has to be disposed. Therefor, the IntModel must outlive all its
 * IntModelInstances</p>
 * 
 * The IntModelInstance creates a full copy of all materials, nodes and animations.
 * @author badlogic, xoppa */
public class IntModelInstance implements IntRenderableProvider {
	/** Whether, by default, {@link NodeKeyframe}'s are shared amongst {@link IntModel} and IntModelInstance. Can be overridden per
	 * IntModelInstance using the constructor argument. */
	public static boolean defaultShareKeyframes = true;

	/** the materials of the model, used by nodes that have a graphical representation FIXME not sure if superfluous, allows
	 * modification of materials without having to traverse the nodes **/
	public final Array<Material> materials = new Array();
	/** root nodes of the model **/
	public final Array<IntNode> nodes = new Array();
	/** animations of the model, modifying node transformations **/
	public final Array<IntAnimation> animations = new Array();
	/** the {@link IntModel} this instances derives from **/
	public final IntModel model;
	/** the world transform **/
	public Matrix4 transform;
	/** user definable value, which is passed to the {@link IntShader}. */
	public Object userData;

	/** Constructs a new IntModelInstance with all nodes and materials of the given model.
	 * @param model The {@link IntModel} to create an instance of. */
	public IntModelInstance(final IntModel model) {
		this(model, (String[])null);
	}

	/** @param model The source {@link IntModel}
	 * @param nodeId The ID of the root {@link IntNode} of the {@link IntModel} for the instance to contain
	 * @param mergeTransform True to apply the source node transform to the instance transform, resetting the node transform. */
	public IntModelInstance(final IntModel model, final String nodeId, boolean mergeTransform) {
		this(model, null, nodeId, false, false, mergeTransform);
	}

	/** @param model The source {@link IntModel}
	 * @param transform The {@link Matrix4} instance for this IntModelInstance to reference or null to create a new matrix.
	 * @param nodeId The ID of the root {@link IntNode} of the {@link IntModel} for the instance to contain
	 * @param mergeTransform True to apply the source node transform to the instance transform, resetting the node transform. */
	public IntModelInstance(final IntModel model, final Matrix4 transform, final String nodeId, boolean mergeTransform) {
		this(model, transform, nodeId, false, false, mergeTransform);
	}

	/** Recursively searches the mode for the specified node.
	 * @param model The source {@link IntModel}
	 * @param nodeId The ID of the {@link IntNode} within the {@link IntModel} for the instance to contain
	 * @param parentTransform True to apply the parent's node transform to the instance (only applicable if recursive is true).
	 * @param mergeTransform True to apply the source node transform to the instance transform, resetting the node transform. */
	public IntModelInstance(final IntModel model, final String nodeId, boolean parentTransform, boolean mergeTransform) {
		this(model, null, nodeId, true, parentTransform, mergeTransform);
	}

	/** Recursively searches the mode for the specified node.
	 * @param model The source {@link IntModel}
	 * @param transform The {@link Matrix4} instance for this IntModelInstance to reference or null to create a new matrix.
	 * @param nodeId The ID of the {@link IntNode} within the {@link IntModel} for the instance to contain
	 * @param parentTransform True to apply the parent's node transform to the instance (only applicable if recursive is true).
	 * @param mergeTransform True to apply the source node transform to the instance transform, resetting the node transform. */
	public IntModelInstance(final IntModel model, final Matrix4 transform, final String nodeId, boolean parentTransform,
		boolean mergeTransform) {
		this(model, transform, nodeId, true, parentTransform, mergeTransform);
	}

	/** @param model The source {@link IntModel}
	 * @param nodeId The ID of the {@link IntNode} within the {@link IntModel} for the instance to contain
	 * @param recursive True to recursively search the IntModel's node tree, false to only search for a root node
	 * @param parentTransform True to apply the parent's node transform to the instance (only applicable if recursive is true).
	 * @param mergeTransform True to apply the source node transform to the instance transform, resetting the node transform. */
	public IntModelInstance(final IntModel model, final String nodeId, boolean recursive, boolean parentTransform,
		boolean mergeTransform) {
		this(model, null, nodeId, recursive, parentTransform, mergeTransform);
	}

	/** @param model The source {@link IntModel}
	 * @param transform The {@link Matrix4} instance for this IntModelInstance to reference or null to create a new matrix.
	 * @param nodeId The ID of the {@link IntNode} within the {@link IntModel} for the instance to contain
	 * @param recursive True to recursively search the IntModel's node tree, false to only search for a root node
	 * @param parentTransform True to apply the parent's node transform to the instance (only applicable if recursive is true).
	 * @param mergeTransform True to apply the source node transform to the instance transform, resetting the node transform. */
	public IntModelInstance(final IntModel model, final Matrix4 transform, final String nodeId, boolean recursive,
		boolean parentTransform, boolean mergeTransform) {
		this(model, transform, nodeId, recursive, parentTransform, mergeTransform, defaultShareKeyframes);
	}

	/** @param model The source {@link IntModel}
	 * @param transform The {@link Matrix4} instance for this IntModelInstance to reference or null to create a new matrix.
	 * @param nodeId The ID of the {@link IntNode} within the {@link IntModel} for the instance to contain
	 * @param recursive True to recursively search the IntModel's node tree, false to only search for a root node
	 * @param parentTransform True to apply the parent's node transform to the instance (only applicable if recursive is true).
	 * @param mergeTransform True to apply the source node transform to the instance transform, resetting the node transform. */
	public IntModelInstance(final IntModel model, final Matrix4 transform, final String nodeId, boolean recursive,
		boolean parentTransform, boolean mergeTransform, boolean shareKeyframes) {
		this.model = model;
		this.transform = transform == null ? new Matrix4() : transform;
		IntNode copy, node = model.getNode(nodeId, recursive);
		this.nodes.add(copy = node.copy());
		if (mergeTransform) {
			this.transform.mul(parentTransform ? node.globalTransform : node.localTransform);
			copy.translation.set(0, 0, 0);
			copy.rotation.idt();
			copy.scale.set(1, 1, 1);
		} else if (parentTransform && copy.hasParent()) this.transform.mul(node.getParent().globalTransform);
		invalidate();
		copyAnimations(model.animations, shareKeyframes);
		calculateTransforms();
	}

	/** Constructs a new IntModelInstance with only the specified nodes and materials of the given model. */
	public IntModelInstance(final IntModel model, final String... rootNodeIds) {
		this(model, null, rootNodeIds);
	}

	/** Constructs a new IntModelInstance with only the specified nodes and materials of the given model. */
	public IntModelInstance(final IntModel model, final Matrix4 transform, final String... rootNodeIds) {
		this.model = model;
		this.transform = transform == null ? new Matrix4() : transform;
		if (rootNodeIds == null)
			copyNodes(model.nodes);
		else
			copyNodes(model.nodes, rootNodeIds);
		copyAnimations(model.animations, defaultShareKeyframes);
		calculateTransforms();
	}

	/** Constructs a new IntModelInstance with only the specified nodes and materials of the given model. */
	public IntModelInstance(final IntModel model, final Array<String> rootNodeIds) {
		this(model, null, rootNodeIds);
	}

	/** Constructs a new IntModelInstance with only the specified nodes and materials of the given model. */
	public IntModelInstance(final IntModel model, final Matrix4 transform, final Array<String> rootNodeIds) {
		this(model, transform, rootNodeIds, defaultShareKeyframes);
	}

	/** Constructs a new IntModelInstance with only the specified nodes and materials of the given model. */
	public IntModelInstance(final IntModel model, final Matrix4 transform, final Array<String> rootNodeIds, boolean shareKeyframes) {
		this.model = model;
		this.transform = transform == null ? new Matrix4() : transform;
		copyNodes(model.nodes, rootNodeIds);
		copyAnimations(model.animations, shareKeyframes);
		calculateTransforms();
	}

	/** Constructs a new IntModelInstance at the specified position. */
	public IntModelInstance(final IntModel model, Vector3 position) {
		this(model);
		this.transform.setToTranslation(position);
	}

	/** Constructs a new IntModelInstance at the specified position. */
	public IntModelInstance(final IntModel model, float x, float y, float z) {
		this(model);
		this.transform.setToTranslation(x, y, z);
	}

	/** Constructs a new IntModelInstance with the specified transform. */
	public IntModelInstance(final IntModel model, Matrix4 transform) {
		this(model, transform, (String[])null);
	}

	/** Constructs a new IntModelInstance which is an copy of the specified IntModelInstance. */
	public IntModelInstance(IntModelInstance copyFrom) {
		this(copyFrom, copyFrom.transform.cpy());
	}

	/** Constructs a new IntModelInstance which is an copy of the specified IntModelInstance. */
	public IntModelInstance(IntModelInstance copyFrom, final Matrix4 transform) {
		this(copyFrom, transform, defaultShareKeyframes);
	}

	/** Constructs a new IntModelInstance which is an copy of the specified IntModelInstance. */
	public IntModelInstance(IntModelInstance copyFrom, final Matrix4 transform, boolean shareKeyframes) {
		this.model = copyFrom.model;
		this.transform = transform == null ? new Matrix4() : transform;
		copyNodes(copyFrom.nodes);
		copyAnimations(copyFrom.animations, shareKeyframes);
		calculateTransforms();
	}

	/** @return A newly created IntModelInstance which is a copy of this IntModelInstance */
	public IntModelInstance copy () {
		return new IntModelInstance(this);
	}

	private void copyNodes (Array<IntNode> nodes) {
		for (int i = 0, n = nodes.size; i < n; ++i) {
			final IntNode node = nodes.get(i);
			this.nodes.add(node.copy());
		}
		invalidate();
	}

	private void copyNodes (Array<IntNode> nodes, final String... nodeIds) {
		for (int i = 0, n = nodes.size; i < n; ++i) {
			final IntNode node = nodes.get(i);
			for (final String nodeId : nodeIds) {
				if (nodeId.equals(node.id)) {
					this.nodes.add(node.copy());
					break;
				}
			}
		}
		invalidate();
	}

	private void copyNodes (Array<IntNode> nodes, final Array<String> nodeIds) {
		for (int i = 0, n = nodes.size; i < n; ++i) {
			final IntNode node = nodes.get(i);
			for (final String nodeId : nodeIds) {
				if (nodeId.equals(node.id)) {
					this.nodes.add(node.copy());
					break;
				}
			}
		}
		invalidate();
	}

	/** Makes sure that each {@link IntNodePart} of the {@link IntNode} and its sub-nodes, doesn't reference a node outside this node
	 * tree and that all materials are listed in the {@link #materials} array. */
	private void invalidate (IntNode node) {
		for (int i = 0, n = node.parts.size; i < n; ++i) {
			IntNodePart part = node.parts.get(i);
			ArrayMap<IntNode, Matrix4> bindPose = part.invBoneBindTransforms;
			if (bindPose != null) {
				for (int j = 0; j < bindPose.size; ++j) {
					bindPose.keys[j] = getNode(bindPose.keys[j].id);
				}
			}
			if (!materials.contains(part.material, true)) {
				final int midx = materials.indexOf(part.material, false);
				if (midx < 0)
					materials.add(part.material = part.material.copy());
				else
					part.material = materials.get(midx);
			}
		}
		for (int i = 0, n = node.getChildCount(); i < n; ++i) {
			invalidate(node.getChild(i));
		}
	}

	/** Makes sure that each {@link IntNodePart} of each {@link IntNode} doesn't reference a node outside this node tree and that all
	 * materials are listed in the {@link #materials} array. */
	private void invalidate () {
		for (int i = 0, n = nodes.size; i < n; ++i) {
			invalidate(nodes.get(i));
		}
	}

	private void copyAnimations (final Iterable<IntAnimation> source, boolean shareKeyframes) {
		for (final IntAnimation anim : source) {
			IntAnimation animation = new IntAnimation();
			animation.id = anim.id;
			animation.duration = anim.duration;
			for (final IntNodeAnimation nanim : anim.nodeAnimations) {
				final IntNode node = getNode(nanim.node.id);
				if (node == null) continue;
				IntNodeAnimation nodeAnim = new IntNodeAnimation();
				nodeAnim.node = node;
				if (shareKeyframes) {
					nodeAnim.translation = nanim.translation;
					nodeAnim.rotation = nanim.rotation;
					nodeAnim.scaling = nanim.scaling;
				} else {
					if (nanim.translation != null) {
						nodeAnim.translation = new Array<>();
						for (final NodeKeyframe<Vector3> kf : nanim.translation)
							nodeAnim.translation.add(new NodeKeyframe<Vector3>(kf.keytime, kf.value));
					}
					if (nanim.rotation != null) {
						nodeAnim.rotation = new Array<>();
						for (final NodeKeyframe<Quaternion> kf : nanim.rotation)
							nodeAnim.rotation.add(new NodeKeyframe<>(kf.keytime, kf.value));
					}
					if (nanim.scaling != null) {
						nodeAnim.scaling = new Array<>();
						for (final NodeKeyframe<Vector3> kf : nanim.scaling)
							nodeAnim.scaling.add(new NodeKeyframe<>(kf.keytime, kf.value));
					}
				}
				if (nodeAnim.translation != null || nodeAnim.rotation != null || nodeAnim.scaling != null)
					animation.nodeAnimations.add(nodeAnim);
			}
			if (animation.nodeAnimations.size > 0) animations.add(animation);
		}
	}

	/** Traverses the IntNode hierarchy and collects {@link IntRenderable} instances for every node with a graphical representation.
	 * IntRenderables are obtained from the provided pool. The resulting array can be rendered via a {@link IntModelBatch}.
	 * 
	 * @param renderables the output array
	 * @param pool the pool to obtain IntRenderables from */
	public void getRenderables (Array<IntRenderable> renderables, Pool<IntRenderable> pool) {
		for (IntNode node : nodes) {
			getRenderables(node, renderables, pool);
		}
	}

	/** @return The renderable of the first node's first part. */
	public IntRenderable getRenderable (final IntRenderable out) {
		return getRenderable(out, nodes.get(0));
	}

	/** @return The renderable of the node's first part. */
	public IntRenderable getRenderable (final IntRenderable out, final IntNode node) {
		return getRenderable(out, node, node.parts.get(0));
	}

	public IntRenderable getRenderable (final IntRenderable out, final IntNode node, final IntNodePart nodePart) {
		nodePart.setRenderable(out);
		if (nodePart.bones == null && transform != null)
			out.worldTransform.set(transform).mul(node.globalTransform);
		else if (transform != null)
			out.worldTransform.set(transform);
		else
			out.worldTransform.idt();
		out.userData = userData;
		return out;
	}

	protected void getRenderables (IntNode node, Array<IntRenderable> renderables, Pool<IntRenderable> pool) {
		if (node.parts.size > 0) {
			for (IntNodePart nodePart : node.parts) {
				if (nodePart.enabled) renderables.add(getRenderable(pool.obtain(), node, nodePart));
			}
		}

		for (IntNode child : node.getChildren()) {
			getRenderables(child, renderables, pool);
		}
	}

	/** Calculates the local and world transform of all {@link IntNode} instances in this model, recursively. First each
	 * {@link IntNode#localTransform} transform is calculated based on the translation, rotation and scale of each IntNode. Then each
	 * {@link IntNode#calculateWorldTransform()} is calculated, based on the parent's world transform and the local transform of each
	 * IntNode. Finally, the animation bone matrices are updated accordingly.</p>
	 * 
	 * This method can be used to recalculate all transforms if any of the IntNode's local properties (translation, rotation, scale)
	 * was modified. */
	public void calculateTransforms () {
		final int n = nodes.size;
		for (int i = 0; i < n; i++) {
			nodes.get(i).calculateTransforms(true);
		}
		for (int i = 0; i < n; i++) {
			nodes.get(i).calculateBoneTransforms(true);
		}
	}

	/** Calculate the bounding box of this model instance. This is a potential slow operation, it is advised to cache the result.
	 * @param out the {@link BoundingBox} that will be set with the bounds.
	 * @return the out parameter for chaining */
	public BoundingBox calculateBoundingBox (final BoundingBox out) {
		out.inf();
		return extendBoundingBox(out);
	}

	/** Extends the bounding box with the bounds of this model instance. This is a potential slow operation, it is advised to cache
	 * the result.
	 * @param out the {@link BoundingBox} that will be extended with the bounds.
	 * @return the out parameter for chaining */
	public BoundingBox extendBoundingBox (final BoundingBox out) {
		final int n = nodes.size;
		for (int i = 0; i < n; i++)
			nodes.get(i).extendBoundingBox(out);
		return out;
	}

	/** @param id The ID of the animation to fetch (case sensitive).
	 * @return The {@link IntAnimation} with the specified id, or null if not available. */
	public IntAnimation getAnimation (final String id) {
		return getAnimation(id, false);
	}

	/** @param id The ID of the animation to fetch.
	 * @param ignoreCase whether to use case sensitivity when comparing the animation id.
	 * @return The {@link IntAnimation} with the specified id, or null if not available. */
	public IntAnimation getAnimation (final String id, boolean ignoreCase) {
		final int n = animations.size;
		IntAnimation animation;
		if (ignoreCase) {
			for (int i = 0; i < n; i++)
				if ((animation = animations.get(i)).id.equalsIgnoreCase(id)) return animation;
		} else {
			for (int i = 0; i < n; i++)
				if ((animation = animations.get(i)).id.equals(id)) return animation;
		}
		return null;
	}

	/** @param id The ID of the material to fetch.
	 * @return The {@link Material} with the specified id, or null if not available. */
	public Material getMaterial (final String id) {
		return getMaterial(id, true);
	}

	/** @param id The ID of the material to fetch.
	 * @param ignoreCase whether to use case sensitivity when comparing the material id.
	 * @return The {@link Material} with the specified id, or null if not available. */
	public Material getMaterial (final String id, boolean ignoreCase) {
		final int n = materials.size;
		Material material;
		if (ignoreCase) {
			for (int i = 0; i < n; i++)
				if ((material = materials.get(i)).id.equalsIgnoreCase(id)) return material;
		} else {
			for (int i = 0; i < n; i++)
				if ((material = materials.get(i)).id.equals(id)) return material;
		}
		return null;
	}

	/** @param id The ID of the node to fetch.
	 * @return The {@link IntNode} with the specified id, or null if not found. */
	public IntNode getNode (final String id) {
		return getNode(id, true);
	}

	/** @param id The ID of the node to fetch.
	 * @param recursive false to fetch a root node only, true to search the entire node tree for the specified node.
	 * @return The {@link IntNode} with the specified id, or null if not found. */
	public IntNode getNode (final String id, boolean recursive) {
		return getNode(id, recursive, false);
	}

	/** @param id The ID of the node to fetch.
	 * @param recursive false to fetch a root node only, true to search the entire node tree for the specified node.
	 * @param ignoreCase whether to use case sensitivity when comparing the node id.
	 * @return The {@link IntNode} with the specified id, or null if not found. */
	public IntNode getNode (final String id, boolean recursive, boolean ignoreCase) {
		return IntNode.getNode(nodes, id, recursive, ignoreCase);
	}
}
