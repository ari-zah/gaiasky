#!/usr/bin/env python

import json, io, re

class Country(object):
    def __init__(self, name, population, gdp, censusYear, perimeter):
        self.name = name
        self.parent = "Earth"
        self.impl = "gaia.cu9.ari.gaiaorbit.scenegraph.Area"
        self.ct = [ "Countries" ]
        self.population = population
        self.gdp = gdp
        self.censusYear = censusYear
        self.perimeter = perimeter

def nindex(mystr, substr, n=0, index=0):
    for _ in range(n+1):
        index = mystr.index(substr, index) + 1
    return index - 1

def to_json(line, num):
    n = 2
    groups = line.split('\"')
    '\"'.join(groups[:n]), '\"'.join(groups[n:])
    
    values = groups[2].split(",")
    
    name = values[9]
    if len(values) > 35:
        population = int(float(values[35]))
    else:
        population = -1
        
    if len(values) > 36:
        gdp = float(values[36])
    else:
        gdp = -1.0
    
    if len(values) > 38:
        censusyear = int(float(values[38]))
    else:
        censusyear = -1
    
    multipolygon = groups[1][14:-1]
    
    # Number of closed lines
    lines = multipolygon.count("((")
    print("%d: %s: %d lines" % (num, name, lines))
    
    lists = []
    
    for k in range(lines):
        i0 = nindex(multipolygon, "((", k)
        i1 = nindex(multipolygon, "))", k)
        
    
        numberstr = multipolygon[i0+2:i1]
        numberstr = re.sub(',', '', numberstr)
        numberstr = re.sub('\(', '', numberstr)
        numberstr = re.sub('\)', '', numberstr)
    
        aux = numberstr.split(" ")
        N = 2
        numbers = [aux[n:n+N] for n in range(0, len(aux), N)]
        
        
        lst = []
        
        for pointstr in numbers:
            point = [ float(pointstr[0]), float(pointstr[1]) ]
            lst.append(point)
            
        lists.append(lst)
    
    
    bean = Country(name, population, gdp, censusyear, lists)
    
    jsonstr = json.dumps(bean.__dict__)
    
    return jsonstr
    
        
    

with open('../../android/assets/data/countries.csv', 'r') as fr:
    lines = fr.readlines()
    with open('/tmp/countries.json', 'w') as fw:
        fw.write("{\"objects\" : [\n")
        N = len(lines)
        N = 50
        for idx, line in enumerate(lines[1:]):
            if line.strip():
                jsonstring = to_json(line, idx)
                fw.write(jsonstring)
                if idx < N - 2:
                    fw.write(",")
                fw.write("\n")
            
        fw.write("]}")
