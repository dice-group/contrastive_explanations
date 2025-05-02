@echo off

:: Define paths
set "CLASS_PATH=..\target\classes"
set "JAVA_CLASS=nl.vu.kai.contrastive.experiments.ExtractSummaryStatsToCSV"
set "FILE_LIST=csv_file_list.txt"
set "CSV_FOLDER=."

:: Compile the Java class
echo Compiling Java class...
cd ..
javac -d target\classes src\main\java\nl\vu\kai\contrastive\experiments\ExtractSummaryStatsToCSV.java
cd experiments

:: Run the Java program
echo Running ExtractSummaryStatsToCSV...
java -cp %CLASS_PATH% %JAVA_CLASS% %FILE_LIST% %CSV_FOLDER%

pause
