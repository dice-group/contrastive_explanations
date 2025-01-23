package nl.vu.kai.contrastive.conflicts;

import com.clarkparsia.owlapi.explanation.*;
import de.tu_dresden.inf.lat.prettyPrinting.datatypes.DisjointnessAxiom;
import nl.vu.kai.contrastive.ContrastiveExplanation;
import nl.vu.kai.contrastive.ContrastiveExplanationGenerator;
import nl.vu.kai.contrastive.helper.ContrastiveExplanationInstatiator;
import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Currently only supports EL
 */
public class ConflictHandler {
    private final OWLOntology ontology;
    private Set<OWLAxiom> removedAxioms;

    private OWLDataFactory factory;

    private ContrastiveExplanationInstatiator contrastiveExplanationInstatiator;

    public ConflictHandler(OWLOntology ontology, OWLDataFactory factory) {
        this.ontology=ontology;
        this.factory=factory;
        this.contrastiveExplanationInstatiator =
                new ContrastiveExplanationInstatiator(factory);
    }

    /**
     * Adapt the TBox so that no conflicts with the ABox are possible, assuming ontology is in EL
     */
    public void makeTBoxConflictSave(){
        removedAxioms = ontology.tboxAxioms(Imports.INCLUDED)
                .filter(this::conflictUnsave)
                .collect(Collectors.toSet());

        ontology.removeAxioms(removedAxioms);

        System.out.println("Removed "+removedAxioms.size()+" TBox axioms to avoid conflicts");
        //System.out.println("Those are: ");
        removedAxioms.stream()
                .map(OWLAxiom::toString)
                .forEach(System.out::println);
        System.out.println();
    }

    public void restoreOntology(){
        ontology.addAxioms(removedAxioms);
        removedAxioms=null;
    }

    public ContrastiveExplanation addConflict(ContrastiveExplanation explanation) {

        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();

        /*System.out.println("We are going to add the conflict for the following explanation:");
        System.out.println(explanation.toString(renderer));
        System.out.println();*/

        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        OWLOntology toRepair = null;

        try {
            toRepair = manager.createOntology();
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }

        Set<OWLAxiom> difference = contrastiveExplanationInstatiator
                .instantiateFoilDifference(explanation)
                .collect(Collectors.toSet());

        toRepair.addAxioms(ontology.axioms());
        toRepair.addAxioms(removedAxioms);
        toRepair.addAxioms(difference);

        /*System.out.println("The following may be inconsistent: ");
        toRepair.axioms()
                .filter(x -> x.isLogicalAxiom())
                .map(renderer::render)
                .forEach(System.out::println);
        */

        OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(toRepair);
        Set<OWLAxiom> conflict = new HashSet<>();
        while(!reasoner.isConsistent()){
            MyBlackBoxExplanation explanationGenerator =
                    new MyBlackBoxExplanation(toRepair,reasonerFactory,reasoner);
            explanationGenerator.setStaticPart(difference);
            Set<OWLAxiom> justification = explanationGenerator.getExplanation(factory.getOWLThing());
            //System.out.println("Justification: "+justification.stream().map(renderer::render).collect(Collectors.joining(", ")));
            Optional<OWLAxiom> toFix = justification
                    .stream()
                    .filter(x -> x.isOfType(AxiomType.ABoxAxiomTypes))
                    .filter(x -> !difference.contains(x))
                    .findFirst();
            if(toFix.isPresent()){
                toRepair.removeAxiom(toFix.get());
                conflict.add(toFix.get());
                reasoner.flush();
            } else
                throw new IllegalArgumentException("Conflict cannot be resolved by original ABox alone!");
        }

        return new ContrastiveExplanation(
                explanation.getCommon(),
                explanation.getDifferent(),
                explanation.getFactMapping(),
                explanation.getFoilMapping(),
                conflict);
    }



    /**
     * Check whether the axiom can contribute to a conflict in the ABox, assuming ontology is in EL
     */
    private boolean conflictUnsave(OWLAxiom axiom) {
        if(axiom instanceof OWLDisjointClassesAxiom)
            return true;
        else if(axiom instanceof OWLDisjointUnionAxiom)
            return true;
        else if(axiom instanceof OWLSubClassOfAxiom) {
            OWLSubClassOfAxiom sub = (OWLSubClassOfAxiom) axiom;
            return syntacticallyUnsatisfiable(sub.getSuperClass());
        }
        else if(axiom instanceof OWLSubClassOfAxiomShortCut){
            OWLSubClassOfAxiomShortCut shortCut = (OWLSubClassOfAxiomShortCut) axiom;
            return conflictUnsave(shortCut.asOWLSubClassOfAxiom());
        } else if(axiom instanceof OWLSubClassOfAxiomSetShortCut){
            OWLSubClassOfAxiomSetShortCut shortCut = (OWLSubClassOfAxiomSetShortCut) axiom;
            return shortCut.asOWLSubClassOfAxioms().stream().anyMatch(this::conflictUnsave);
        }
        return true;
    }

    private boolean syntacticallyUnsatisfiable(OWLClassExpression expression) {
        if(expression.isBottomEntity())
            return true;
        else if(expression instanceof OWLObjectIntersectionOf){
            OWLObjectIntersectionOf intersection = (OWLObjectIntersectionOf) expression;
            return intersection.conjunctSet()
                    .anyMatch(this::syntacticallyUnsatisfiable);
        }
        else if(expression instanceof OWLObjectSomeValuesFrom){
            OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) expression;
            return syntacticallyUnsatisfiable(some.getFiller());
        } else
            return false;
    }
}
