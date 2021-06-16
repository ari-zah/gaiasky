/*******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

/**
 * This adds the lens dirt and starburst effects to the lens flare.
 * @see <a href=
 * "http://john-chapman-graphics.blogspot.co.uk/2013/02/pseudo-lens-flare.html">http://john-chapman-graphics.blogspot.co.uk/2013/02/pseudo-lens-flare.html</a>
 **/
public final class FlareDirt extends Filter<FlareDirt> {
    private Texture lensDirtTexture;
    private Texture lensStarburstTexture;
    private float starburstOffset;

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        LensDirt("u_texture1", 0),
        LensStarburst("u_texture2", 0),
        StarburstOffset("u_starburstOffset", 0);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String mnemonic, int arrayElementSize) {
            this.mnemonic = mnemonic;
            this.elementSize = arrayElementSize;
        }

        @Override
        public String mnemonic() {
            return this.mnemonic;
        }

        @Override
        public int arrayElementSize() {
            return this.elementSize;
        }
    }

    public FlareDirt() {
        super(ShaderLoader.fromFile("screenspace", "flaredirt"));
        rebind();
    }

    public void setLensDirtTexture(Texture tex) {
        this.lensDirtTexture = tex;
        setParam(Param.LensDirt, u_texture1);
    }

    public void setLensStarburstTexture(Texture tex) {
        this.lensStarburstTexture = tex;
        setParam(Param.LensStarburst, u_texture2);
    }

    public void setStarburstOffset(float offset){
        this.starburstOffset = offset;
        setParam(Param.StarburstOffset, offset);
    }

    @Override
    public void rebind() {
        // Re-implement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.LensDirt, u_texture1);
        setParams(Param.LensStarburst, u_texture2);
        setParams(Param.StarburstOffset, starburstOffset);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        lensDirtTexture.bind(u_texture1);
        if (lensStarburstTexture != null)
            lensStarburstTexture.bind(u_texture2);
    }
}
