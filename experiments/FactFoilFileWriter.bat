@echo off
setlocal

set BASE=E:\Workspace_Dice\contrastive_explanations_3
set JAR=E:\Workspace_Dice\contrastive_explanations_3\experiments\contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar
set CLASSES=%BASE%\target\classes


set INPUT=E:\Workspace_Dice\contrastive_explanations_3\experiments\family_json_input.json

echo Running FactFoilFileWriter with input %INPUT%...
java -cp "%CLASSES%;%JAR%" anonymized.contrastive.experiments.FactFoilFileWriter %INPUT%
pause
