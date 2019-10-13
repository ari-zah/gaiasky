/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*-----------------------------------------------------------------------------
* Gaia Database Dictionary Tool
* Copyright (C) 2006-2011 Gaia Data Processing and Analysis Consortium
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
*
*------------------------------------------------------------------------*/

package gaiasky.util.gaia.time;


/**
 * Gaia Data Model.
 * 
 * Auto generated by Gaia Main Database Dictionary Tool on Wed Feb 12 17:22:59 CET 2014.
 * 
 */
public enum TimeScale {

    /**
     * Gaia's proper Time "Temp de Gaia"
     */
      TG,

    /**
     * Barycentric Coordinate Time
     */
      TCB,

    /**
     * Terrestial Time
     */
      TT,

    /**
     * Barycentric Dynamical Time
     */
      TDB,

    /**
     * Geocentric Coordinate Time
     */
      TCG,

    /**
     * Coordinated Universal Time
     */
      UTC,

    /**
     * International Atomic Time
     */
      TAI,

    /**
     * GPS time
     */
      GPS,

    /**
     * Embrace the unknown
     */
      UNKNOWN

}
