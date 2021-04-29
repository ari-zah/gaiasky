/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

public interface IVisibilitySwitch {
    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    boolean isVisible();
    boolean isVisible(boolean attributeValue);

    void setVisible(boolean visible);
}
