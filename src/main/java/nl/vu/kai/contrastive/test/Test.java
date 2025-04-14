package nl.vu.kai.contrastive.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import nl.vu.kai.contrastive.ContrastiveExplanation;
import nl.vu.kai.contrastive.ContrastiveExplanationProblem;
import nl.vu.kai.contrastive.ContrastiveExplanationGenerator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.stream.Collectors;

public class Test {
    public static void main(String[] args) throws FileNotFoundException, OWLOntologyCreationException {

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger("org.semanticweb.owlapi");
        logger.setLevel(Level.OFF);
        loggerContext.getLogger("org.semanticweb.elk").setLevel(Level.OFF);
        loggerContext.getLogger("com.clarkparsia.owlapi").setLevel(Level.OFF);
        loggerContext.getLogger("uk.ac.manchester.cs.owlapi").setLevel(Level.OFF);

        OWLOntologyManager man = OWLManager.createOWLOntologyManager();

        OWLDataFactory factory = man.getOWLDataFactory();

        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();

        OWLOntology ontology = man.loadOntologyFromOntologyDocument(new FileInputStream(new File("./simple-example.owl")));

        System.out.println("This is the ontology: ");
        System.out.println("-------");
        System.out.println(ontology.axioms()
                .map(renderer::render)
                .collect(Collectors.joining("\n")));
        System.out.println("-------");

        {
            System.out.println("Explaining why 'a' is interesting but 'b' is not ");

            ContrastiveExplanationProblem problem =
                    new ContrastiveExplanationProblem(ontology,
                            factory.getOWLClass(IRI.create("http://www.semanticweb.org/patrickk/ontologies/2025/0/untitled-ontology-247#Interesting")),
                            factory.getOWLNamedIndividual(IRI.create("http://www.semanticweb.org/patrickk/ontologies/2025/0/untitled-ontology-247#a")),
                            factory.getOWLNamedIndividual(IRI.create("http://www.semanticweb.org/patrickk/ontologies/2025/0/untitled-ontology-247#b")));

            ContrastiveExplanationGenerator explainer = new ContrastiveExplanationGenerator(factory);

            ContrastiveExplanation expl = explainer.computeExplanation(problem);

            System.out.println(expl.toString(renderer));
        }
        System.out.println();

        {
            System.out.println("Explaining why 'a' is a GrandMother but 'b' is not ");

            ContrastiveExplanationProblem problem =
                    new ContrastiveExplanationProblem(ontology,
                            factory.getOWLClass(IRI.create("http://www.semanticweb.org/patrickk/ontologies/2025/0/untitled-ontology-247#GrandMother")),
                            factory.getOWLNamedIndividual(IRI.create("http://www.semanticweb.org/patrickk/ontologies/2025/0/untitled-ontology-247#a")),
                            factory.getOWLNamedIndividual(IRI.create("http://www.semanticweb.org/patrickk/ontologies/2025/0/untitled-ontology-247#b")));

            ContrastiveExplanationGenerator explainer = new ContrastiveExplanationGenerator(factory);

            ContrastiveExplanation expl = explainer.computeExplanation(problem);

            System.out.println(expl.toString(renderer));
        }
        System.out.println();
    }
}
