/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 only, as published by
 * the Free Software Foundation. Oracle designates this particular file as
 * subject to the "Classpath" exception as provided by Oracle in the LICENSE
 * file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License version 2 for more
 * details (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this work; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA or
 * visit www.oracle.com if you need additional information or have any
 * questions.
 */

package gaia.cu9.ari.gaiaorbit.render;

import gaia.cu9.ari.gaiaorbit.util.I18n;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

/**
 * BitSet with some added functionality
 *
 * @author Toni Sagrista
 */
public class ComponentTypes extends BitSet {
    private static final long serialVersionUID = 1L;
    public static final int CT_SIZE = 32;

    public enum ComponentType {
        Stars("icon-elem-stars"),
        Planets("icon-elem-planets"),
        Moons("icon-elem-moons"),
        Satellites("icon-elem-satellites"),
        Asteroids("icon-elem-asteroids"),

        Clusters("icon-elem-clusters"),
        MilkyWay("icon-elem-milkyway"),
        Galaxies("icon-elem-galaxies"),
        Nebulae("icon-elem-nebulae"),
        Meshes("icon-elem-meshes"),

        Equatorial("icon-elem-equatorial"),
        Ecliptic("icon-elem-ecliptic"),
        Galactic("icon-elem-galactic"),
        Labels("icon-elem-labels"),
        Titles("icon-elem-titles"),

        Orbits("icon-elem-orbits"),
        Locations("icon-elem-locations"),
        Countries("icon-elem-countries"),
        Constellations("icon-elem-constellations"),
        Boundaries("icon-elem-boundaries"),

        Ruler("icon-elem-ruler"),
        Effects("icon-elem-effects"),
        Atmospheres("icon-elem-atmospheres"),
        Clouds("icon-elem-clouds"),
        Others("icon-elem-others"),

        VelocityVectors("icon-elem-arrows"),

        // ALWAYS LAST
        Invisible(null);

        private static Map<String, ComponentType> keysMap = new HashMap<>();

        static {
            for (ComponentType ct : ComponentType.values()) {
                keysMap.put(ct.key, ct);
            }
        }

        public String key;
        public String style;

        ComponentType(String icon) {
            this.key = "element." + name().toLowerCase();
            this.style = icon;
        }

        public String getName() {
            try {
                return I18n.bundle.get(key);
            } catch (MissingResourceException e) {
                return null;
            }
        }

        @Override
        public String toString() {
            return super.toString();
        }

        public static ComponentType getFromKey(String key) {
            return keysMap.get(key);
        }
    }

    public ComponentTypes() {
        super(CT_SIZE);
    }

    public ComponentTypes(int ordinal) {
        super(CT_SIZE);
        set(ordinal);
    }

    public ComponentTypes(ComponentType... cts) {
        super();
        for (ComponentType ct : cts)
            set(ct.ordinal());
    }

    /**
     * Returns the index of the rightmost bit set to 1. If no bits are set to 1,
     * returns -1
     *
     * @return The first ordinal
     */
    public int getFirstOrdinal() {
        return nextSetBit(0);
    }

    /**
     * Checks if all the t bits in this bit set are also set in other.
     *
     * @param other The bit set to check against
     * @return True if all the bits set to true in this bit set are also true in
     * other. Returns false otherwise
     */
    public boolean allSetLike(ComponentTypes other) {
        long thisval = this.toLongArray()[0];
        return (thisval & other.toLongArray()[0]) == thisval;
    }

    /**
     * Checks whether the given {@link ComponentType} is enabled in this
     * {@link ComponentTypes} instance
     * @param ct The component type to check
     * @return True if the component type is enabled
     */
    public boolean isEnabled(ComponentType ct){
        return this.get(ct.ordinal());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        ComponentType[] values = ComponentType.values();
        for (int i = nextSetBit(0); i >= 0; i = nextSetBit(i + 1)) {
            // operate on index i here
            sb.append(values[i]).append(" ");
            if (i == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
        }
        return sb.toString();
    }

}