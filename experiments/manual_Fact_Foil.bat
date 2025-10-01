@echo off
:: ============================================
:: Run ManualFactFoilExperimenter from built JAR
:: ============================================

:: Path to the fat JAR
set "JAR_FILE=E:\Workspace_Dice\contrastive_explanations_3\experiments\contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar"

:: Fully qualified main class
set "MAIN_CLASS=anonymized.contrastive.experiments.ManualFactFoilExperimenter"

:: Path to JSON input file
set "INPUT_JSON=E:\Workspace_Dice\DataSource\family_json_input.json"

echo Running %MAIN_CLASS% with input %INPUT_JSON%...
java -cp "%JAR_FILE%" %MAIN_CLASS% "%INPUT_JSON%"

pause



@echo off
setlocal

set BASE=E:\Workspace_Dice\contrastive_explanations_3
set JAR=%BASE%\experiments\contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar
set CLASSES=%BASE%\target\classes

set INPUT=E:\Workspace_Dice\DataSource\family_json_input.json

echo Running ManualFactFoilExperimenter with input %INPUT%...
java -cp "%CLASSES%;%JAR%" anonymized.contrastive.experiments.ManualFactFoilExperimenter %INPUT%

pause
