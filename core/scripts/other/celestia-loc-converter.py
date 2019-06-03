"""
Converts location files from celestia to Gaia Sandbox.
:Author:
    Toni Sagrista Selles
:Organization:
    Astronomisches Rechen-Institut - Zentrum fur Astronomie Heidelberg - UNIVERSITAT HEIDELBERG
:Version:
    0.1

"""

import os
import re
import sys


class Loc:
    locCount = 0
    def __init__(self, name):
       Loc.locCount += 1
       self.name = name

def findnth(haystack, needle, n):
    parts = haystack.split(needle, n + 1)
    if len(parts) <= n + 1:
        return -1
    return len(haystack) - len(parts[-1]) - len(needle)

nargs = len(sys.argv)
if nargs < 3 or nargs > 5:
    print ("Usage: %s [celestia_file.ssc] [output_file] [[min_size] [max_size]]" % str(sys.argv[0]))
    exit()

ssc_file = open(str(sys.argv[1]), 'r')
output_file = open(str(sys.argv[2]), 'w')

min_size = 0.6
max_size = 10.0

if len(sys.argv) > 3:
    min_size = float(sys.argv[3])

if len(sys.argv) > 4:
    max_size = float(sys.argv[4])

loclist = []

for line in ssc_file.readlines():
    line = line.strip()
    if line.startswith("Location"):
        name = line[findnth(line, "\"", 0) + 1 : findnth(line, "\"", 1)]
        parent = line[findnth(line, "\"", 2) + 1 : findnth(line, "\"", 3)]
        parent = parent[parent.rfind("/") + 1 : len(parent)]

        current_loc = Loc(name)
        current_loc.parent = parent
        loclist.append(current_loc)
    elif line.startswith("LongLat"):
        lonlat = line[findnth(line, "[", 0) + 1 : findnth(line, "]", 1)].strip()
        lon = float(lonlat.split()[0])
        lat = float(lonlat.split()[1])
        loclist[len(loclist) - 1].lon = lon
        loclist[len(loclist) - 1].lat = lat
    elif line.startswith("Size"):
        tokens = line.split()
        size = float(tokens[1])
        loclist[len(loclist) - 1].size = size

print ("Writing json output file: %s..." % str(sys.argv[2]))

"""
Example:

    {
        "parent" : "Gaia", 
        "impl" : "gaia.cu9.ari.gaiaorbit.scenegraph.Loc",
        "ct"   : Others,
        "name" : "FoV2",                
        "location" : [141.0, 62.0],
        "size" : 0.00003,
        "distFactor" : 0.55
    },

"""

tab = '\t'
tab2 = tab + tab
nl = '\n'

output_file.write("{\"objects\" : [" + nl)

for i, loc in enumerate(loclist):
    if hasattr(loc, "size"):
        print("%s / %s / %.2f / %.2f / %.2f" % (loc.name, loc.parent, loc.lon, loc.lat, loc.size))
    else:
        print("%s / %s / %.2f / %.2f" % (loc.name, loc.parent, loc.lon, loc.lat))

    output_file.write(tab + "{" + nl)
    output_file.write(tab2 + "\"parent\" : \"%s\"," % loc.parent + nl)
    output_file.write(tab2 + "\"impl\" : \"gaia.cu9.ari.gaiaorbit.scenegraph.Loc\"," + nl)
    output_file.write(tab2 + "\"ct\" : Others," + nl)
    output_file.write(tab2 + "\"name\" : \"%s\"," % loc.name.title() + nl)
    output_file.write(tab2 + "\"location\" : [%.2f, %.2f]," % (loc.lon, loc.lat) + nl)

    if hasattr(loc, "size"):
        output_file.write(tab2 + "\"size\" : %.2f" % (max(min_size, min(max_size, loc.size * 3 / 500))) + nl)
    else:
        output_file.write(tab2 + "\"size\" : 2.0" + nl)

    output_file.write(tab + "}")

    if i != len(loclist) - 1:
        output_file.write("," + nl)
    else:
        output_file.write(nl)

output_file.write("]}" + nl)
