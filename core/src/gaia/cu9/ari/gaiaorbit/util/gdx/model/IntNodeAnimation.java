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

import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/** A NodeAnimation defines keyframes for a {@link IntNode} in a {@link IntModel}. The keyframes are given as a translation vector, a
 * rotation quaternion and a scale vector. Keyframes are interpolated linearly for now. Keytimes are given in seconds.
 * @author badlogic, Xoppa */
public class IntNodeAnimation {
	/** the Node affected by this animation **/
	public IntNode node;
	/** the translation keyframes if any (might be null), sorted by time ascending **/
	public Array<NodeKeyframe<Vector3>> translation = null;
	/** the rotation keyframes if any (might be null), sorted by time ascending **/
	public Array<NodeKeyframe<Quaternion>> rotation = null;
	/** the scaling keyframes if any (might be null), sorted by time ascending **/
	public Array<NodeKeyframe<Vector3>> scaling = null;
}
