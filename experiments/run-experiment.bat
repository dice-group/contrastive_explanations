@echo off
for /f %%i in (el-classification-track-by-size.txt) do (
    echo %%i
    timeout /t 600
    java -cp contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar nl.vu.kai.contrastive.experiments.ExperimenterWithClasses C:\path\to\Git\Data\ore2015_sample\pool_sample\files\%%i 1000 > %%i.log 2>&1
)
