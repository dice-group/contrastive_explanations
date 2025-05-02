#!/bin/bash

# Set the JAR file path (update with the actual path if different)
JAR_FILE="contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar"

# Java class with main method
JAVA_CLASS="nl.vu.kai.contrastive.experiments.ExtractSummaryStatsToCSV"

# Input file with list of logs
FILE_LIST="csv_file_list.txt"

# Log folder path (where .log files reside)
LOG_FOLDER="$HOME/Contrastive_Abduction/Complex/individual_csv"

# Run the Java program using the JAR
echo "Running CombineStatsCSVsFromList from JAR..."
java -cp "$JAR_FILE" "$JAVA_CLASS" "$FILE_LIST" "$LOG_FOLDER"
