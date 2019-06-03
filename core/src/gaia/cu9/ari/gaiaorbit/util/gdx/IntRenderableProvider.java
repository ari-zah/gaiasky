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

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/** Returns a list of {@link IntRenderable} instances to be rendered by a {@link IntModelBatch}.
 * @author badlogic */
public interface IntRenderableProvider {
	/** Returns {@link IntRenderable} instances. Renderables are obtained from the provided {@link Pool} and added to the provided
	 * array. The IntRenderables obtained using {@link Pool#obtain()} will later be put back into the pool, do not store them
	 * internally. The resulting array can be rendered via a {@link IntModelBatch}.
	 * @param renderables the output array
	 * @param pool the pool to obtain IntRenderables from */
	void getRenderables(Array<IntRenderable> renderables, Pool<IntRenderable> pool);
}
