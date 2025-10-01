# Contrastive Explanations for ABox Entailments

## Project Overview

This project implements a reasoning system for computing **contrastive ABox explanations** over OWL ontologies, inspired by the methodology presented in research paper:

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

## Implementation Overview

### Main Driver: `ExperimenterWithClassExpressions`

- Loads OWL ontologies.
- Supports **ELK** or **HermiT** reasoners.
- Generates random complex class expressions.
- Identifies fact/foil individuals.
- Solves CEPs and outputs results.

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

#### 1. Copy the JAR

After building, move the JAR into the `experiments/` folder:

#### 2. Run the Experiment

> To run the experiments, you need to provide JSON files that contain a list of class expressions along with their corresponding lists of facts and foils.
> 
## Explanation Components

### `q₁` - Commonality

ABox assertions shared between **fact** and **foil** individuals.

### `q₂` - Difference

Assertions that hold for the **fact** but not for the **foil**.

These components define a **contrastive explanation** — clarifying *why* one individual satisfies a concept while the other does not.

---

## Example Run

```bash
{
  "ontology_file_path": "E:/Workspace_Dice/DataSource/family.owl",
  "experiments": [
    {
      "class_expression": "Sister and (hasSibling some (married some (hasChild some Grandchild)))",
      "facts": ["F9F143", "F9F148"],
      "foils": ["F9M161", "F9M147"]
    },
    {
      "class_expression": "hasChild some Male",
      "facts": ["F10M171"],
      "foils": ["F4F56"]
    }
  ]
}
```

- > This processes Each class expression is processed together with all possible pairs formed from its facts and foils.
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
