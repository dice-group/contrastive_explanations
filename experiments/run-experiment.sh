#! /bin/bash

for i in `cat el-classification-track-by-size.txt`
do
    echo $i
    timeout 600 java -cp contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar nl.vu.kai.contrastive.experiments.ExperimenterWithClasses ~/Git/Data/ore2015_sample/pool_sample/files/$i 1000 &> $i.log
done


