"""
Crossmatch between constellation lines data from http://www.midnightkite.com/index.aspx?AID=0&URL=StarChartFAQ and the
processed HYG catalog. Output is a file with the constellation lines in the form [name, star1, star2]
:Author:
    Toni Sagrista Selles
:Organization:
    Astronomisches Rechen-Institut - Zentrum fur Astronomie Heidelberg - UNIVERSITAT HEIDELBERG
:Version:
    0.1

"""

import os

def isInt(s):
    try:
        int(s)
        return True
    except ValueError:
        return False

def isFloat(s):
    try:
        float(s)
        return True
    except ValueError:
        return False



fcl = open('../assets/data/ConstellationLinesAll2002.csv', 'r')
fhyg = open('../assets/data/hygxyz.csv', 'r')

""" List of stars as [id, ra, dec, mag] """
hyg = []

for p in fhyg:
    values = p.split('\t')
    if(len(values) > 8 and isInt(values[0]) and isFloat(values[7]) and isFloat(values[8])):
        hyg.append([int(values[0]), float(values[7]), float(values[8]), float(values[10])])


nomatches = 0
matches = 0
blanks = 0
outputlines = 0

with open('../assets/data/ConstellationLinesAll2002.csv', 'r') as f:
    total = sum(1 for _ in f)

out = open('../assets/data/constel.csv', 'w')
out.write('#constelname\tstar1id\n')

for i, line in enumerate(fcl):
    if(not line.startswith("#")):
        vals = line.split(',')
        for value in vals:
            value.strip()

        if(vals[2] and vals[3]):
            ra1 = float(vals[2])
            dec1 = float(vals[3])
            # print ("ra: %f, dec: %f" % (ra1, dec1))

            matchesList = []
            highestG = 500
            index = -1

            for j, star in enumerate(hyg):
                if(abs(ra1 - star[1]) < 0.0095 and abs(dec1 - star[2]) < 0.5):
                    # Match
                    if(star[3] < highestG):
                        index = j
                        highestG = star[3]


            if index >= 0:
                out.write(str(vals[0]) + "\t" + str(hyg[index][0]) + " \n")
                matches += 1
                outputlines += 1
            else:
                print ("No match found for index %i: ra %f dec %f" % (i, ra1, dec1))
                nomatches += 1
        else:
            blanks += 1
            outputlines += 1
            out.write("JUMP\tJUMP \n")

    else:
        blanks += 1

print("STATS")
print("Total lines %i" % total)
print("Matches %i" % matches)
print("No matches %i" % nomatches)
print("Blanks %i" % blanks)
print("Output lines %i" % outputlines)


