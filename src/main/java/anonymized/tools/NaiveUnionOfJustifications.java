package anonymized.tools;

import com.clarkparsia.owlapi.explanation.MyBlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.MyHSTExplanationGenerator;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.util.HashSet;
import java.util.Set;

public class NaiveUnionOfJustifications {
    public static Set<OWLAxiom> unionOfJustifications(OWLOntology ontology, OWLClassExpression unsatClass, OWLReasonerFactory reasonerFactory) {
        MyBlackBoxExplanation expl =
                new MyBlackBoxExplanation(ontology, reasonerFactory, reasonerFactory.createReasoner(ontology));
        MyHSTExplanationGenerator explAll = new MyHSTExplanationGenerator(ontology.getABoxAxioms(Imports.INCLUDED), expl);

        Set<OWLAxiom> result = new HashSet<>();
        explAll.getExplanations(unsatClass).forEach(result::addAll);
        return result;
    }
}
