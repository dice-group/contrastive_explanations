# Contrastive Explanations for ABox Entailments

## What This Code Does

- Generates random, complex OWL class expressions over a given ontology.
- Identifies **fact** individuals (satisfy the expression) and **foil** individuals (do not).
- Constructs **Contrastive Explanation Problems (CP)** by pairing a fact, a foil, and a target class.
- Computes contrastive explanations:
    - `q₁` (commonality): shared assertions between fact and foil.
    - `q₂` (difference): what fact has that foil lacks.
    - **Conflict set**: axioms preventing foil from satisfying the class.
- Reports statistics on explanation size, conflict size, runtime, and synthetic individuals introduced.

---

## Implementation Overview

### Main Driver: `ExperimenterWithClassExpressions`

- Loads OWL ontologies.
- Supports **ELK** or **HermiT** reasoners.
- Generates random complex class expressions.
- Identifies fact/foil individuals.
- Solves CPs and outputs results.

### ABox Processing: `ABoxProcessor`

- Augments ABox with synthetic individuals.
- Processes class/role assertions.
- Tracks axiom usage and transformations.

### Dependencies

- Java 8+
- [OWL API 5.1.20](https://github.com/owlcs/owlapi)
- [ELK Reasoner](https://github.com/liveontologies/elk-reasoner)
- [HermiT Reasoner](https://github.com/owlcs/hermit-reasoner)
- [EVEE Library](https://github.com/de-tu-dresden-inf-lat/evee) for justification computation
- SLF4J / Logback for logging

---
## Installation & Setup

### Step 1: Install EVEE

This project depends on the EVEE library. Please follow the installation steps provided in the official GitHub repository:
[EVEE GitHub Repository](https://github.com/de-tu-dresden-inf-lat/evee)

### Step 2: Build the Project

After EVEE is set up, build this project with:

```bash
mvn package
```

---

## Running Experiments

### Step-by-Step Guide

All scripts are located in the `experiments/` folder.

#### 1. Copy the JAR

After building, move the JAR into the `experiments/` folder:

#### 2. Download Ontologies

Download the ORE 2015 benchmark ontologies:
 [ORE 2015 Ontologies - Zenodo](https://zenodo.org/records/18578)

> **Note:** Do not commit extracted `.owl` files to the Git repository due to size.

#### 3. Update Script Path

Edit `experiments/run-rexperiment-complex.sh` and update the path to your ontology directory.

#### 4. Run the Experiment

```bash
cd experiments
./run-rexperiment-complex.sh
```

> This processes each ontology and produces explanation log files.

#### 5. Generate CSV from Logs

Convert the output logs to CSV format:

```bash
./run_log_to_csv.sh
```

---

## Explanation Components

### `q₁` - Commonality

ABox assertions shared between **fact** and **foil** individuals.

### `q₂` - Difference

Assertions that hold for the **fact** but not for the **foil**.

These components define a **contrastive explanation** — clarifying *why* one individual satisfies a concept while the other does not.

---

## Example Run

```bash
java -cp target/contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar \
    anonymized.contrastive.experiments.ExperimenterWithClassExpressions \
    examples/university.owl 4 20 ELK
```

- Runs 20 CPs
- Class expression size: 4
- Ontology: `university.owl`
- Reasoner: ELK

---

## Notes & Tips

- Ontologies with >10,000 axioms are skipped for performance reasons.
- Unsupported constructs (e.g., individual-based TBox axioms, `sameAs`) are filtered.
- Random seed is fixed (`0`) for reproducibility.
- Logging is suppressed for clarity — enable SLF4J if needed.

---

## Contributions & Contact

We welcome feedback and contributions! Please:

- Submit pull requests
- Open issues for bugs or suggestions
- *For academic inquiries or collaborations, contact the project maintainers via GitHub.*
