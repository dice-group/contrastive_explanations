#! /bin/bash

for i in `cat ~/Git/Data/ore2015_sample/pool_sample/el/instantiation/names-sorted-by-size.txt`
do
    echo $i
    timeout 600 java -cp contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar nl.vu.kai.contrastive.experiments.RedundancyEliminator ~/Git/Data/ore2015_sample/pool_sample/files/$i $i-non-redundant.owl ELK 
done


