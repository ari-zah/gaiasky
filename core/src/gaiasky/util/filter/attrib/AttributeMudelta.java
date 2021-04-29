/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scenegraph.particle.ParticleRecord;
import gaiasky.util.I18n;

public class AttributeMudelta extends AttributeAbstract implements IAttribute<ParticleRecord> {
    @Override
    public double get(ParticleRecord bean) {
        return bean.mudelta();
    }
    public String getUnit(){
        return "mas/yr";
    }
    public String toString(){
        return I18n.txt("gui.focusinfo.mudelta");
    }
}
