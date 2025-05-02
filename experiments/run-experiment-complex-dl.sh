#! /bin/bash

for i in non-redundant-dl/*owl
do
    filename=$(basename "$i")
    echo $filename
    timeout 600 java  -Dlogback.configurationFile=logback.xml -cp contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar nl.vu.kai.contrastive.experiments.ExperimenterWithClassExpressions non-redundant-dl/$filename 5 100 HERMIT &> $filename.log
done
