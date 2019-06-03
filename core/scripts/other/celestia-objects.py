#!/usr/bin/env python

import sys, math

if len(sys.argv) != 4:
    print("Usage:")
    print("%s dist[ly] radius[arcsec] ra[hou_r_angle]" % sys.argv[0])
    exit(0)

dist = float(sys.argv[1])
radius = float(sys.argv[2])
ra = float(sys.argv[3])

distpc = dist * 0.30660139194648
radiusrad = radius * 0.0000048481368110954
radeg = ra * 15.0

sizepc = math.tan(radiusrad) * distpc

print("Size [pc]: %f" % sizepc)
print("Dist [pc]: %f" % distpc)
print("RA  [deg]: %f" % radeg)
