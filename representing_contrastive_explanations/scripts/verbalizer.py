import os
import re
import argparse
import sys
from huggingface_hub import InferenceClient
from graph_representation import install_requirements


def extract_all_fact_foil_blocks(file_path):
    """
    Parses a log file to extract contrastive explanation blocks.
    Handles both single-line and multi-line CEP entries.
    Returns a list of block dicts with keys:
      class_expression, common, fact_name, foil_name,
      fact_mappings, foil_mappings, difference_axioms, conflicts
    """
    # 1) Read raw lines
    with open(file_path, 'r', encoding='utf-8') as f:
        raw = [ln.rstrip('\n') for ln in f]

    # 2) Preprocess: flatten multi-line CEPs
    lines = []
    i = 0
    while i < len(raw):
        ln = raw[i].strip()
        if ln.startswith("CEP:") and "Fact:" not in ln:
            # start of a multi-line CEP
            ce_parts = [ln]
            i += 1
            while i < len(raw):
                nxt = raw[i].strip()
                # stop on any known block marker or a fresh CEP:
                if any(nxt.startswith(tok) for tok in (
                        "Fact:", "Foil:", "CE: Common:", "Fact mapping:",
                        "Foil mapping:", "Different:", "Conflicts:", "CEP:"
                    )):
                    break
                ce_parts.append(nxt)
                i += 1
            # merge and push
            merged = " ".join(ce_parts)
            lines.append(merged)
            # do NOT consume the stopper line here—let the next loop handle it
        else:
            lines.append(ln)
            i += 1

    # 3) Now parse those flattened lines
    blocks = []
    current = {
        "class_expression": None,
        "common": None,
        "fact_name": None,
        "foil_name": None,
        "fact_mappings": {},
        "foil_mappings": {},
        "difference_axioms": [],
        "conflicts": None
    }

    for ln in lines:
        ln = ln.strip()

        # ---- CEP lines ----
        if ln.startswith("CEP:") and "ClassExpression:" in ln:
            # single-line CEP w/ inline fact & foil?
            if "Fact:" in ln and "Foil:" in ln:
                m = re.match(
                    r"CEP:\s*ClassExpression:(.+?),\s*Fact:\s*([^,]+),\s*Foil:\s*(\S+)",
                    ln
                )
                if m:
                    ce = re.sub(r"\s+", " ", m.group(1).strip())
                    current["class_expression"] = ce
                    current["fact_name"] = m.group(2).strip()
                    current["foil_name"] = m.group(3).strip()
                    continue

            # otherwise it's a flattened multi-line CEP
            # just grab after ClassExpression:
            ce = ln.split("ClassExpression:", 1)[1].strip()
            current["class_expression"] = re.sub(r"\s+", " ", ce)
            continue

        # ---- multi-line fact/foil lines ----
        if ln.startswith("Fact:"):
            current["fact_name"] = ln.split("Fact:",1)[1].strip()
            continue
        if ln.startswith("Foil:"):
            current["foil_name"] = ln.split("Foil:",1)[1].strip()
            continue

        # ---- common features ----
        if ln.startswith("CE: Common:"):
            current["common"] = ln.split("CE: Common:",1)[1].strip()
            continue

        # ---- mappings ----
        if ln.startswith("Fact mapping:"):
            for p in ln.split("Fact mapping:",1)[1].split(", "):
                if "->" in p:
                    var, val = p.split("->",1)
                    current["fact_mappings"][var.strip()] = val.strip()
            continue

        if ln.startswith("Foil mapping:"):
            for p in ln.split("Foil mapping:",1)[1].split(", "):
                if "->" in p:
                    var, val = p.split("->",1)
                    current["foil_mappings"][var.strip()] = val.strip()
            continue

        # ---- difference axioms ----
        if ln.startswith("Different:"):
            diff = ln.split("Different:",1)[1].strip()
            if diff:
                current["difference_axioms"].append(diff)
            continue

        # ---- finalize block ----
        if ln.startswith("Conflicts:"):
            conf = ln.split("Conflicts:",1)[1].strip() or None
            current["conflicts"] = conf
            blocks.append(current)
            # reset for next block
            current = {
                "class_expression": None,
                "common": None,
                "fact_name": None,
                "foil_name": None,
                "fact_mappings": {},
                "foil_mappings": {},
                "difference_axioms": [],
                "conflicts": None
            }
            continue

    # catch any trailing block
    if any([
        current["class_expression"],
        current["common"],
        current["fact_name"],
        current["foil_name"],
        current["difference_axioms"],
        current["conflicts"]
    ]):
        blocks.append(current)

    return blocks


def generate_natural_language_explanations(log_file, api_key, output_file = "../output/verbalizer/verbalizer_output.txt"):
    blocks = extract_all_fact_foil_blocks(log_file)

    prompt = (
        "You are given contrastive explanation blocks. Generate short paragraphs for each:\n"
        "- why fact qualifies\n"
        # "- why foil doesn't\n"
        "- what foil needs\n"
        "- the difference\n"
        # "- the commonality and\n"
        "- the conflicts\n"
        "Mention the names of all the instances while explaining."
        "Dont write any expressions such as '*' or similar"
        "Answer all the class expressions (100+), one block at a time, with no extra commentary.\n"
    )
    for i, blk in enumerate(blocks, start=1):
        prompt += f"--- Block {i} ---\n"
        prompt += f"ClassExpression: {blk['class_expression']}\n"
        prompt += f"Common: {blk['common']}\n"
        prompt += f"Fact: {blk['fact_name']}\n"
        prompt += f"Foil: {blk['foil_name']}\n"
        prompt += f"Fact mapping: {blk['fact_mappings']}\n"
        prompt += f"Foil mapping: {blk['foil_mappings']}\n"
        prompt += f"Different: {blk['difference_axioms']}\n"
        prompt += f"Conflicts: {blk['conflicts']}\n"

    client = InferenceClient(
        provider="together",
        api_key=api_key,
    )
    response = client.chat.completions.create(
        model="deepseek-ai/DeepSeek-V3",
        messages=[{"role": "user", "content": prompt}],
    )
    explanation_text = response.choices[0].message.content

    with open(output_file, 'w', encoding='utf-8') as fout:
        fout.write(explanation_text)

    print(f"Natural language explanations written to {output_file}")


if __name__ == "__main__":
    install_requirements()

    parser = argparse.ArgumentParser("Natural Language Representation of Fact vs Foil")
    parser.add_argument(
        "--input-file",
        type=str,
        # Change this to your input file path when running without cmd line
        default="../logs/family-ontology.owl.log",
        help="Path to the input file containing the mappings and axioms."
        # todo need to make this required = true before pushing
    )
    parser.add_argument(
        "--api-key",
        type=str,
        help="LLM model api key.",
        default = "key",
        #todo store default key here
    )
    try:
        args = parser.parse_args()
        log_path = f"{args.input_file}"
        api_key = f"{args.api_key}"
    except Exception as e:
        print(f"Error parsing arguments: {e}")
        sys.exit(1)

    generate_natural_language_explanations(log_path, api_key)
