#!/usr/bin/env python3
import argparse
import json
import os
import subprocess
import sys

import graphviz
from graphviz import Digraph

def install_requirements():
    venv_dir = "scripts/venv"
        # os.path.join(os.path.dirname(__file__), "venv"))
    python_exe = os.path.join(venv_dir, "bin", "python")

    # 1. Create venv if it doesn't exist
    if not os.path.exists(venv_dir):
        subprocess.run([sys.executable, "-m", "venv", venv_dir], check=True)

    # 2. Install requirements using the venv’s python
    subprocess.run(
        [python_exe, "-m", "pip", "install", "-r",
         "requirements.txt",
         "-q", "--disable-pip-version-check", "--no-input"],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True
    )

    return python_exe

def apply_mapping_to_edges(axiom_strings, mapping):
    """
    axiom_strings like ["_X26 Type Father", "_X63 married _X53"]
    returns list of (subj, rel, obj) with mapping applied
    """
    edges = []
    if not axiom_strings:
        return edges

    for stmt in axiom_strings:
        if not stmt:
            continue
        parts = stmt.strip().split()
        if len(parts) != 3:
            # skip malformed lines
            continue
        subj, rel, obj = parts
        mapped_subj = mapping.get(subj, subj)
        mapped_obj = mapping.get(obj, obj)
        edges.append((mapped_subj, rel, mapped_obj))
    return edges


def color_for_node(flags):
    """
    flags is a dict like {"fact": True, "foil": True/False}
    Returns a fillcolor string.
    """
    fact = flags.get("fact", False)
    foil = flags.get("foil", False)
    if fact and foil:
        return "lightyellow"     # present in both
    if fact:
        return "#3399FF"         # fact-only
    if foil:
        return "#6AC780"         # foil-only
    return "white"


def build_and_save_graph(block_idx, class_expr, result, out_dir):
    """
    Build a single combined graph image for a (fact, foil) result.
    """
    # Extract parts
    fact_id = result.get("fact", "")
    foil_id = result.get("foil", "")
    common_axioms = result.get("common", []) or []
    different_axioms = result.get("different", []) or []
    fact_mapping = result.get("fact_mapping", {}) or {}
    foil_mapping = result.get("foil_mapping", {}) or {}

    # Build edges with mappings applied
    fact_edges = apply_mapping_to_edges(common_axioms, fact_mapping) + \
                 apply_mapping_to_edges(different_axioms, fact_mapping)
    foil_common_edges = apply_mapping_to_edges(common_axioms, foil_mapping)
    foil_diff_edges = apply_mapping_to_edges(different_axioms, foil_mapping)

    # Main graph
    title = f"Query: {class_expr} | fact={fact_id} | foil={foil_id}"
    dot = Digraph(
        # comment="Fact vs Foil in one image",
        graph_attr={
            'rankdir': 'LR',
            'splines': 'true',
            'bgcolor': 'lightyellow',
            'label': title,
            'fontsize': '20',
            'fontcolor': 'black'
        }
    )

    # Track node presence to color later
    node_flags = {}  # name -> {"fact": bool, "foil": bool}

    def flag(node, which):
        if node not in node_flags:
            node_flags[node] = {}
        node_flags[node][which] = True

    # FACT subgraph
    with dot.subgraph(name='cluster_fact') as f:
        f.attr(style='filled', color='lightyellow', label="", fontsize='16', fontcolor='black', labelloc='t')
        for s, r, o in fact_edges:
            f.edge(s, o, label=r)
            flag(s, "fact")
            flag(o, "fact")

    # FOIL subgraph (common edges)
    with dot.subgraph(name='cluster_foil_common') as f:
        f.attr(style='filled', color='lightyellow', label="", fontsize='16', fontcolor='black', labelloc='t')
        for s, r, o in foil_common_edges:
            f.edge(s, o, label=r)
            flag(s, "foil")
            flag(o, "foil")

    # FOIL subgraph (different edges, dashed red)
    with dot.subgraph(name='cluster_foil_diff') as f:
        f.attr(style='filled', color='lightyellow', fontsize='16', fontcolor='black', labelloc='t')
        for s, r, o in foil_diff_edges:
            f.edge(s, o, label=r, style='dashed', color='#ff0000', penwidth='2')
            flag(s, "foil")
            flag(o, "foil")

    # Apply node coloring after edges are in
    for node, flags in node_flags.items():
        dot.node(node, style='filled', fillcolor=color_for_node(flags))

    # Legend
    dot.node(
        'legend',
        label="""<
            <TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="4">
              <TR><TD COLSPAN="2"><B>Legend</B></TD></TR>
              <TR><TD BGCOLOR="#3399FF"></TD><TD>Fact node</TD></TR>
              <TR><TD BGCOLOR="#6AC780"></TD><TD>Foil node</TD></TR>
              <TR><TD BGCOLOR="lightyellow"></TD><TD>Common node</TD></TR>
              <TR><TD><FONT COLOR="#ff0000"><I>--------</I></FONT></TD><TD>Missing edge in foil</TD></TR>
            </TABLE>
        >""",
        shape='none'
    )

    # Ensure output directory exists
    # os.makedirs(out_dir, exist_ok=True)
    # output_file = os.path.join(out_dir, f"graph-{block_idx}")
    # dot.render(output_file, format="png", cleanup=True)
    # print(f"Graph saved to: {output_file}.png")
    print(str(dot), flush=True)

def main():
    # install_requirements()
    parser = argparse.ArgumentParser("Graph Representation of Fact vs Foil (JSON input)")
    parser.add_argument(
        "--input-file",
        type=str,
        # required=True,
        # default="/Users/ashikmr/Desktop/contrastive_explanations/representing_contrastive_explanations/outputs/reasoner/family_output.json",
        help="Path to the JSON file containing blocks with class_expression and results."
    )
    parser.add_argument(
        "--out-dir",
        type=str,
        # default="/Users/ashikmr/Desktop/contrastive_explanations/representing_contrastive_explanations/outputs/explanation/graph",
        help="Directory to write PNG files into."
    )
    args = parser.parse_args()

    with open(args.input_file, "r") as f:
        data = json.load(f)

    # Expecting top-level list of blocks, each with:
    # {
    #   "namespace": "...",
    #   "class_expression": "...",
    #   "results": [ {...} ]
    # }
    idx = 1
    for block in data:
        class_expr = block.get("class_expression", "<no class expression>")
        results = block.get("results", []) or []
        for res in results:
            build_and_save_graph(idx, class_expr, res, args.out_dir)
            idx += 1


if __name__ == "__main__":
    main()
