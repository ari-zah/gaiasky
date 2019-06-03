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

package gaia.cu9.ari.gaiaorbit.util.gdx.model.data;

import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.g3d.model.data.ModelAnimation;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

/** Returned by a {@link ModelLoader}, contains meshes, materials, nodes and animations. OpenGL resources like textures or vertex
 * buffer objects are not stored. Instead, a IntModelData instance needs to be converted to a Model first.
 * @author badlogic */
public class IntModelData {
	public String id;
	public final short version[] = new short[2];
	public final Array<IntModelMesh> meshes = new Array<>();
	public final Array<ModelMaterial> materials = new Array<>();
	public final Array<IntModelNode> nodes = new Array<>();
	public final Array<ModelAnimation> animations = new Array<>();

	public void addMesh (IntModelMesh mesh) {
		for (IntModelMesh other : meshes) {
			if (other.id.equals(mesh.id)) {
				throw new GdxRuntimeException("Mesh with id '" + other.id + "' already in model");
			}
		}
		meshes.add(mesh);
	}
}
