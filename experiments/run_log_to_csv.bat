@echo off

:: Step 1: Define paths
set "CLASS_PATH=..\target\classes"
set "JAVA_CLASS=nl.vu.kai.contrastive.experiments.IndividualCSVLogGenerator"
set "FILE_LIST=stats_file2.txt"
set "LOG_FOLDER=."

:: Step 2: Compile the Java class (optional - only needed if not already compiled)
echo Compiling Java class...
cd ..
javac -d target\classes src\main\java\nl\vu\kai\contrastive\experiments\IndividualCSVLogGenerator.java
cd experiments

:: Step 3: Run the Java program
echo Running LogStatsExtractor...
java -cp %CLASS_PATH% %JAVA_CLASS% %FILE_LIST% %LOG_FOLDER%

pause
