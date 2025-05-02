#!/bin/bash

# Set the JAR file path (update with the actual path if different)
JAR_FILE="contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar"

# Java class with main method
JAVA_CLASS="nl.vu.kai.contrastive.experiments.IndividualCSVLogGenerator"

# Input file with list of logs
FILE_LIST="stats_file2.txt"

# Log folder path (where .log files reside)
LOG_FOLDER="$HOME/Contrastive_Abduction/Complex/redundancy_files_logs"

# Run the Java program using the JAR
echo "Running ComplexLogExtractorConverter from JAR..."
java -cp "$JAR_FILE" "$JAVA_CLASS" "$FILE_LIST" "$LOG_FOLDER"

