#! /bin/bash

for i in `cat el-corpus-sorted.csv`
#for i in non-redundant-el/*owl
do
    filename=$(basename "$i")
    echo $filename
    #timeout 600 java -cp contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar nl.vu.kai.contrastive.experiments.ExperimenterWithClassExpressions ~/Git/Data/ore2015_sample/pool_sample/files/$i 7 1000 &> $i.log
    timeout 600 java  -Dlogback.configurationFile=logback.xml \
    -cp contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar \
    nl.vu.kai.contrastive.experiments.ExperimenterWithClassExpressions non-redundant-el/$filename 5 100 &> $filename.log
done
