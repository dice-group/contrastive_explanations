# Contrastive Explanations for ABox Entailments

## Project Overview

This project implements a reasoning system for computing **contrastive ABox explanations** over OWL ontologies, inspired by the methodology presented in the research paper:

> *“Can You Tell the Difference? Contrastive Explanations for ABox Entailments” (KR 2025)*

It aims to answer questions like:

> **Why is individual `a` an instance of concept `C`, but individual `b` is not?**

---

## What This Code Does

- Generates random, complex OWL class expressions over a given ontology.
- Identifies **fact** individuals (satisfy the expression) and **foil** individuals (do not).
- Constructs **Contrastive Explanation Problems (CEP)** by pairing a fact, a foil, and a target class.
- Computes contrastive explanations:
  - `q₁` (commonality): shared assertions between fact and foil.
  - `q₂` (difference): what fact has that foil lacks.
  - **Conflict set**: axioms preventing foil from satisfying the class.
- Reports statistics on explanation size, conflict size, runtime, and synthetic individuals introduced.

---

## Input JSON Format

You can download an example input JSON file here:  
[family_json_input.json](https://github.com/dice-group/contrastive_explanations/blob/manual_fact_foil/experiments/family_json_input.json)


### Field Explanations

The JSON file contains the following fields:

* **`ontology_input_file_path`**: Path to the OWL ontology to be loaded.
* **`output_file_path`**: Base path for saving the results file (text or JSON).
* **`reasoner`**: Choice of OWL reasoner (`HERMIT` or `ELK`).
* **`output_format`**: Format of the output file (`text` or `json`).
* **`experiments`**: Array of experiments, each containing:

    * `class_expression`: Manchester OWL syntax expression to evaluate.
    * `facts`: List of individuals that satisfy the class expression.
    * `foils`: List of individuals that do **not** satisfy the class expression.

---

## Running the Experiment

You can run the experiments either via the provided [`.bat`](https://github.com/dice-group/contrastive_explanations/blob/manual_fact_foil/experiments/FactFoilFileWriter.bat) scripts or directly from the command line.

### Command Line Example

```bash
java -cp <classes_path>;<jar_path> anonymized.contrastive.experiments.FactFoilFileWriter <input_json_file>
```

**Example `.bat` file content:**

### JAR File Explanation

* The JAR file (`contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar`) contains:

    * All compiled project classes.
    * All dependency libraries (OWL API, ELK, HermiT, EVEE, SLF4J).
* It allows running the experiments standalone without manually adding classpath dependencies.

---

## Explanation Components

* **`q₁` - Commonality**: ABox assertions shared between fact and foil individuals.
* **`q₂` - Difference**: Assertions that hold for the fact but not for the foil.
* **Conflict set**: Axioms preventing the foil from satisfying the class.

---

## Notes & Tips

* Ontologies with >10,000 axioms are skipped for performance reasons.
* Unsupported constructs (e.g., individual-based TBox axioms, `sameAs`) are filtered.
* Random seed is fixed (`0`) for reproducibility.
* Logging is suppressed by default — enable SLF4J if needed.

---

## Contributions & Contact

We welcome contributions:

* Submit pull requests.
* Open issues for bugs or suggestions.
* For academic inquiries or collaborations, contact the project maintainers via GitHub.

```
