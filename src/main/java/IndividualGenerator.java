import javafx.util.Pair;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class IndividualGenerator {

    public Set<Pair<OWLNamedIndividual, OWLNamedIndividual>> generatePairs(Set<OWLNamedIndividual> individuals) {
        Set<Pair<OWLNamedIndividual, OWLNamedIndividual>> pairs = new HashSet<>();
        for (OWLNamedIndividual individual1 : individuals) {
            for (OWLNamedIndividual individual2 : individuals) {
                pairs.add(new Pair<>(individual1, individual2));
            }
        }
        return pairs;
    }

    // Formats the pairs as original OWLNamedIndividual form and sorts them for readability
    public Set<Pair<OWLNamedIndividual, OWLNamedIndividual>> formatPairs(Set<Pair<OWLNamedIndividual, OWLNamedIndividual>> pairs) {
        // Sort pairs based on their OWLNamedIndividual elements
        return pairs.stream()
                .sorted((p1, p2) -> {
                    // First compare based on the first OWLNamedIndividual (key)
                    int comp = p1.getKey().toString().compareTo(p2.getKey().toString());
                    if (comp != 0) return comp;

                    // If the first elements are equal, compare based on the second OWLNamedIndividual (value)
                    return p1.getValue().toString().compareTo(p2.getValue().toString());
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));  // Collect into a LinkedHashSet to maintain the order
    }
}
