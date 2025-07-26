/*
package anonymized.contrastive.experiments;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.*;

import java.io.File;
import java.util.Collections;

public class ManchesterSyntaxParserExample {
    public static void main(String[] args) throws Exception {
        // === Inputs ===
        String ontologyPath = "src/main/resources/family.owl"; // update to your .owl file
        String baseIRI = "http://www.benchmark.org/family#";
        //String manchesterSyntax = "hasChild some Male"; // example Manchester expression
        String manchesterSyntax = ":hasChild some :Male";

        // === Setup OWL API ===
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyPath));
        OWLDataFactory dataFactory = manager.getOWLDataFactory();

        // === Create prefix manager ===
        DefaultPrefixManager prefixManager = new DefaultPrefixManager(null, null, baseIRI);
        prefixManager.setPrefix("owl:", "http://www.w3.org/2002/07/owl#");

        // === Create short form provider from the ontology ===
        BidirectionalShortFormProvider shortFormProvider =
                new BidirectionalShortFormProviderAdapter(Collections.singleton(ontology), prefixManager);

        // === Create OWLEntityChecker using the short form provider ===
        OWLEntityChecker checker = new ShortFormEntityChecker(shortFormProvider);

        // === Create Manchester class expression parser with entity checker ===
        ManchesterOWLSyntaxClassExpressionParser parser =
                new ManchesterOWLSyntaxClassExpressionParser(dataFactory, checker);

        // === Parse and print class expression ===
        OWLClassExpression classExpression = parser.parse(manchesterSyntax);
        System.out.println("Parsed OWL Class Expression:");
        System.out.println(classExpression);
    }
}

*/



package anonymized.contrastive.experiments;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

import java.io.File;
import java.util.Collections;

public class ManchesterSyntaxParserExample {

    public static void main(String[] args) throws Exception {
        // === Inputs ===
        String ontologyPath = "src/main/resources/family.owl"; // Update to your ontology file path
        //String baseIRI = "http://www.benchmark.org/family#";

        // Complex Manchester Syntax expression to parse:
        String manchesterSyntax = "Brother or Grandfather or (hasChild only (not (Grandchild)))";
        //String manchesterSyntax = "Sister and (hasSibling some (married some (hasChild some Grandchild)))";
        //String manchesterSyntax = "Sister and owl:Thing and (hasSibling some (married some (owl:Thing and (hasChild some Grandchild))))";

        // === Setup OWL API ===
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyPath));
        OWLDataFactory dataFactory = manager.getOWLDataFactory();


        // === Setup Short Form Provider and Bidirectional Provider for resolving names ===
        BidirectionalShortFormProvider shortFormProvider =
                new BidirectionalShortFormProviderAdapter(Collections.singleton(ontology), new SimpleShortFormProvider());

        // === Create OWLEntityChecker using short form provider ===
        OWLEntityChecker entityChecker = new ShortFormEntityChecker(shortFormProvider);

        // === Create the Manchester Syntax Class Expression Parser with data factory and entity checker ===
        ManchesterOWLSyntaxClassExpressionParser parser =
                new ManchesterOWLSyntaxClassExpressionParser(dataFactory, entityChecker);

        // === Parse the expression ===
        OWLClassExpression classExpression = parser.parse(manchesterSyntax);

        // === Output the parsed class expression in OWL Functional Syntax ===
        System.out.println("Parsed OWL Class Expression:");
        System.out.println(classExpression);
    }
}
