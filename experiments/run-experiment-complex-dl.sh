#! /bin/bash

for i in `cat ~/Git/Data/ore2015_sample/pool_sample/dl/instantiation/names-sorted-by-size.txt`
do
    echo $i
    #timeout 600 java -cp contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar nl.vu.kai.contrastive.experiments.ExperimenterWithClassExpressions ~/Git/Data/ore2015_sample/pool_sample/files/$i 7 1000 &> $i.log
    timeout 600 java  -Dlogback.configurationFile=logback.xml -cp contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar nl.vu.kai.contrastive.experiments.ExperimenterWithClassExpressions ~/Git/Data/ore2015_sample/pool_sample/files/$i 5 10 HERMIT &> $i.log
done
