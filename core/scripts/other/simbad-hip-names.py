#!/usr/bin/env python

import urllib3
import os


baseurl = "http://simbad.u-strasbg.fr/simbad/sim-id"
baseurl = "http://simbad.harvard.edu/simbad/sim-id"

http = urllib3.PoolManager()

out_file = open("/tmp/hip-names.csv", "w")

n = 117956
for i in range(1, n):
    print("Processing HIP %d" % i)
    response = http.request('GET', 
                baseurl,
                fields={'output.format': 'ASCII', 'Ident': 'HIP %d' % i})

    if response.status >= 200 and response.status < 300:
        res = response.data.decode('utf-8')
        idlines = False
        for line in res.splitlines():
            
            if idlines:
                if not line:
                    # Empty!
                    idlines = False
                    break
                tokens = line.split("  ")
                tokens = list(filter(None, tokens))
                for token in tokens:
                    token = token.strip()
                    if token.startswith('NAME'):
                        print("HIP %d -> %s" % (i, token[5:]))
                        out_file.write("%d,%s" %(i, token[5:]))

            if line.startswith("Identifiers"):
                idlines = True
    else:
        print("Request for HIP %d unsuccessful: %d" % (i, response.status))

out_file.close()
