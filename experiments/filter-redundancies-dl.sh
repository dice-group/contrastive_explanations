#! /bin/bash

for i in `cat dl-classification-track.txt`
do
    echo $i
    timeout 600 java  -Dlogback.configurationFile=logback.xml -cp contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar nl.vu.kai.contrastive.experiments.RedundancyEliminator ~/Gits/Data/Ontologies/ORE2015/pool_sample/files/$i $i-non-redundant.owl HERMIT
done


