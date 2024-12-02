# README: contrastive_explanations with Pellet Reasoner
## Overview
This program uses contrastive reasoning with OWL ontologies. Its aim is to explain why an individual belongs to a class and another does not. The goal is achieved by identifying missing axioms needed for the latter individual to satisfy the same query. The process leverages the Pellet reasoner for ontology reasoning and explanation generation.

## Features
1. **Ontology Loading:** Reads an OWL ontology file for reasoning.
2. **Query Parsing:** Supports parsing OWL class expressions with logical operators (`and`, `or`, `not`, `some`, `only`, `exactly`).
3. **Explanation Generation:** Provides explanations for why an individual satisfies a class expression query.
4. **Abduction Reasoning:** Identifies and outputs missing axioms for another individual to meet the query.

## Input File Format
- **Ontology Path:** Specifies the local path to the OWL ontology file.
- **Namespace:** Defines the namespace IRI for ontology terms.
- **Query:** Contains the OWL class expression to reason over.
- **Individuals:**
  - **`individual_with_explanation`:** Individual that satisfies the query.
  - **`individual_without_explanation`:** Individual for which missing axioms are computed.

### Example Input File
```
ontology=/path/to/ontology.owl
namespace=http://example.org/ontology#
query=(ClassA and (propertyA some ClassB))
individual_with_explanation=Individual1
individual_without_explanation=Individual2
```

## How to Use
1. **Prepare the Input File:** Ensure the file is formatted as shown above.
2. **Run the Program:**
   - Compile the program using required dependencies.
   - Execute the program and provide the input file path when prompted.
3. **View the Output:**
   - Query results and explanations for the selected individual.
   - Missing axioms required for the second individual.

## Output Details
- **Query:** The parsed class expression for reasoning.
- **Explanations:** A list of shared axioms that justify why the first individual satisfies the query.
- **Missing Axioms:** Axioms needed for the second individual to satisfy the query.
- **Execution Time:** Time taken for reasoning and explanation generation.

## Requirements
- **Java 8 or Higher**
- **Pellet Reasoner Library**
- **OWLAPI v3.5 or Higher**
- An ontology file in OWL format.

## Notes
- The ontology must include the individuals specified in the input file.
- Ensure all required libraries are included in the classpath during compilation and execution.
