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

import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.graphics.g3d.model.data.*;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider;
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider.FileTextureProvider;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.*;
import gaia.cu9.ari.gaiaorbit.util.gdx.loader.IntModelLoader;
import gaia.cu9.ari.gaiaorbit.util.gdx.mesh.IntMesh;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.data.IntModelData;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.data.IntModelMesh;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.data.IntModelMeshPart;
import gaia.cu9.ari.gaiaorbit.util.gdx.model.data.IntModelNode;

/**
 * This implementation uses {@link IntMesh} and {@link IntMeshPart} instead of {@link Mesh} and {@link MeshPart}.
 *
 * A model represents a 3D assets. It stores a hierarchy of nodes. A node has a transform and optionally a graphical part in form
 * of a {@link IntMeshPart} and {@link Material}. Mesh parts reference subsets of vertices in one of the meshes of the model.
 * Animations can be applied to nodes, to modify their transform (translation, rotation, scale) over time.</p>
 * 
 * A model can be rendered by creating a {@link IntModelInstance} from it. That instance has an additional transform to position the
 * model in the world, and allows modification of materials and nodes without destroying the original model. The original model is
 * the owner of any meshes and textures, all instances created from the model share these resources. Disposing the model will
 * automatically make all instances invalid!</p>
 * 
 * A model is created from {@link IntModelData}, which in turn is loaded by a {@link IntModelLoader}.
 * 
 * @author badlogic, xoppa */
public class IntModel implements Disposable {
	/** the materials of the model, used by nodes that have a graphical representation **/
	public final Array<Material> materials = new Array();
	/** root nodes of the model **/
	public final Array<IntNode> nodes = new Array();
	/** animations of the model, modifying node transformations **/
	public final Array<IntAnimation> animations = new Array();
	/** the meshes of the model **/
	public final Array<IntMesh> meshes = new Array();
	/** parts of meshes, used by nodes that have a graphical representation **/
	public final Array<IntMeshPart> meshParts = new Array();
	/** Array of disposable resources like textures or meshes the Model is responsible for disposing **/
	protected final Array<Disposable> disposables = new Array();

	/** Constructs an empty model. Manual created models do not manage their resources by default. Use
	 * {@link #manageDisposable(Disposable)} to add resources to be managed by this model. */
	public IntModel() {
	}

	/** Constructs a new Model based on the {@link IntModelData}. Texture files will be loaded from the internal file storage via an
	 * {@link FileTextureProvider}.
	 * @param modelData the {@link IntModelData} got from e.g. {@link ModelLoader} */
	public IntModel(IntModelData modelData) {
		this(modelData, new FileTextureProvider());
	}

	/** Constructs a new Model based on the {@link IntModelData}.
	 * @param modelData the {@link IntModelData} got from e.g. {@link ModelLoader}
	 * @param textureProvider the {@link TextureProvider} to use for loading the textures */
	public IntModel(IntModelData modelData, TextureProvider textureProvider) {
		load(modelData, textureProvider);
	}

	protected void load (IntModelData modelData, TextureProvider textureProvider) {
		loadMeshes(modelData.meshes);
		loadMaterials(modelData.materials, textureProvider);
		loadNodes(modelData.nodes);
		loadAnimations(modelData.animations);
		calculateTransforms();
	}

	protected void loadAnimations (Iterable<ModelAnimation> modelAnimations) {
		for (final ModelAnimation anim : modelAnimations) {
			IntAnimation animation = new IntAnimation();
			animation.id = anim.id;
			for (ModelNodeAnimation nanim : anim.nodeAnimations) {
				final IntNode node = getNode(nanim.nodeId);
				if (node == null) continue;
				IntNodeAnimation nodeAnim = new IntNodeAnimation();
				nodeAnim.node = node;

				if (nanim.translation != null) {
					nodeAnim.translation = new Array<>();
					nodeAnim.translation.ensureCapacity(nanim.translation.size);
					for (ModelNodeKeyframe<Vector3> kf : nanim.translation) {
						if (kf.keytime > animation.duration) animation.duration = kf.keytime;
						nodeAnim.translation.add(new NodeKeyframe<>(kf.keytime, new Vector3(kf.value == null ? node.translation : kf.value)));
					}
				}

				if (nanim.rotation != null) {
					nodeAnim.rotation = new Array<>();
					nodeAnim.rotation.ensureCapacity(nanim.rotation.size);
					for (ModelNodeKeyframe<Quaternion> kf : nanim.rotation) {
						if (kf.keytime > animation.duration) animation.duration = kf.keytime;
						nodeAnim.rotation.add(new NodeKeyframe<>(kf.keytime, new Quaternion(kf.value == null ? node.rotation : kf.value)));
					}
				}

				if (nanim.scaling != null) {
					nodeAnim.scaling = new Array<>();
					nodeAnim.scaling.ensureCapacity(nanim.scaling.size);
					for (ModelNodeKeyframe<Vector3> kf : nanim.scaling) {
						if (kf.keytime > animation.duration) animation.duration = kf.keytime;
						nodeAnim.scaling.add(new NodeKeyframe<>(kf.keytime, new Vector3(kf.value == null ? node.scale : kf.value)));
					}
				}

				if ((nodeAnim.translation != null && nodeAnim.translation.size > 0)
					|| (nodeAnim.rotation != null && nodeAnim.rotation.size > 0)
					|| (nodeAnim.scaling != null && nodeAnim.scaling.size > 0)) animation.nodeAnimations.add(nodeAnim);
			}
			if (animation.nodeAnimations.size > 0) animations.add(animation);
		}
	}

	private ObjectMap<IntNodePart, ArrayMap<String, Matrix4>> nodePartBones = new ObjectMap<>();

	protected void loadNodes (Iterable<IntModelNode> modelNodes) {
		nodePartBones.clear();
		for (IntModelNode node : modelNodes) {
			nodes.add(loadNode(node));
		}
		for (ObjectMap.Entry<IntNodePart, ArrayMap<String, Matrix4>> e : nodePartBones.entries()) {
			if (e.key.invBoneBindTransforms == null)
				e.key.invBoneBindTransforms = new ArrayMap<>(Node.class, Matrix4.class);
			e.key.invBoneBindTransforms.clear();
			for (ObjectMap.Entry<String, Matrix4> b : e.value.entries())
				e.key.invBoneBindTransforms.put(getNode(b.key), new Matrix4(b.value).inv());
		}
	}

	protected IntNode loadNode (IntModelNode modelNode) {
		IntNode node = new IntNode();
		node.id = modelNode.id;

		if (modelNode.translation != null) node.translation.set(modelNode.translation);
		if (modelNode.rotation != null) node.rotation.set(modelNode.rotation);
		if (modelNode.scale != null) node.scale.set(modelNode.scale);
		// FIXME create temporary maps for faster lookup?
		if (modelNode.parts != null) {
			for (ModelNodePart modelNodePart : modelNode.parts) {
				IntMeshPart meshPart = null;
				Material meshMaterial = null;

				if (modelNodePart.meshPartId != null) {
					for (IntMeshPart part : meshParts) {
						if (modelNodePart.meshPartId.equals(part.id)) {
							meshPart = part;
							break;
						}
					}
				}

				if (modelNodePart.materialId != null) {
					for (Material material : materials) {
						if (modelNodePart.materialId.equals(material.id)) {
							meshMaterial = material;
							break;
						}
					}
				}

				if (meshPart == null || meshMaterial == null) throw new GdxRuntimeException("Invalid node: " + node.id);

				if (meshPart != null && meshMaterial != null) {
					IntNodePart nodePart = new IntNodePart();
					nodePart.meshPart = meshPart;
					nodePart.material = meshMaterial;
					node.parts.add(nodePart);
					if (modelNodePart.bones != null) nodePartBones.put(nodePart, modelNodePart.bones);
				}
			}
		}

		if (modelNode.children != null) {
			for (IntModelNode child : modelNode.children) {
				node.addChild(loadNode(child));
			}
		}

		return node;
	}

	protected void loadMeshes (Iterable<IntModelMesh> meshes) {
		for (IntModelMesh mesh : meshes) {
			convertMesh(mesh);
		}
	}

	protected void convertMesh (IntModelMesh modelMesh) {
		int numIndices = 0;
		for (IntModelMeshPart part : modelMesh.parts) {
			numIndices += part.indices.length;
		}
		VertexAttributes attributes = new VertexAttributes(modelMesh.attributes);
		int numVertices = modelMesh.vertices.length / (attributes.vertexSize / 4);

		IntMesh mesh = new IntMesh(true, numVertices, numIndices, attributes);
		meshes.add(mesh);
		disposables.add(mesh);

		BufferUtils.copy(modelMesh.vertices, mesh.getVerticesBuffer(), modelMesh.vertices.length, 0);
		int offset = 0;
		mesh.getIndicesBuffer().clear();
		for (IntModelMeshPart part : modelMesh.parts) {
			IntMeshPart meshPart = new IntMeshPart();
			meshPart.id = part.id;
			meshPart.primitiveType = part.primitiveType;
			meshPart.offset = offset;
			meshPart.size = part.indices.length;
			meshPart.mesh = mesh;
			mesh.getIndicesBuffer().put(part.indices);
			offset += meshPart.size;
			meshParts.add(meshPart);
		}
		mesh.getIndicesBuffer().position(0);
		for (IntMeshPart part : meshParts)
			part.update();
	}

	protected void loadMaterials (Iterable<ModelMaterial> modelMaterials, TextureProvider textureProvider) {
		for (ModelMaterial mtl : modelMaterials) {
			this.materials.add(convertMaterial(mtl, textureProvider));
		}
	}

	protected Material convertMaterial (ModelMaterial mtl, TextureProvider textureProvider) {
		Material result = new Material();
		result.id = mtl.id;
		if (mtl.ambient != null) result.set(new ColorAttribute(ColorAttribute.Ambient, mtl.ambient));
		if (mtl.diffuse != null) result.set(new ColorAttribute(ColorAttribute.Diffuse, mtl.diffuse));
		if (mtl.specular != null) result.set(new ColorAttribute(ColorAttribute.Specular, mtl.specular));
		if (mtl.emissive != null) result.set(new ColorAttribute(ColorAttribute.Emissive, mtl.emissive));
		if (mtl.reflection != null) result.set(new ColorAttribute(ColorAttribute.Reflection, mtl.reflection));
		if (mtl.shininess > 0f) result.set(new FloatAttribute(FloatAttribute.Shininess, mtl.shininess));
		if (mtl.opacity != 1.f) result.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, mtl.opacity));

		ObjectMap<String, Texture> textures = new ObjectMap<String, Texture>();

		// FIXME uvScaling/uvTranslation totally ignored
		if (mtl.textures != null) {
			for (ModelTexture tex : mtl.textures) {
				Texture texture;
				if (textures.containsKey(tex.fileName)) {
					texture = textures.get(tex.fileName);
				} else {
					texture = textureProvider.load(tex.fileName);
					textures.put(tex.fileName, texture);
					disposables.add(texture);
				}

				TextureDescriptor descriptor = new TextureDescriptor(texture);
				descriptor.minFilter = texture.getMinFilter();
				descriptor.magFilter = texture.getMagFilter();
				descriptor.uWrap = texture.getUWrap();
				descriptor.vWrap = texture.getVWrap();

				float offsetU = tex.uvTranslation == null ? 0f : tex.uvTranslation.x;
				float offsetV = tex.uvTranslation == null ? 0f : tex.uvTranslation.y;
				float scaleU = tex.uvScaling == null ? 1f : tex.uvScaling.x;
				float scaleV = tex.uvScaling == null ? 1f : tex.uvScaling.y;

				switch (tex.usage) {
				case ModelTexture.USAGE_DIFFUSE:
					result.set(new TextureAttribute(TextureAttribute.Diffuse, descriptor, offsetU, offsetV, scaleU, scaleV));
					break;
				case ModelTexture.USAGE_SPECULAR:
					result.set(new TextureAttribute(TextureAttribute.Specular, descriptor, offsetU, offsetV, scaleU, scaleV));
					break;
				case ModelTexture.USAGE_BUMP:
					result.set(new TextureAttribute(TextureAttribute.Bump, descriptor, offsetU, offsetV, scaleU, scaleV));
					break;
				case ModelTexture.USAGE_NORMAL:
					result.set(new TextureAttribute(TextureAttribute.Normal, descriptor, offsetU, offsetV, scaleU, scaleV));
					break;
				case ModelTexture.USAGE_AMBIENT:
					result.set(new TextureAttribute(TextureAttribute.Ambient, descriptor, offsetU, offsetV, scaleU, scaleV));
					break;
				case ModelTexture.USAGE_EMISSIVE:
					result.set(new TextureAttribute(TextureAttribute.Emissive, descriptor, offsetU, offsetV, scaleU, scaleV));
					break;
				case ModelTexture.USAGE_REFLECTION:
					result.set(new TextureAttribute(TextureAttribute.Reflection, descriptor, offsetU, offsetV, scaleU, scaleV));
					break;
				}
			}
		}

		return result;
	}

	/** Adds a {@link Disposable} to be managed and disposed by this Model. Can be used to keep track of manually loaded textures
	 * for {@link IntModelInstance}.
	 * @param disposable the Disposable */
	public void manageDisposable (Disposable disposable) {
		if (!disposables.contains(disposable, true)) disposables.add(disposable);
	}

	/** @return the {@link Disposable} objects that will be disposed when the {@link #dispose()} method is called. */
	public Iterable<Disposable> getManagedDisposables () {
		return disposables;
	}

	@Override
	public void dispose () {
		for (Disposable disposable : disposables) {
			disposable.dispose();
		}
	}

	/** Calculates the local and world transform of all {@link Node} instances in this model, recursively. First each
	 * {@link Node#localTransform} transform is calculated based on the translation, rotation and scale of each Node. Then each
	 * {@link Node#calculateWorldTransform()} is calculated, based on the parent's world transform and the local transform of each
	 * Node. Finally, the animation bone matrices are updated accordingly.</p>
	 * 
	 * This method can be used to recalculate all transforms if any of the Node's local properties (translation, rotation, scale)
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
	 * @return The {@link Animation} with the specified id, or null if not available. */
	public IntAnimation getAnimation (final String id) {
		return getAnimation(id, true);
	}

	/** @param id The ID of the animation to fetch.
	 * @param ignoreCase whether to use case sensitivity when comparing the animation id.
	 * @return The {@link Animation} with the specified id, or null if not available. */
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
