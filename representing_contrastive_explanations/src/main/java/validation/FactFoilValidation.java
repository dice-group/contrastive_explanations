package validation;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.*;

import static validation.constants.ValidationConstants.*;

/**
 * Validate that provided fact/foil/query inputs are present in the supplied OWL ontology.
 *
 * <p>This class throws {@link IllegalArgumentException} with standardized
 * messages from {@link validation.constants.ValidationConstants} when
 * validation fails.</p>
 */
public class FactFoilValidation {

    /**
     * Validate inputs for a contrastive explanation request.
     *
     * Checks performed:
     * - non\-null and non\-empty for fact, foil and query
     * - fact and foil are not identical
     * - both fact and foil exist in the provided ontology (by short form)
     *
     * @param fact lexical short form of the fact entity
     * @param foil lexical short form of the foil entity
     * @param query user query string (must be non\-empty)
     * @param owlOntology ontology used to resolve entity short forms
     * @throws IllegalArgumentException on any validation failure with a consistent message
     */
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

    /**
     * Resolve whether the provided short form matches any entity in the ontology.
     *
     * <p>This method uses a {@link SimpleShortFormProvider} to map entities to their
     * short forms (labels/names) and checks the ontology signature for any match.</p>
     *
     * @param input short form to resolve
     * @param owlOntology ontology to search
     * @return true if an entity with the given short form exists in the ontology
     */
    private static Boolean resolveEntity(
            String input,
            OWLOntology owlOntology) {
        ShortFormProvider sfp = new SimpleShortFormProvider();
        return owlOntology.getSignature().stream()
                .anyMatch(e -> sfp.getShortForm(e).equals(input));

    }
}
