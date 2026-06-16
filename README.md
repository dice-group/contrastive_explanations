# Contrastive Explanations Protégé Plugin

This document describes how to install and use the **Contrastive Explanations** plugin for Protégé.

## Installation

The plugin is distributed through a custom Protégé plugin repository.

By default, Protégé is configured to use the official plugin repository, which contains the standard Protégé plugins. To install the Contrastive Explanations plugin, first change the plugin repository URL to the custom repository.

### Step 1: Configure the Plugin Repository

1. Open **Protégé Settings**.
2. Select the **Plugins** tab.
3. Replace the default plugin repository URL with:

```text
https://ashik-18.github.io/contrastive-explanations-protege-plugin/plugins.repository
```

![Plugin Repository Settings](https://github.com/user-attachments/assets/5a28a814-734a-41f3-81f6-fca65a3159a2)

### Step 2: Install the Plugin

1. Navigate to **File → Check for Plugins**.
2. Select  **Contrastive Explanations** plugin and click **Install**.
3. Wait for the installation to complete.
4. Restart Protégé when prompted.

After restarting, the plugin will be available for use.

---

## Using the Plugin

1. Open the ontology you would like to analyze in Protégé.
2. Open the plugin view via:

```text
Window → Views → Ontology Views → Contrastive Explanations
```

![Opening the Plugin View](https://github.com/user-attachments/assets/62097f41-337e-4687-97d6-28b4ede3142e)

### Generating Explanations

The plugin view allows you to enter:

* **Fact** – the individual who entails the query.
* **Foil** – an alternative individual who does not entail the query.
* **Query** – the entailment to be explained.

After entering a valid Fact–Foil–Query triple:

1. Click **Generate Explanations**.
2. The plugin will compute a contrastive explanation.
3. Within a few seconds, an image will appear below the plugin view.

The generated explanation highlights:

* Why the selected **Fact** entails the specified **Query**.
* Which missing relationship prevent the **Foil** from entailing the same **Query**.

This enables users to understand not only *why* a query holds, but also *why an alternative does not*.

## Example
In the below example, the fact individual **F2F17**, entails the given query **Daugher and (hasParent some (married some person))**.  The foil individual **F6M100**, does not entail the query, as the relationship from the foil node to the daughter node is missing in the ontology. The missing relationship of the foil entity is highlighted with the red dotted line. 

<img width="514" height="416" alt="image" src="https://github.com/user-attachments/assets/8422bd32-9f57-4e33-acc5-3f525e2134df" />

