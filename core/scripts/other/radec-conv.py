#!/usr/bin/env python

import sys, math

if len(sys.argv) != 3:
    print("Usage:")
    print("%s [RA HH:MM:SS] [DEC Deg:Arcmin:Arcsec] " % sys.argv[0])
    exit(0)

ra = sys.argv[1]
dec = sys.argv[2]

rai = ra.split(":")
deci = dec.split(":")

radeg = float(rai[0]) * 15.0 + float(rai[1]) * (1.0 / 60.0) + float(rai[2]) * (1.0 / 3600)
decdeg = float(deci[0]) + float(deci[1]) * (1.0 / 60.0) + float(deci[2]) * (1.0 / 3600.0)

print("RA,DEC: %f,%f deg" % (radeg, decdeg))
