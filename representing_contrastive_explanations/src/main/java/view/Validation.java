package view;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.*;

public class Validation {

    public static void validateFactFoil(String fact, String foil, String query, OWLOntology owlOntology) throws IllegalArgumentException {
        if (fact == null || fact.trim().isEmpty()) {
            throw new IllegalArgumentException("Fact cannot be null or empty.");
        }
        if (foil == null || foil.trim().isEmpty()) {
            throw new IllegalArgumentException("Foil cannot be null or empty.");
        }
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty.");
        }
        if (fact.equals(foil)) {
            throw new IllegalArgumentException("Fact and foil must be different.");
        }
        Boolean factExists =
                resolveEntity(fact, owlOntology);
        if (!factExists) {
            throw new IllegalArgumentException("Fact not found in ontology: " + fact);
        }
        Boolean foilExists =
                resolveEntity(foil, owlOntology);
        if (!foilExists) {
            throw new IllegalArgumentException("Foil not found in ontology: " + foil);
        }
    }

    private static Boolean resolveEntity(
            String input,
            OWLOntology owlOntology) {
        ShortFormProvider sfp = new SimpleShortFormProvider();
        return owlOntology.getSignature().stream()
                .anyMatch(e -> sfp.getShortForm(e).equals(input));

    }
}
