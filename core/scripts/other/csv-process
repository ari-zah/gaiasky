#!/bin/bash

# Processes the source GDR2 *.csv.gz files extracting the desired columns for Gaia Sky


print_usage() {
    echo "$0: convert DR2 csv.gz files for use in Gaia Sky octree generator"
    echo
    echo "Usage:"
    echo "  $0 input_folder [output_folder]"
}

# This flag controls whether we overwrite the files or not
# 0 - do not overwrite in same folder
# 1 - overwrite
# 2 - do not overwrite, different folder
OW=1

if [ $# -lt 1 ]; then
    print_usage
    exit 1
fi

IN_FOLDER=$1

if [ $# -gt 1 ]; then
    OUT_FOLDER=$2
    OW=2
fi

if [ ! -d "${IN_FOLDER}" ]; then
    echo "Error: $IN_FOLDER is not a directory!"
    print_usage
    exit 1
fi

count=0
for FILE in $IN_FOLDER/*.csv.gz; do
    echo "Processing: $FILE"
    NAME=`echo "$FILE" | cut -d'.' -f1`
    NEW_NAME=$NAME.csv.processed.gz

    zcat $FILE | awk -F "," '{print $3 "," $6 "," $8 "," $10 "," $7 "," $9 "," $11 "," $13 "," $15 "," $67 "," $14 "," $16 "," $68 "," $51 "," $56 "," $61 "," $5 "," $79 "," $89 "," $82 "," $85 }' | gzip > $NEW_NAME
    (( count++ ))
    if [ $OW -eq 1 ]; then
        # Overwrite
        mv $NEW_NAME $FILE
    elif [ $OW -eq 2 ]; then
        mv $NEW_NAME $OUT_FOLDER/$NAME.csv.gz
    fi
done

echo "Processed $count files in $IN_FOLDER"
