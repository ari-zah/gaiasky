#!/bin/bash

INPUT=/db/gdr2/rawdata/gaiadr2_complements/geometric_distance/distances/*
OUTPUT=/dataB/gaiasky/geo_dist

i=0

for f in $INPUT
do
    if [ -f "${f}" ]; then
        f_name=$(basename $f)
        awk -F"," '{print $1,$2}' "$f" > "$OUTPUT/$f_name"
        i=$((i + 1))
        echo "$i - $f_name done"
    fi
done

