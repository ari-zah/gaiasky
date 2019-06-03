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

package gaia.cu9.ari.gaiaorbit.util.gdx.mesh;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * <p>
 * This implementation uses 32-bit integer indices instead of 16-bit shorts
 * </p>
 * <p>
 * IntIndexBufferObject wraps OpenGL's index buffer functionality to be used in conjunction with VBOs.
 * </p>
 * 
 * <p>
 * You can also use this to store indices for vertex arrays. Do not call {@link #bind()} or {@link #unbind()} in this case but
 * rather use {@link #getBuffer()} to use the buffer directly with glDrawElements. You must also create the IntIndexBufferObject with
 * the second constructor and specify isDirect as true as glDrawElements in conjunction with vertex arrays needs direct buffers.
 * </p>
 * 
 * <p>
 * VertexBufferObjects must be disposed via the {@link #dispose()} method when no longer needed
 * </p>
 * 
 * @author mzechner */
public class IntIndexBufferObjectSubData implements IntIndexData {
	final IntBuffer buffer;
	final ByteBuffer byteBuffer;
	int bufferHandle;
	final boolean isDirect;
	boolean isDirty = true;
	boolean isBound = false;
	final int usage;

	/** Creates a new IntIndexBufferObject.
	 * 
	 * @param isStatic whether the index buffer is static
	 * @param maxIndices the maximum number of indices this buffer can hold */
	public IntIndexBufferObjectSubData(boolean isStatic, int maxIndices) {
		byteBuffer = BufferUtils.newByteBuffer(maxIndices * 4);
		isDirect = true;

		usage = isStatic ? GL20.GL_STATIC_DRAW : GL20.GL_DYNAMIC_DRAW;
		buffer = byteBuffer.asIntBuffer();
		buffer.flip();
		byteBuffer.flip();
		bufferHandle = createBufferObject();
	}

	/** Creates a new IntIndexBufferObject to be used with vertex arrays.
	 * 
	 * @param maxIndices the maximum number of indices this buffer can hold */
	public IntIndexBufferObjectSubData(int maxIndices) {
		byteBuffer = BufferUtils.newByteBuffer(maxIndices * 4);
		this.isDirect = true;

		usage = GL20.GL_STATIC_DRAW;
		buffer = byteBuffer.asIntBuffer();
		buffer.flip();
		byteBuffer.flip();
		bufferHandle = createBufferObject();
	}

	private int createBufferObject () {
		int result = Gdx.gl20.glGenBuffer();
		Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, result);
		Gdx.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.capacity(), null, usage);
		Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
		return result;
	}

	/** @return the number of indices currently stored in this buffer */
	public int getNumIndices () {
		return buffer.limit();
	}

	/** @return the maximum number of indices this IntIndexBufferObject can store. */
	public int getNumMaxIndices () {
		return buffer.capacity();
	}

	/** <p>
	 * Sets the indices of this IntIndexBufferObject, discarding the old indices. The count must equal the number of indices to be
	 * copied to this IntIndexBufferObject.
	 * </p>
	 * 
	 * <p>
	 * This can be called in between calls to {@link #bind()} and {@link #unbind()}. The index data will be updated instantly.
	 * </p>
	 * 
	 * @param indices the vertex data
	 * @param offset the offset to start copying the data from
	 * @param count the number of floats to copy */
	public void setIndices (int[] indices, int offset, int count) {
		isDirty = true;
		buffer.clear();
		buffer.put(indices, offset, count);
		buffer.flip();
		byteBuffer.position(0);
		byteBuffer.limit(count << 2);

		if (isBound) {
			Gdx.gl20.glBufferSubData(GL20.GL_ELEMENT_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer);
			isDirty = false;
		}
	}

	public void setIndices (IntBuffer indices) {
		int pos = indices.position();
		isDirty = true;
		buffer.clear();
		buffer.put(indices);
		buffer.flip();
		indices.position(pos);
		byteBuffer.position(0);
		byteBuffer.limit(buffer.limit() << 2);
		
		if (isBound) {
			Gdx.gl20.glBufferSubData(GL20.GL_ELEMENT_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer);
			isDirty = false;
		}
	}

	@Override
	public void updateIndices (int targetOffset, int[] indices, int offset, int count) {
		isDirty = true;
		final int pos = byteBuffer.position();
		byteBuffer.position(targetOffset * 4);
		BufferUtils.copy(indices, offset, byteBuffer, count);
		byteBuffer.position(pos);
		buffer.position(0);

		if (isBound) {
			Gdx.gl20.glBufferSubData(GL20.GL_ELEMENT_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer);
			isDirty = false;
		}
	}

	
	/** <p>
	 * Returns the underlying IntBuffer. If you modify the buffer contents they wil be uploaded on the call to {@link #bind()}.
	 * If you need immediate uploading use {@link #setIndices(int[], int, int)}.
	 * </p>
	 * 
	 * @return the underlying int buffer. */
	public IntBuffer getBuffer () {
		isDirty = true;
		return buffer;
	}

	/** Binds this IntIndexBufferObject for rendering with glDrawElements. */
	public void bind () {
		if (bufferHandle == 0) throw new GdxRuntimeException("IntIndexBufferObject cannot be used after it has been disposed.");

		Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, bufferHandle);
		if (isDirty) {
			byteBuffer.limit(buffer.limit() * 4);
			Gdx.gl20.glBufferSubData(GL20.GL_ELEMENT_ARRAY_BUFFER, 0, byteBuffer.limit(), byteBuffer);
			isDirty = false;
		}
		isBound = true;
	}

	/** Unbinds this IntIndexBufferObject. */
	public void unbind () {
		Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
		isBound = false;
	}

	/** Invalidates the IntIndexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
	public void invalidate () {
		bufferHandle = createBufferObject();
		isDirty = true;
	}

	/** Disposes this IntIndexBufferObject and all its associated OpenGL resources. */
	public void dispose () {
		GL20 gl = Gdx.gl20;
		gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
		gl.glDeleteBuffer(bufferHandle);
		bufferHandle = 0;
	}
}
