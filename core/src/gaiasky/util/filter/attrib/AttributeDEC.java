/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.I18n;

public class AttributeDEC extends AttributeAbstract implements IAttribute<IParticleRecord> {
    @Override
    public double get(IParticleRecord bean) {
        return bean.dec();
    }
    public String getUnit(){
        return "deg";
    }
    public String toString(){
        return "Declination (" + I18n.txt("gui.focusinfo.delta") + ")";
    }
}
