Project Setup & Experimentation Guide
Installation
To compile and run this project, you need to install EVEE locally by following the instructions provided in the official repository:

🔗 EVEE GitHub Repository

Once EVEE is installed, you can compile the project using:

bash
Copy
Edit
mvn package
Running Experiments
All experiment scripts are located in the experiments/ subfolder.

Step 1: Copy the Compiled JAR
After successful compilation, copy the generated JAR file from the target/ directory into the experiments/ folder:

bash
Copy
Edit
cp target/contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar experiments/
Step 2: Download the ORE 2015 Ontology Repository
Download and extract the ORE 2015 ontology repository from the following link into a directory of your choice:

🔗 ORE 2015 Repository (Zenodo)

⚠️ Important: Do not add any of the ontology files to your Git repository. These files are large and should be kept out of version control.

Step 3: Run the Experiment
Update the file path in the run-rexperiment-complex.sh script to point to the location where you extracted the ontologies.

Then run the script from the command line:

bash
Copy
Edit
cd experiments
./run-rexperiment-complex.sh
This script will process each ontology and generate corresponding OWL log files. This may take some time depending on the dataset size.

⚠️ Again, ensure the generated OWL log files are not added to your Git repository.

Step 4: Generate CSV Statistics
To convert OWL log files into CSV statistics, run the following script:

bash
Copy
Edit
./run_log_to_csv.sh
This will produce one CSV file per OWL log file.

