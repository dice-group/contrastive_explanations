package anonymized.contrastive.experiments;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import anonymized.contrastive.ContrastiveExplanation;
import anonymized.contrastive.ContrastiveExplanationGenerator;
import anonymized.contrastive.ContrastiveExplanationProblem;
import anonymized.contrastive.experiments.classGeneration.ClassExpressionGenerator;
import anonymized.contrastive.experiments.classGeneration.ELClassExpressionGenerator;
import anonymized.contrastive.experiments.helpers.FoilCandidateFinder;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.LoggerFactory;
import anonymized.tools.Util;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ExperimenterWithClassExpressions {

    public static int MAX_ONT_SIZE=10000;

    public static enum ReasonerChoice { ELK, HERMIT }

    public static ExperimenterWithClasses.ReasonerChoice reasoner = ExperimenterWithClasses.ReasonerChoice.ELK;

    public static void main(String[] args) throws OWLOntologyCreationException {

        boolean conflictMinimal=false;

        if(args.length<3){
            System.out.println("Usage: ");
            System.out.println(ExperimenterWithClasses.class+ " ONTOLOGY CLASS_EXPRESSION_SIZE NUMBER_OF_REPITITIONS [ELK|HERMIT] [conflict-minimal]");
            System.exit(0);
        }
        if(args.length>=4){
            if(args[3].equals("HERMIT"))
                reasoner = ExperimenterWithClasses.ReasonerChoice.HERMIT;
            else if(!args[3].equals("ELK"))
                throw new IllegalArgumentException("Unexpected reasoner choice: "+args[3]);
        }
        if(args.length==5){
            if(!args[4].equals("conflict-minimal"))
                throw new IllegalArgumentException("Expected 'conflict-minimal' as 5th argument, got "+args[4]);
            conflictMinimal=true;

        }

        System.out.println("Reasoner choice: "+reasoner);

        ExperimenterWithClasses.reasoner=reasoner;

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger("org.semanticweb.owlapi");
        logger.setLevel(Level.OFF);
        loggerContext.getLogger("org.semanticweb.elk").setLevel(Level.OFF);
        loggerContext.getLogger("com.clarkparsia.owlapi").setLevel(Level.OFF);
        loggerContext.getLogger("uk.ac.manchester.cs.owlapi").setLevel(Level.OFF);

        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();

        final int classExpressionSize = Integer.parseInt(args[1]);

        final int maxIterations = Integer.parseInt(args[2]);

        System.out.println("Class expression size: "+classExpressionSize);
        System.out.println("Iterations: "+maxIterations);

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        System.out.println("Parsing ontology...");
        OWLOntology ont = manager.loadOntologyFromOntologyDocument(new File(args[0]));

        if(ont.getAxiomCount()>MAX_ONT_SIZE){
            System.out.println("Ontology has more than "+MAX_ONT_SIZE+" axioms!");
            System.exit(0);
        }

        // remove unsupported axioms
        List<OWLAxiom> toRemove = ont.axioms(Imports.INCLUDED)
                        .filter(x -> x.isOfType(AxiomType.TBoxAxiomTypes))
                        .filter( x-> x.individualsInSignature().findAny().isPresent())
                        .collect(Collectors.toList());
        ont.axioms(Imports.INCLUDED).filter(x -> x.isOfType(AxiomType.SAME_INDIVIDUAL))
                        .forEach(toRemove::add);
        ont.remove(toRemove);
        toRemove.forEach(System.out::println);
        System.out.println("Removed "+toRemove.size()+" unsupported axioms");

        Set<OWLNamedIndividual> allIndividuals = ont.individualsInSignature().collect(Collectors.toSet());

        List<OWLClassExpression> candidates = new ArrayList<>();
        Map<OWLClassExpression, List<OWLNamedIndividual>> facts = new HashMap<>();
        Map<OWLClassExpression, List<OWLNamedIndividual>> foils = new HashMap<>();

        int maxFoils = 0;
        int maxFacts = 0;

        OWLReasonerFactory reasonerFactory =
                reasoner== ExperimenterWithClasses.ReasonerChoice.HERMIT ?
                        new ReasonerFactory() :
                        new ElkReasonerFactory();

        ClassExpressionGenerator classExpressionGenerator =
                new ELClassExpressionGenerator(ont, factory);

        System.out.println("Computing Instantiation...");

        OWLReasoner reasoner = reasonerFactory.createReasoner(ont);

        System.out.println("Finding contrastive explanation problems...");

        Random random = new Random(0);

        FoilCandidateFinder foilCandidateFinder = new FoilCandidateFinder(ont, FoilCandidateFinder.Strategy.CommonClass);
        foilCandidateFinder.setReasoner(reasoner);

        List<OWLClassExpression> exps = classExpressionGenerator.generateClassExpressions(maxIterations,classExpressionSize);

        System.out.println("Done finding contrastive explanation concepts");

        if(exps.isEmpty())
            System.out.println("No contrastive problems found!");

        for(OWLClassExpression exp : exps)  {

            Set<OWLNamedIndividual> instances = reasoner.instances(exp)
                    .collect(Collectors.toSet());


            OWLNamedIndividual fact = Util.randomItem(instances,random);

            List<OWLNamedIndividual> others = foilCandidateFinder.foilCandidates(fact)
                    //ont.individualsInSignature()
                    .filter(x -> !instances.contains(x))
                    .collect(Collectors.toList());

            if(others.isEmpty()) {
                System.out.println("no match!");
                continue;
            }

            OWLNamedIndividual foil = Util.randomItem(others, random);

            ContrastiveExplanationProblem cep = new ContrastiveExplanationProblem(ont,exp,fact,foil);
            System.out.println("CEP: "+cep.toString(renderer));
            long startTime = System.currentTimeMillis();
            try {
                ContrastiveExplanationGenerator gen = new ContrastiveExplanationGenerator(manager);
                gen.useConflictMinimality(conflictMinimal);
                ContrastiveExplanation ce = gen.computeExplanation(cep);
                System.out.println("CE: " + ce.toString(renderer));
                long duration = System.currentTimeMillis() - startTime;
                int commonSize = ce.getCommon().size();
                int differenceSize = ce.getDifferent().size();
                int conflictSize = ce.getConflict().size();
                long freshIndividuals = ce.getFoilMapping()
                        .values()
                        .stream()
                        .filter(x -> !allIndividuals.contains(x))
                        .count();
                System.out.println("STATS: "+commonSize+" "+differenceSize+" "+conflictSize+" "+freshIndividuals+" "+duration);
            } catch(Error error){
                System.out.println("STATS: Exception "+error.getClass()+" "+error.getMessage());
                if(error instanceof OutOfMemoryError)
                    System.exit(1);
            }

        }


    }

    private static OWLClassExpression asUnsat(OWLAxiom axiom, OWLDataFactory factory) {
        if(axiom instanceof OWLClassAssertionAxiom) {
            OWLClassAssertionAxiom ca = (OWLClassAssertionAxiom)axiom;
            return factory.getOWLObjectIntersectionOf(factory.getOWLObjectOneOf(ca.getIndividual()), factory.getOWLObjectComplementOf(ca.getClassExpression()));
        } else
            throw new AssertionError("Not implemented!");
    }
}
