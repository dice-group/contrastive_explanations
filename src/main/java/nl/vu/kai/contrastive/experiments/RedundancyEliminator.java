package nl.vu.kai.contrastive.experiments;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.stream.Collectors;

public class RedundancyEliminator {
    /**
     * Remove redundant class assertions
     */
    public static void main(String[] args) throws OWLOntologyCreationException, FileNotFoundException, OWLOntologyStorageException {
        String input = args[0];
        String output = args[1];
        String reasonerChoice = args[2];

        OWLOntologyManager man = OWLManager.createOWLOntologyManager();

        System.out.println("Parsing ontology...");
        OWLOntology ont = man.loadOntologyFromOntologyDocument(new File(input));

        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();

        List<OWLClassAssertionAxiom> assertions = ont.aboxAxioms(Imports.INCLUDED)
                .filter(x -> x instanceof OWLClassAssertionAxiom)
                .map(x -> (OWLClassAssertionAxiom) x)
                .collect(Collectors.toList());
        ont.removeAxioms(assertions);

        OWLReasonerFactory reasonerFactory = reasonerChoice=="ELK"
                ? new ElkReasonerFactory()
                : new ReasonerFactory();

        OWLReasoner reasoner = reasonerFactory.createReasoner(ont);

        int redundant = 0;

        System.out.println("Checking for redundant assertions...");
        for(OWLClassAssertionAxiom ass: assertions) {
            reasoner.flush();
            if(!reasoner.isEntailed(ass))
                ont.add(ass);
            else {
                System.out.println("Redundant: " + renderer.render(ass));
                redundant++;
            }
        }

        System.out.println(redundant+" redundant assertions removed");

        System.out.println("Saving ontology...");
        man.saveOntology(ont, new FileOutputStream(new File(output)));
    }
}
