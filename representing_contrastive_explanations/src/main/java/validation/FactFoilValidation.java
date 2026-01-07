package validation;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.*;

import static validation.constants.ValidationConstants.*;

public class FactFoilValidation {

    public static void validateFactFoil(String fact, String foil, String query, OWLOntology owlOntology) throws IllegalArgumentException {
        if (fact == null || fact.trim().isEmpty()) {
            throw new IllegalArgumentException(FACT_CANNOT_BE_NULL_OR_EMPTY);
        }
        if (foil == null || foil.trim().isEmpty()) {
            throw new IllegalArgumentException(FOIL_CANNOT_BE_NULL_OR_EMPTY);
        }
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException(QUERY_CANNOT_BE_NULL_OR_EMPTY);
        }
        if (fact.equals(foil)) {
            throw new IllegalArgumentException(FACT_FOIL_MUST_BE_DIFFERENT);
        }
        Boolean factExists =
                resolveEntity(fact, owlOntology);
        if (!factExists) {
            throw new IllegalArgumentException(FACT_NOT_FOUND_IN_ONTOLOGY + fact);
        }
        Boolean foilExists =
                resolveEntity(foil, owlOntology);
        if (!foilExists) {
            throw new IllegalArgumentException(FOIL_NOT_FOUND_IN_ONTOLOGY + foil);
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
