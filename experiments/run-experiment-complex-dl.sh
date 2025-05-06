#! /bin/bash

for i in `cat dl-corpus-sorted.csv`
do
    filename=$(basename "$i")
    echo $filename
    timeout 600 java  -Dlogback.configurationFile=logback.xml -cp contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar nl.vu.kai.contrastive.experiments.ExperimenterWithClassExpressions ~/Gits/Data/Ontologies/ORE2015/pool_sample/files/$filename 5 100 HERMIT &> $filename.log
done
