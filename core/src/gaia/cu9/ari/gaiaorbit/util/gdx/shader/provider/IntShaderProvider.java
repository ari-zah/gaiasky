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

package gaia.cu9.ari.gaiaorbit.util.gdx.shader.provider;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import gaia.cu9.ari.gaiaorbit.util.gdx.IntRenderable;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.IntShader;

/** Returns {@link IntShader} instances for a {@link IntRenderable} on request. Also responsible for disposing of any created
 * {@link ShaderProgram} instances on a call to {@link #dispose()}.
 * @author badlogic */
public interface IntShaderProvider {
	/** Returns a {@link IntShader} for the given {@link IntRenderable}. The RenderInstance may already contain a IntShader, in which case
	 * the provider may decide to return that.
	 * @param renderable the Renderable
	 * @return the IntShader to be used for the RenderInstance */
	IntShader getShader(IntRenderable renderable);

	/** Disposes all resources created by the provider */
	void dispose();
}
