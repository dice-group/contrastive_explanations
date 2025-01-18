package nl.vu.kai.contrastive.helper;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import tools.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class IndividualGenerator {

    private static boolean DEBUG = true;

    // map pair to fresh individual
    private Map<Pair<OWLNamedIndividual, OWLNamedIndividual>, OWLNamedIndividual> pair2ind = new HashMap<>();
    private Map<OWLNamedIndividual, Pair<OWLNamedIndividual, OWLNamedIndividual>> ind2pair = new HashMap<>();

    private int counter = 0;

    private final OWLDataFactory factory;

    public IndividualGenerator(OWLDataFactory factory) {
        this.factory=factory;
    }

    public OWLNamedIndividual getIndividualForPair(OWLNamedIndividual ind1, OWLNamedIndividual ind2) {
        Pair<OWLNamedIndividual, OWLNamedIndividual> pair = new Pair(ind1, ind2);
        if (pair2ind.containsKey(pair))
            return pair2ind.get(pair);
        else {
            IRI name = DEBUG ? IRI.create(ind1.getIRI().getFragment()+"_"+ind2.getIRI().getFragment()) : IRI.create("_X" + counter);
            counter++;
            OWLNamedIndividual newInd = factory.getOWLNamedIndividual(name);
            pair2ind.put(pair, newInd);
            ind2pair.put(newInd, pair);
            return newInd;
        }
    }

    public Pair<OWLNamedIndividual, OWLNamedIndividual> getPairForIndividual(OWLNamedIndividual ind) {
        if (!ind2pair.containsKey(ind))
            return new Pair<>(ind,ind);
            //throw new IllegalArgumentException("Individual not known!");
        else
            return ind2pair.get(ind);
    }

    /**
     * Create set of all pairs over individuals
     *
     * @param individuals
     * @return
     */
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
