Got it! Here's the full content formatted exactly as a **README.md** file for your repo:

````markdown
# Contrastive Explanations for ABox Entailments

## 📘 Project Overview

This project implements a reasoning system for computing **contrastive ABox explanations** over OWL ontologies, following the methodology described in:

> *“Can You Tell the Difference? Contrastive Explanations for ABox Entailments” (KR 2025)*

It answers questions like:

> **Why is individual `a` an instance of concept `C`, but individual `b` is not?**

---

## 🔍 What Does This Code Do?

- Generates random complex OWL class expressions over an ontology.
- Finds **fact** individuals that satisfy a class expression and **foil** individuals that do not.
- Constructs a **Contrastive Explanation Problem (CEP)** by pairing a fact, a foil, and a target class.
- Computes a minimal contrastive explanation consisting of:
  - **q₁ (commonality):** what fact and foil share.
  - **q₂ (difference):** what fact has that foil is missing.
  - **Conflict set:** axioms that must be removed to make the foil satisfy the class.
- Produces statistics on explanation size, fresh individuals introduced, and runtime.

---

## 🏗 How Is It Implemented?

- **Main driver (`ExperimenterWithClassExpressions`):**
  - Loads OWL ontologies.
  - Supports `ELK` or `HermiT` reasoners (configurable).
  - Generates random complex class expressions.
  - Finds fact and foil individuals.
  - Builds and solves contrastive explanation problems.
  - Outputs explanations and statistics.

- **ABox processing (`ABoxProcessor`):**
  - Expands ABox assertions using synthetic individuals.
  - Processes class and role assertions for ABox augmentation.
  - Tracks axiom preservation or modification.

- **Dependencies:**
  - Java 8+
  - OWL API 5.1.20 for ontology parsing.
  - ELK reasoner for fast EL⊥ reasoning.
  - HermiT reasoner for expressive reasoning.
  - EVEE-LIB for justification extraction.
  - SLF4J / Logback for logging.

---

## 📥 Inputs

| Parameter                   | Description                                                                                         |
|-----------------------------|-----------------------------------------------------------------------------------------------------|
| OWL ontology file            | Path to `.owl` ontology file                                                                        |
| Class expression size (int)  | Size (complexity) of generated class expressions (e.g., conjunctions, restrictions)                  |
| Number of repetitions (int)  | Number of CEP problems to generate per run                                                          |
| Reasoner choice (optional)   | `"ELK"` (default) or `"HERMIT"`                                                                     |
| Conflict-minimality flag     | Optional `"conflict-minimal"` to minimize conflicts in explanations                                  |

---

## 📤 Outputs

| Output                      | Description                                                                                          |
|-----------------------------|-----------------------------------------------------------------------------------------------------|
| Explanation summary (console) | - Class expression<br>- Fact and foil individuals<br>- Explanation patterns (`q₁`, `q₂`)<br>- Conflict axioms |
| Statistics (console)        | - Sizes of commonality, difference, conflict<br>- Number of fresh individuals<br>- Runtime          |
| Optional logs               | Debug or error logs (logging suppressed by default)                                                 |

---

## ⚙ How to Build and Run

### Build

```bash
mvn package
````

### Run Experiment Directly

```bash
java -cp target/contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar \
    anonymized.contrastive.experiments.ExperimenterWithClassExpressions \
    path/to/ontology.owl 5 10 ELK conflict-minimal
```

Arguments:

1. OWL ontology file path.
2. Class expression size.
3. Number of CEP repetitions.
4. Reasoner choice (optional): `ELK` or `HERMIT`.
5. Conflict-minimal flag (optional).

---

### Step-by-Step Experiment Run

1. **Prepare ontology data:**

   Download and extract the ORE 2015 ontologies:

   [https://zenodo.org/records/18578](https://zenodo.org/records/18578)

   > *Note:* Do not commit extracted ontologies to Git due to size.

2. **Update experiment script:**

   Edit `experiments/run-rexperiment-complex.sh` to point to the directory containing extracted ontologies.

3. **Run the experiment:**

   ```bash
   cd experiments
   ./run-rexperiment-complex.sh
   ```

   This processes each ontology and generates OWL log files. Execution time depends on dataset size.

   > *Important:* Ensure OWL log files are **not** committed to the Git repository.

4. **Generate CSV statistics:**

   After running experiments, convert logs to CSV:

   ```bash
   ./run_log_to_csv.sh
   ```

---

## 📦 Explanation Components: q₁ and q₂

* **q₁ (Commonality):** ABox assertions shared by both fact and foil individuals.
* **q₂ (Difference):** ABox assertions true for the fact individual but missing or contradicted in the foil individual.

These form the core of contrastive explanations, highlighting why the fact satisfies the concept and the foil does not.

---

## 🚀 Example Run

```bash
java -cp target/contrastive-explanations-0.3-SNAPSHOT-jar-with-dependencies.jar \
    anonymized.contrastive.experiments.ExperimenterWithClassExpressions \
    examples/university.owl 4 20 ELK
```

Runs 20 CEP problems with class expression size 4 on `university.owl` using the ELK reasoner.

---

## 🧪 Notes and Tips

* Ontologies with >10,000 axioms are skipped for performance reasons.
* Unsupported axioms (e.g., TBox axioms with individuals, same-as assertions) are filtered out.
* Random seed fixed to `0` for reproducibility.
* Logging is minimized for clean output but can be enabled for debugging.

---

## Contact & Contributions

Please open issues or pull requests on GitHub for feedback or improvements.