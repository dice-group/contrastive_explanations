package nl.vu.kai.contrastive;

import com.clarkparsia.owlapi.explanation.MyBlackBoxExplanation;
import nl.vu.kai.contrastive.conflicts.ConflictHandler;
import nl.vu.kai.contrastive.experiments.ExperimenterWithClasses;
import nl.vu.kai.contrastive.helper.RelevantScopeFinder;
import nl.vu.kai.contrastive.helper.IndividualGenerator;
//import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.HermiT.ReasonerFactory;
//import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.util.*;
import java.util.stream.Collectors;


public class ContrastiveExplanationGenerator {

    private boolean conflictOptimization=true;

    private final IndividualGenerator individualGenerator;

    private final ABoxProcessor aboxProcessor;

    private final OWLDataFactory factory;


    public ContrastiveExplanationGenerator(OWLDataFactory factory) {
        this.factory=factory;
        individualGenerator =new IndividualGenerator(factory);
        aboxProcessor=new ABoxProcessor(individualGenerator, factory);
    }

    public void useConflictMinimality(boolean conflictMinimal) {
        this.conflictOptimization=conflictMinimal;
    }

    private class Ontologies {
        Set<OWLAxiom> module,abox2,abox3;
        OWLOntology overApproximationOntology;
        Set<OWLAxiom> conflictSet = new HashSet<>();
    }

    public ContrastiveExplanation computeExplanation(ContrastiveExplanationProblem problem) throws OWLOntologyCreationException {

        if(!conflictOptimization) {
            // Step 0: make TBox "conflict-save"
            ConflictHandler conflictHandler = new ConflictHandler(problem.getOntology(), factory);
            conflictHandler.makeTBoxConflictSave();

            Ontologies ontologies = computeOntologies(problem);

            ContrastiveExplanation result = minimizeToExplanation(problem, ontologies);

            result = conflictHandler.addConflict(result);
            conflictHandler.restoreOntology();

            // Step 8: Return ContrastiveExplanation
            return result;
        } else {
            Ontologies ontologies = computeOntologies(problem);

            OWLReasonerFactory reasonerFactory = ExperimenterWithClasses.reasoner== ExperimenterWithClasses.ReasonerChoice.ELK ?
                    new ElkReasonerFactory() :
                    new ReasonerFactory();

            OWLOntologyManager manager = problem.getOntology().getOWLOntologyManager();
            OWLOntology foilVersion = instantiateFoils(ontologies.abox2, manager);
            foilVersion.addAxioms(ontologies.module);

            /*ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
            System.out.println("This is the foil ontology: ");
            System.out.println("-------");
            System.out.println(foilVersion.axioms()
                    .map(renderer::render)
                    .collect(Collectors.joining("\n")));
            System.out.println("-------");*/

            //System.out.println(reasonerFactory.createReasoner(foilVersion).isEntailed(factory.getOWLClassAssertionAxiom(problem.getOwlClassExpression(), problem.getFoil())));

            boolean success = minimizeConflict(ontologies, problem, foilVersion, reasonerFactory);
            if(!success)
                throw new AssertionError("Couldn't eliminate conflict---shouldn't be possible!");

            ontologies.module.retainAll(foilVersion.getAxioms());
            for(OWLAxiom axiom: new HashSet<>(ontologies.abox2)){
                if(!foilVersion.containsAxiom(instantiateFoil(axiom)))
                    ontologies.abox2.remove(axiom);
            }
            ontologies.abox3.retainAll(ontologies.abox2);

            return minimizeToExplanation(problem,ontologies);
        }
    }


    private Ontologies computeOntologies(ContrastiveExplanationProblem problem) throws OWLOntologyCreationException {
        Ontologies ontologies = new Ontologies();
        // Step 1: Use RelevantScopeFinder to get relevant axioms and individuals
        Set<OWLAxiom> relevantAxioms = RelevantScopeFinder.getRelevantAxioms(problem);
        Set<OWLNamedIndividual> relevantIndividuals = RelevantScopeFinder.getRelevantIndividuals(problem, relevantAxioms, factory);

        Set<OWLEntity> signature = relevantAxioms.stream()
                .flatMap(x -> x.signature())
                .collect(Collectors.toSet());
        signature.addAll(relevantIndividuals);
        signature.addAll(problem.getOwlClassExpression().getClassesInSignature());
        ontologies.module = RelevantScopeFinder.getModule(problem.getOntology(), signature);
        //ontologies.module = RelevantScopeFinder.getModule(problem, relevantIndividuals);

        // Step 2: Compute ABox2 and ABox3
        ontologies.abox2 = aboxProcessor.generateAbox2(relevantAxioms, relevantIndividuals);
        ontologies.abox3 = aboxProcessor.generateABox3(ontologies.module, ontologies.abox2);

        System.out.println("Size ABox2: "+ontologies.abox2.size());
        System.out.println("Size ABox3: "+ontologies.abox3.size());

        /*System.out.println("ABox 3:");
        abox3.forEach(System.out::println);
        System.out.println();*/
        System.out.println("Generated ABoxes");


        // Step 4: Construct overApproximationOntology
        OWLOntologyManager manager = problem.getOntology().getOWLOntologyManager();
        ontologies.overApproximationOntology = manager.createOntology();
        manager.addAxioms(ontologies.overApproximationOntology, ontologies.module);//problem.getOntology().getAxioms());
        manager.addAxioms(ontologies.overApproximationOntology, ontologies.abox2);


        return ontologies;
    }


    private boolean minimizeConflict(Ontologies ontologies,
                                        ContrastiveExplanationProblem problem,
                                        OWLOntology foilOntology,
                                        OWLReasonerFactory reasonerFactory) {
        OWLOntology ontology = foilOntology;
        OWLReasoner reasoner = reasonerFactory.createReasoner(foilOntology);
        if(reasoner.isConsistent()){
            if(!reasoner.isEntailed(factory.getOWLClassAssertionAxiom(problem.getOwlClassExpression(), problem.getFoil()))) {
                //System.out.println("Lost entailment! Backtracking...");
                return false; // need to backtrack
            } else
                return true;
        } else {
            MyBlackBoxExplanation expl = new MyBlackBoxExplanation(ontology, reasonerFactory, reasonerFactory.createReasoner(ontology));
            Set<OWLAxiom> exp = expl.getExplanation(factory.getOWLThing());
            //System.out.println("Explanation: "+exp.stream().map(Object::toString).collect(Collectors.joining(", ")));
            Set<OWLAxiom> fromOnt = new HashSet<>(exp);
            fromOnt.retainAll(ontologies.module);
            Set<OWLAxiom> fresh = new HashSet<>(exp);
            fresh.removeAll(ontologies.module);

            // try to remove fresh ones first
            for(OWLAxiom axiom:fresh){
                //System.out.println("Try removing fresh "+axiom);
                foilOntology.removeAxiom(axiom);
                boolean success = minimizeConflict(ontologies,problem,foilOntology,reasonerFactory);
                if(!success){
                    //System.out.println("Failed with "+axiom);
                    foilOntology.addAxiom(axiom);
                } else
                    return true;
            }

            // Otherwise build conflict set
            for(OWLAxiom axiom:fromOnt){
                if(axiom.isOfType(AxiomType.ABoxAxiomTypes)) {
                    foilOntology.removeAxiom(axiom);
                    //System.out.println("Try removing actual " + axiom);
                    //System.out.println("Failed with " + axiom);
                    boolean success = minimizeConflict(ontologies, problem, foilOntology, reasonerFactory);
                    if (!success) {
                        foilOntology.addAxiom(axiom);
                    } else {
                        ontologies.conflictSet.add(axiom);
                        return true;
                    }
                }
            }

            // all failed
            return false;
        }
    }


    public ContrastiveExplanation minimizeToExplanation(ContrastiveExplanationProblem problem, Ontologies ontologies) {

        OWLNamedIndividual combinedIndividual = individualGenerator.getIndividualForPair(problem.getFact(),problem.getFoil());

        // Step 3: Create "specialAxiom"
        OWLAxiom specialAxiom = factory.getOWLClassAssertionAxiom(problem.getOwlClassExpression(), combinedIndividual);

        /*System.out.println("Overapproximation: ");
        overApproximationOntology.axioms().forEach(System.out::println);
        System.out.println();*/

        Set<OWLAxiom> flexibleSet = new HashSet<>();
        flexibleSet.addAll(ontologies.abox2);
        flexibleSet.removeAll(ontologies.abox3);

        System.out.println("OverApproximated ontology size: "+ontologies.overApproximationOntology.getAxiomCount());
        System.out.println("Flexible: "+flexibleSet.size());

        // Step 5: Compute first explanation
        Set<OWLAxiom> different = computeExplanation(ontologies.overApproximationOntology, specialAxiom, flexibleSet);

        System.out.println("Computed first justification");

        // Step 6: Update overApproximationOntology
        OWLOntologyManager manager = problem.getOntology().getOWLOntologyManager();
        manager.removeAxioms(ontologies.overApproximationOntology, flexibleSet);
        manager.addAxioms(ontologies.overApproximationOntology, ontologies.abox3);
        manager.addAxioms(ontologies.overApproximationOntology, different);

        flexibleSet = ontologies.abox3;


        System.out.println("OverApproximated ontology size: "+ontologies.overApproximationOntology.getAxiomCount());
        System.out.println("Flexible: "+flexibleSet.size());

        // Step 7: Compute second explanation
        Set<OWLAxiom> common = computeExplanation(ontologies.overApproximationOntology, specialAxiom, flexibleSet);

        System.out.println("Computed second justification");

        // Step 8: extract mappings and conflict

        ContrastiveExplanation result = extractMappings(common,different, ontologies.conflictSet);
        return result;
    }

    private OWLOntology instantiateFoils(Set<OWLAxiom> abox, OWLOntologyManager manager) throws OWLOntologyCreationException {
        OWLOntology ontology = manager.createOntology();
        for(OWLAxiom axiom:abox){
            ontology.add(instantiateFoil(axiom));
        }
        return ontology;
    }

    private OWLAxiom instantiateFoil(OWLAxiom axiom){
        if(axiom instanceof OWLClassAssertionAxiom){
            OWLClassAssertionAxiom ass = (OWLClassAssertionAxiom) axiom;
            return factory.getOWLClassAssertionAxiom(ass.getClassExpression(),
                    individualGenerator.getPairForIndividual(ass.getIndividual().asOWLNamedIndividual()).getValue());
        } else if(axiom instanceof  OWLObjectPropertyAssertionAxiom){
            OWLObjectPropertyAssertionAxiom prp = (OWLObjectPropertyAssertionAxiom) axiom;
            return factory.getOWLObjectPropertyAssertionAxiom(prp.getProperty(),
                    individualGenerator.getPairForIndividual(prp.getSubject().asOWLNamedIndividual()).getValue(),
                    individualGenerator.getPairForIndividual(prp.getObject().asOWLNamedIndividual()).getValue());
        } else
            throw new AssertionError("Unexpected axiom to translate: "+axiom);
    }


    private ContrastiveExplanation extractMappings(Set<OWLAxiom> common, Set<OWLAxiom> different, Set<OWLAxiom> conflicts) {
        Map<OWLNamedIndividual,OWLNamedIndividual> factMap = new HashMap<>();
        Map<OWLNamedIndividual,OWLNamedIndividual> foilMap = new HashMap<>();
        for(OWLAxiom axiom:common) {
            for (OWLNamedIndividual ind : axiom.getIndividualsInSignature()) {
                factMap.put(ind, individualGenerator.getPairForIndividual(ind).getKey());
                foilMap.put(ind, individualGenerator.getPairForIndividual(ind).getValue());
            }
        }

        for(OWLAxiom axiom:different) {
            for (OWLNamedIndividual ind : axiom.getIndividualsInSignature()) {
                factMap.put(ind, individualGenerator.getPairForIndividual(ind).getKey());
                foilMap.put(ind, individualGenerator.getPairForIndividual(ind).getValue());
            }
        }
        return new ContrastiveExplanation(common,different,factMap,foilMap,conflicts);
    }

    private Set<OWLAxiom> computeExplanation(OWLOntology ontology, OWLAxiom axiom,
                                             Set<OWLAxiom> flexibleSet) {
        Set<OWLAxiom> fixedSet = new HashSet<>(ontology.getAxioms());
        fixedSet.removeAll(flexibleSet);
        //MyExplanation expl = new MyExplanation(ontology,fixedSet);
        //Set<OWLAxiom> result = expl.getEntailmentExplanation(axiom);

        //ElkReasonerFactory reasonerFactory = new ElkReasonerFactory();
        OWLReasonerFactory reasonerFactory = ExperimenterWithClasses.reasoner== ExperimenterWithClasses.ReasonerChoice.ELK ?
                new ElkReasonerFactory() :
                new ReasonerFactory();

        MyBlackBoxExplanation expl = new MyBlackBoxExplanation(ontology, reasonerFactory, reasonerFactory.createReasoner(ontology));
        expl.setStaticPart(fixedSet);
        Set<OWLAxiom> result = expl.getExplanation(asUnsat(axiom));


        result.removeAll(fixedSet);
        return result;
    }

    private OWLClassExpression asUnsat(OWLAxiom axiom) {
        if(axiom instanceof OWLClassAssertionAxiom) {
            OWLClassAssertionAxiom ca = (OWLClassAssertionAxiom)axiom;
            return factory.getOWLObjectIntersectionOf(factory.getOWLObjectOneOf(ca.getIndividual()), factory.getOWLObjectComplementOf(ca.getClassExpression()));
        } else
            throw new AssertionError("Not implemented!");
    }
}

