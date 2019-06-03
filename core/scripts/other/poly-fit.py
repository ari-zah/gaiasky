#!/usr/bin/env python
import numpy as np
import csv

f = "/home/tsagrista/Downloads/Teff-result.csv"
cxp = []
teff = []
with open(f, 'r') as csvfile:
    spamreader = csv.reader(csvfile, delimiter=',')
    i = 1
    for row in spamreader:
        if i > 1:
            cxp.append(float(row[3]))
            teff.append(float(row[4]))
        i+=1

cxp = np.float64(cxp)
teff = np.float64(teff)

z = np.polyfit(cxp, teff,6)

print(z)

import matplotlib.pyplot as plt

p = np.poly1d(z)
p30 = np.poly1d(np.polyfit(cxp, teff, 30))

xp = np.linspace(-1, 7, 100)
_ = plt.plot(cxp, teff, '.', xp, p(xp), '-')
plt.ylim(2500, 10000)
plt.show()
