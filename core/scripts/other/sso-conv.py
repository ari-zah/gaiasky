#!/usr/bin/env python3

import json, io, re, datetime, jdcal, math

# Max number of asteroids to process
N_MAX = 90000
# Unit conversion
AU_TO_KM = 149598000
Y_TO_D = 365.25 
# Standard gravitational parameter of the Sun
GM_SUN = 1.32712440019e20

# Incoming date format
FMT = "%Y%m%d"

class SSO(object):
    def __init__(self, name, color, epoch, meananomaly, semimajoraxis, eccentricity, argofpericenter, ascendingnode, period, inclination):
        self.name = name
        self.color = color
        self.parent = "Sol"
        self.impl = "gaia.cu9.ari.gaiaorbit.scenegraph.Orbit"
        self.provider = "gaia.cu9.ari.gaiaorbit.data.orbit.OrbitalParametersProvider"
        self.ct = [ "Asteroids", "Orbits" ]
        self.transformFunction = "eclipticToEquatorial"
        self.orbit = {}
        self.orbit["epoch"] = epoch
        self.orbit["meananomaly"] = meananomaly
        self.orbit["semimajoraxis"] = semimajoraxis
        self.orbit["eccentricity"] = eccentricity
        self.orbit["argofpericenter"] = argofpericenter
        self.orbit["ascendingnode"] = ascendingnode
        self.orbit["period"] = period
        self.orbit["inclination"] = inclination
        self.onlybody = True
        

def to_json(line, idx):
    values = line.split(',')
    # Designation
    name = values[2]
    color = [0.4, 1.0, 0.4, 0.5]
    # Epoch in yyyymmdd, convert
    ymd = values[8]
    dt = datetime.datetime.strptime(ymd, FMT)
    epoch = sum(jdcal.gcal2jd(dt.year, dt.month, dt.day))
    # Mean anomaly [deg]
    meananomaly = float(values[9])
    # Semimajor axis [Km]
    a_au = float(values[14])
    semimajoraxis = a_au * AU_TO_KM
    # Eccentricity
    eccentricity = float(values[13])
    # Argument of pericenter [deg]
    argofpericenter = float(values[10])
    # Ascending node [deg]
    ascendingnode = float(values[11])
    # Period in days
    period = pow(a_au, 1.5) * Y_TO_D
    # Inclination [deg]
    inclination = float(values[12])
    
    bean = SSO(name, color, epoch, meananomaly, semimajoraxis, eccentricity, argofpericenter, ascendingnode, period, inclination)
    
    jsonstr = json.dumps(bean.__dict__)
    
    return jsonstr
    

with open('/media/tsagrista/Daten/Gaia/data/sso/DR2_aux_sso_orbit.csv', 'r') as fr:
    lines = fr.readlines()
    with open('/tmp/orbits_asteroids_dr2.json', 'w') as fw:
        fw.write("{\"objects\" : [\n")
        N = min(N_MAX, len(lines))
        for idx, line in enumerate(lines[1:N]):
            if line.strip():
                jsonstring = to_json(line, idx)
                fw.write(jsonstring)
                if idx < N - 2:
                    fw.write(",")
                fw.write("\n")

        fw.write("]}")
