# Installation

To make this compile, one has to locally install evee, following the
instructions on this page:

https://github.com/de-tu-dresden-inf-lat/evee

Then the project can be compiled with

- mvn package

# Running Experiments

The scripts for running the experiments are in the "experiments"-subfolder. After compiling the project, copy the file "contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar" from the target-folder to the experiments folder.

Download and unpack the ORE 2015 repository from here into some folder of your choice

- https://zenodo.org/records/18578

IMPORTANT: make sure none of the ontologies is ever added to the git repository, as these files are too large!

Adapt the folder name in "filer-redundancies.sh", and run the file from command line. This should take a while and create a new folder with processed ontologies that will be used for the experiment. Again, make sure these ontologies are never added to the git repository!

Now adapt the folder name in "run-experiment-complex.sh" - this is the script that runs the experiment.

Running that script will create a bunch of log files for the different ontologies. To create from them a csv-file with the statistics you run:

grep -h STATS *log|cut -d' ' -f1 --complement > statistics.csv


