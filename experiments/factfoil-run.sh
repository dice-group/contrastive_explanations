#!/bin/bash
# Exit immediately if any command fails
set -e

BASE="/Users/ashikmr/Desktop/contrastive_explanations"
JAR="$BASE/target/contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar"
CLASSES="$BASE/target/classes"
INPUT="/Users/ashikmr/Desktop/contrastive_explanations/experiments/family_json_input.json"

echo "Running FactFoilFileWriter with input $INPUT..."
java -cp "$CLASSES:$JAR" anonymized.contrastive.experiments.FactFoilFileWriter "$INPUT"

read -p "Press Enter to exit..."
xx