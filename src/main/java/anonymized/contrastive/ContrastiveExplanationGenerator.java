package anonymized.contrastive;

import com.clarkparsia.owlapi.explanation.MyBlackBoxExplanation;
import anonymized.contrastive.conflicts.ConflictHandler;
import anonymized.contrastive.experiments.ExperimenterWithClasses;
import anonymized.contrastive.helper.RelevantScopeFinder;
import anonymized.contrastive.helper.IndividualGenerator;
import anonymized.tools.MultiMap;
import anonymized.tools.Pair;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;
import java.util.stream.Collectors;


/**
 * This class implements the algorithm for computing difference-minimal contrastive explanations shown in the
 * supplemental material of the paper, sketched in the evaluation section of the main text, and based on the
 * procedure described in the section on difference-minimal explanations.
 */
public class ContrastiveExplanationGenerator {

    private boolean conflictOptimization=false;

    private final IndividualGenerator individualGenerator;

    private final ABoxProcessor aboxProcessor;

    private final OWLDataFactory factory;

    private final OWLOntologyManager manager;

    private final OWLReasonerFactory reasonerFactory;

    public ContrastiveExplanationGenerator(OWLOntologyManager manager) {
        this.manager=manager;
        this.factory=manager.getOWLDataFactory();
        individualGenerator =new IndividualGenerator(factory);
        aboxProcessor=new ABoxProcessor(individualGenerator, factory);

        reasonerFactory = ExperimenterWithClasses.reasoner== ExperimenterWithClasses.ReasonerChoice.ELK ?
                new ElkReasonerFactory() :
                new ReasonerFactory();
    }

    public void useConflictMinimality(boolean conflictMinimal) {
        this.conflictOptimization=conflictMinimal;
    }

    /**
     * Data structure to contain initially the CE super structure, and later the minimized CP
     */
    private class Ontologies {
        Set<OWLAxiom> module,abox2,abox3; // abox2 corresponds to q in the super structure, abox3 to q_com, the module is used as replacement of the ontology, for optimization
        OWLOntology overApproximationOntology; // this is the union of q and T, needed for reasoning and repair
        Set<OWLAxiom> conflictSet = new HashSet<>();
    }

    /**
     * Computing difference-minimal explanations as described in the paper.
     */
    public ContrastiveExplanation computeExplanation(ContrastiveExplanationProblem problem) throws OWLOntologyCreationException {

        if(!conflictOptimization) {
            Ontologies ontologies = computeOntologies(problem);

            makeABoxConsistent(problem, ontologies);

            ContrastiveExplanation result = minimizeToExplanation(problem, ontologies);
            //conflictHandler.restoreOntology();

            // Step 8: Return ContrastiveExplanation
            return result;
        } else {

            // we first compute the super structure
            Ontologies ontologies = computeOntologies(problem);

            // Step P1' from the paper: remove conflicts
            OWLOntologyManager manager = problem.getOntology().getOWLOntologyManager();
            OWLOntology foilVersion = instantiateFoils(ontologies.abox2, manager);
            foilVersion.addAxioms(ontologies.module);

            boolean success = minimizeConflict(ontologies, problem, foilVersion);
            if(!success)
                throw new AssertionError("Couldn't eliminate conflict---shouldn't be possible!");

            ontologies.module.retainAll(foilVersion.getAxioms());
            for(OWLAxiom axiom: new HashSet<>(ontologies.abox2)){
                if(!foilVersion.containsAxiom(instantiateFoil(axiom)))
                    ontologies.abox2.remove(axiom);
            }
            ontologies.abox3.retainAll(ontologies.abox2);

            // P2' - P4' are implemented in minimizeToExplanation
            return minimizeToExplanation(problem,ontologies);
        }
    }


    private Ontologies computeOntologies(ContrastiveExplanationProblem problem) throws OWLOntologyCreationException {

        Ontologies ontologies = new Ontologies();

        // from the paper: compute the set A' of assertions
        Set<OWLAxiom> relevantAxioms = RelevantScopeFinder.getRelevantAxioms(problem);

        // from the paper: compute the set I of relevant individual names
        Set<OWLNamedIndividual> relevantIndividuals = RelevantScopeFinder.getRelevantIndividuals(problem, relevantAxioms, factory);

        // optimization: compute a module for the relevant signature
        Set<OWLEntity> signature = relevantAxioms.stream()
                .flatMap(x -> x.signature())
                .collect(Collectors.toSet());
        signature.addAll(relevantIndividuals);
        signature.addAll(problem.getOwlClassExpression().getClassesInSignature());
        ontologies.module = RelevantScopeFinder.getModule(problem.getOntology(), signature);
        // abox2 and abox3 correspond to the ABox patterns q_m and q_com for the from the paper
        ontologies.abox2 = aboxProcessor.generateAbox2(relevantAxioms, relevantIndividuals);
        ontologies.abox3 = aboxProcessor.generateABox3(ontologies.module, ontologies.abox2);

        System.out.println("Size ABox2: "+ontologies.abox2.size());
        System.out.println("Size ABox3: "+ontologies.abox3.size());
        System.out.println("Generated ABoxes");


        // the over approximated ontology is the union of the TBox and the ABox pattern
        OWLOntologyManager manager = problem.getOntology().getOWLOntologyManager();
        ontologies.overApproximationOntology = manager.createOntology();
        ontologies.module.stream().filter(x -> x.isOfType(AxiomType.TBoxAxiomTypes)).forEach(ontologies.overApproximationOntology::add);
        //manager.addAxioms(ontologies.overApproximationOntology, ontologies.module);
        manager.addAxioms(ontologies.overApproximationOntology, ontologies.abox2);


        return ontologies;
    }

    /**
     * Implements Step P1' from the paper (supplemental material)
     */
    private void makeABoxConsistent(ContrastiveExplanationProblem problem, Ontologies ontologies) throws OWLOntologyCreationException {

        // we first need to create the ontology to be repaired

        Set<OWLAxiom> tbox = ontologies.module
                .stream()
                .filter(x -> x.isOfType(AxiomType.TBoxAndRBoxAxiomTypes))
                .collect(Collectors.toSet());

        OWLOntology ontology = instantiateFoils(ontologies.abox2,manager);
        ontology.addAxioms(tbox);

        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);

        if(reasoner.isConsistent()) {
            System.out.println("Nothing to fix!");
            return;
        }

        MultiMap<OWLNamedIndividual, OWLNamedIndividual> partners = new MultiMap<>();
        MultiMap<OWLNamedIndividual, OWLNamedIndividual> partnersInverse = new MultiMap<>();
        Set<OWLNamedIndividual> rangeIgnore = new HashSet<>();
        Set<OWLNamedIndividual> pairsSaveToRemove = new HashSet<>();

        // The mapping assocPartner and its inverse are used to check the conditions of Lemma 9+10 from the paper.
        // in particular, it is a superset of the safe vectors,
        Map<OWLNamedIndividual,OWLNamedIndividual> assocPartnerInv = new HashMap<>();
        assocPartnerInv.put(problem.getFoil(),problem.getFact());
        Set<OWLNamedIndividual> usedFirst = new HashSet<>();
        usedFirst.add(problem.getFact());

        Map<OWLNamedIndividual,OWLNamedIndividual> assocPartner = new HashMap<>();
        assocPartner.put(problem.getFact(),problem.getFoil());

        individualGenerator.getMappedPairs().forEach(pair -> {
           partners.add(pair.getKey(),pair.getValue());
           partnersInverse.add(pair.getValue(), pair.getKey());
           if(!assocPartner.containsKey(pair.getKey()) && !assocPartnerInv.containsKey(pair.getValue())){
               assocPartner.put(pair.getKey(),pair.getValue());
               assocPartnerInv.put(pair.getValue(),pair.getKey());
           }
        });

        if(assocPartner.keySet().size()!=partners.keys().size())
            throw new AssertionError("1:1 mapping not possible!");

        // lower bound contains now a set of axioms, based on Lemma 9+10, that should stay in the ABox if we want to
        // preserve the entailment of the fact
        Set<OWLAxiom> lowerBound =
        ontologies.module
                .stream()
                .filter(x -> x.isOfType(AxiomType.CLASS_ASSERTION, AxiomType.OBJECT_PROPERTY_ASSERTION))
                .filter(x -> x.individualsInSignature().allMatch(assocPartner::containsKey))
                .map( x-> {
                    if(x instanceof OWLClassAssertionAxiom) {
                        OWLClassAssertionAxiom ax = (OWLClassAssertionAxiom) x;
                        return factory.getOWLClassAssertionAxiom(ax.getClassExpression(),assocPartner.get(ax.getIndividual()));
                    } else {
                        OWLObjectPropertyAssertionAxiom ax = (OWLObjectPropertyAssertionAxiom) x;
                        return factory.getOWLObjectPropertyAssertionAxiom(ax.getProperty(),assocPartner.get(ax.getSubject()), assocPartner.get(ax.getObject()));
                    }
                })
                .collect(Collectors.toSet());
        int differenceInRange = partnersInverse.keys().size() - partners.keys().size();

        if(differenceInRange<0)
            throw new AssertionError("Less individuals in target!");

        Set<OWLAxiom> staticAxioms = new HashSet<>();
        staticAxioms.addAll(tbox);
        staticAxioms.addAll(lowerBound);

        // We now apply the basic repair algorithm as described in the paper, making sure we never remove axioms from
        // the lower bound
        while(!reasoner.isConsistent()) {

            MyBlackBoxExplanation expl = new MyBlackBoxExplanation(ontology, reasonerFactory, reasonerFactory.createReasoner(ontology));
            expl.setStaticPart(staticAxioms);
            Set<OWLAxiom> exp = expl.getExplanation(factory.getOWLThing());

            exp.removeAll(tbox);
            exp.removeAll(lowerBound);

            OWLAxiom remove = exp.iterator().next();

            if(remove instanceof OWLClassAssertionAxiom){
                OWLClassAssertionAxiom ax = (OWLClassAssertionAxiom) remove;
                OWLNamedIndividual i2 = (OWLNamedIndividual) ax.getIndividual();
                partnersInverse.get(i2).forEach(i1 -> {
                    OWLClassAssertionAxiom cl = factory.getOWLClassAssertionAxiom(ax.getClassExpression(), individualGenerator.getIndividualForPair(i1,i2));
                    ontologies.abox2.remove(cl);
                });
            } else if(remove instanceof OWLObjectPropertyAssertionAxiom){
                OWLObjectPropertyAssertionAxiom ax = (OWLObjectPropertyAssertionAxiom) remove;
                OWLNamedIndividual a2 = (OWLNamedIndividual) ax.getSubject();
                OWLNamedIndividual b2 = (OWLNamedIndividual) ax.getObject();
                partnersInverse.get(a2).forEach(a1 -> {
                    partnersInverse.get(b2).forEach(b1 -> {
                        OWLObjectPropertyAssertionAxiom p =
                                factory.getOWLObjectPropertyAssertionAxiom(
                                        ax.getProperty(),
                                        individualGenerator.getIndividualForPair(a1,a2),
                                        individualGenerator.getIndividualForPair(b1,b2));
                        ontologies.abox2.remove(p);
                    });

                });
            }
            ontology.remove(remove);
            reasoner.flush();
        }
        // For debugging purposes, we track situations where the entailment got lost, which shouldn't be possible by
        // the theoretical results from the paper
        if (!reasoner.isEntailed(factory.getOWLClassAssertionAxiom(problem.getOwlClassExpression(), problem.getFoil()))) {
            throw new AssertionError("Fixing problem destroyed entailment!");
        } else
            return;
    }
    /**
     * Step P4': minimize the conflict set
     */
    private boolean minimizeConflict(Ontologies ontologies,
                                        ContrastiveExplanationProblem problem,
                                        OWLOntology foilOntology) {
        OWLOntology ontology = foilOntology;
        OWLReasoner reasoner = reasonerFactory.createReasoner(foilOntology);
        if(reasoner.isConsistent()){
            if(!reasoner.isEntailed(factory.getOWLClassAssertionAxiom(problem.getOwlClassExpression(), problem.getFoil()))) {
                return false; // need to backtrack
            } else
                return true;
        } else {
            MyBlackBoxExplanation expl = new MyBlackBoxExplanation(ontology, reasonerFactory, reasonerFactory.createReasoner(ontology));
            Set<OWLAxiom> exp = expl.getExplanation(factory.getOWLThing());
            Set<OWLAxiom> fromOnt = new HashSet<>(exp);
            fromOnt.retainAll(ontologies.module);
            Set<OWLAxiom> fresh = new HashSet<>(exp);
            fresh.removeAll(ontologies.module);

            // try to remove fresh ones first
            for(OWLAxiom axiom:fresh){
                foilOntology.removeAxiom(axiom);
                boolean success = minimizeConflict(ontologies,problem,foilOntology);
                if(!success){
                    foilOntology.addAxiom(axiom);
                } else
                    return true;
            }
            // Otherwise build conflict set
            for(OWLAxiom axiom:fromOnt){
                if(axiom.isOfType(AxiomType.ABoxAxiomTypes)) {
                    foilOntology.removeAxiom(axiom);
                    boolean success = minimizeConflict(ontologies, problem, foilOntology);
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
    /**
     * Step P2' and P3' from the paper
     */
    public ContrastiveExplanation minimizeToExplanation(ContrastiveExplanationProblem problem, Ontologies ontologies) throws OWLOntologyCreationException {

        OWLNamedIndividual combinedIndividual = individualGenerator.getIndividualForPair(problem.getFact(),problem.getFoil());

        // Step 3: Create "specialAxiom"
        OWLAxiom specialAxiom = factory.getOWLClassAssertionAxiom(problem.getOwlClassExpression(), combinedIndividual);
        Set<OWLAxiom> flexibleSet = new HashSet<>();
        flexibleSet.addAll(ontologies.abox2);
        flexibleSet.removeAll(ontologies.abox3);

        System.out.println("OverApproximated ontology size: "+ontologies.overApproximationOntology.getAxiomCount());
        System.out.println("Flexible: "+flexibleSet.size());

        // Step 5: Compute first explanation
        Set<OWLAxiom> different = computeExplanation(ontologies.overApproximationOntology, specialAxiom, flexibleSet);

        System.out.println("Computed first justification");

        if(different.isEmpty()) {
            try {
                manager.saveOntology(ontologies.overApproximationOntology, new FileOutputStream(new File("debug.owl")));
                OWLOntology o = manager.createOntology();
                o.addAxioms(flexibleSet);
                manager.saveOntology(ontologies.overApproximationOntology, new FileOutputStream(new File("debug-flexible.owl")));
            } catch (OWLOntologyStorageException e) {
                throw new RuntimeException(e);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            System.out.println(specialAxiom);
            throw new AssertionError("justification among differences empty - shouldn't be possible!");
        }
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

        // Step 8: Compute conflicts
        ontologies.conflictSet = computeConflictSet(common,different,problem.getOntology());

        // Step 9: extract mappings and conflict

        ContrastiveExplanation result = extractMappings(common,different, ontologies.conflictSet);
        return result;
    }

    private Set<OWLAxiom> computeConflictSet(Set<OWLAxiom> common, Set<OWLAxiom> different, OWLOntology ontology) throws OWLOntologyCreationException {
        Set<OWLAxiom> explAxioms = new HashSet<>();
        explAxioms.addAll(common);
        explAxioms.addAll(different);

        OWLOntology extOnt = instantiateFoils(explAxioms, manager);

        Set<OWLAxiom> fixed = ontology.getTBoxAxioms(Imports.INCLUDED);
        fixed.addAll(extOnt.getAxioms());
        Set<OWLAxiom> flexible = ontology.getABoxAxioms(Imports.INCLUDED);
        extOnt.addAxioms(fixed);
        extOnt.addAxioms(flexible);

        OWLReasoner reasoner = reasonerFactory.createReasoner(extOnt);

        Set<OWLAxiom> conflictSet = new HashSet<>();
        while(!reasoner.isConsistent()){
            MyBlackBoxExplanation expl =
                    new MyBlackBoxExplanation(extOnt, reasonerFactory, reasoner);
            Set<OWLAxiom> just = expl.getExplanation(factory.getOWLThing());
            just.removeAll(fixed);
            if(just.isEmpty())
                throw new AssertionError("Shouldn't be possible!");
            OWLAxiom next = just.iterator().next();
            conflictSet.add(next);
            extOnt.remove(next);
            reasoner.flush();
        }
        return conflictSet;
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
        MyBlackBoxExplanation expl = new MyBlackBoxExplanation(ontology, reasonerFactory, reasonerFactory.createReasoner(ontology));
        expl.setStaticPart(fixedSet);
        Set<OWLAxiom> result = expl.getExplanation(asUnsat(axiom));
        result.retainAll(flexibleSet);
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

