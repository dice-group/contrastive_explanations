@echo off
REM Loop through each lin   e in the file
for /f "delims=" %%i in (el-classification-track-by-size_dup.txt) do (
    REM Print the file name
    echo %%i

    REM Run Java command with timeout
    REM Timeout 600 seconds (10 minutes)
    java -cp contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar nl.vu.kai.contrastive.experiments.ExperimenterWithClasses E:\Workspace_Dice\DataSource\Downloads\pool_sample\files2\%%i 1000 > %%i.log
)
