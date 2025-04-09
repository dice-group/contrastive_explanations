package nl.vu.kai.contrastive.experiments;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.clarkparsia.owlapi.explanation.MyBlackBoxExplanation;
import nl.vu.kai.contrastive.ContrastiveExplanation;
import nl.vu.kai.contrastive.ContrastiveExplanationGenerator;
import nl.vu.kai.contrastive.ContrastiveExplanationProblem;
import nl.vu.kai.contrastive.experiments.helpers.FoilCandidateFinder;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.LoggerFactory;
import tools.Util;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ExperimenterWithClasses {

    public static int MAX_ONT_SIZE=10000;

    public static enum ReasonerChoice { ELK, HERMIT }

    public static ReasonerChoice reasoner = ReasonerChoice.ELK;

    public static void main(String[] args) throws OWLOntologyCreationException {
        boolean conflictMinimal=false;
        if(args.length<2){
            System.out.println("Usage: ");
            System.out.println(ExperimenterWithClasses.class+ " ONTOLOGY NUMBER_OF_REPITITIONS [ELK|HERMIT] [conflict-minimal]");
            System.exit(0);
        }
        if(args.length>=3){
            if(args[2]=="HERMIT")
                reasoner = ReasonerChoice.HERMIT;
            else if(args[2]!="ELK")
                throw new IllegalArgumentException("Unexpected reasoner choice: "+args[2]);
        }
        if(args.length==4){
            if(args[3]!="conflict-minimal")
                throw new IllegalArgumentException("Expected 'conflict-minimal' as 5th argument, got "+args[3]);
            conflictMinimal=true;

        }

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger("org.semanticweb.owlapi");
        logger.setLevel(Level.OFF);
        loggerContext.getLogger("org.semanticweb.elk").setLevel(Level.OFF);
        loggerContext.getLogger("com.clarkparsia.owlapi").setLevel(Level.OFF);
        loggerContext.getLogger("uk.ac.manchester.cs.owlapi").setLevel(Level.OFF);

        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();

        int maxIterations = Integer.parseInt(args[1]);

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();

        System.out.println("Parsing ontology...");
        OWLOntology ont = manager.loadOntologyFromOntologyDocument(new File(args[0]));

        if(ont.getAxiomCount()>MAX_ONT_SIZE){
            System.out.println("Ontology has more than "+MAX_ONT_SIZE+" axioms!");
            System.exit(0);
        }

        Set<OWLNamedIndividual> allIndividuals = ont.individualsInSignature().collect(Collectors.toSet());

        List<OWLClass> candidates = new ArrayList<>();
        Map<OWLClass, List<OWLNamedIndividual>> facts = new HashMap<>();
        Map<OWLClass, List<OWLNamedIndividual>> foils = new HashMap<>();

        int maxFoils = 0;
        int maxFacts = 0;

        OWLReasonerFactory reasonerFactory =
                reasoner==ReasonerChoice.HERMIT ?
                        new ReasonerFactory() :
                        new ElkReasonerFactory();

        System.out.println("Computing Instantiation...");

        OWLReasoner reasoner = reasonerFactory.createReasoner(ont);
        reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);

        System.out.println("Finding contrastive explanation problems...");

        Set<OWLAxiom> notABox = new HashSet<>(ont.getAxioms(Imports.INCLUDED));
        notABox.removeAll(ont.getABoxAxioms(Imports.INCLUDED));

        MyBlackBoxExplanation explainer = new MyBlackBoxExplanation(ont, reasonerFactory, reasoner);
        explainer.setStaticPart(notABox);

        for(OWLClass clazz : ont.classesInSignature().collect(Collectors.toSet()))  {
            Set<OWLNamedIndividual> pos = reasoner.getInstances(clazz).getFlattened();
            Set<OWLNamedIndividual> neg = new HashSet<>(allIndividuals);
            neg.removeAll(pos);

            for(OWLNamedIndividual ind:new LinkedList<>(pos)){
                if(ont.classAssertionAxioms(ind)
                        .map(x -> x.getClassExpression())
                        .anyMatch(clazz::equals))
                    pos.remove(ind);
                else {
                    Set<OWLAxiom> explanation = explainer.getExplanation(
                            factory.getOWLObjectIntersectionOf(
                                    factory.getOWLObjectOneOf(ind),
                                    factory.getOWLObjectComplementOf(clazz)
                            ));
                    explanation.removeAll(notABox);
                    if (explanation.size() < 2) {
                        System.out.println("ABox justification too simple: " + clazz + ", " + ind);
                        pos.remove(ind);
                    } else
                        System.out.println("Good: " + clazz + ", " + ind);
                }
            }

            if(!pos.isEmpty() && !neg.isEmpty()){
                candidates.add(clazz);
                maxFacts = Math.max(maxFacts, pos.size());
                maxFoils = Math.max(maxFoils, neg.size());
                facts.put(clazz, new ArrayList<>(pos));
                foils.put(clazz, new ArrayList<>(neg));
            }
        }

        System.out.println("Ontology: "+args[0]);
        System.out.println("Candidate classes: "+candidates.size());
        System.out.println("Max facts per class: "+maxFacts);
        System.out.println("Max foils per class: "+maxFoils);

        Random random = new Random(0);

        //List<Integer> commonSizes = new LinkedList<>();
        //List<Integer> differenceSizes = new LinkedList<>();
        //List<Long> freshIndividuals = new LinkedList<>();

        if(candidates.isEmpty()){
            System.out.println("NO CONTRASTIVE EXPLANATION PROBLEMS!");
            System.exit(0);
        }

        System.out.println("STATS: common-size difference-size conflict-size num-fresh-individuals computation-time");

        FoilCandidateFinder foilCandidateFinder =
                new FoilCandidateFinder(ont, FoilCandidateFinder.Strategy.CommonClass);
        foilCandidateFinder.setReasoner(reasoner);

        for(int i = 0; i<maxIterations; i++){
            OWLClass cl = candidates.get(random.nextInt(candidates.size()));
            List<OWLNamedIndividual> factC = facts.get(cl);
            List<OWLNamedIndividual> foilC = foils.get(cl);
            OWLNamedIndividual fact = factC.get(random.nextInt(factC.size()));
            foilC.retainAll(foilCandidateFinder.foilCandidates(fact).collect(Collectors.toSet()));

            if(!foilC.isEmpty()) {
                OWLNamedIndividual foil = Util.randomItem(
                        foilCandidateFinder.foilCandidates(fact)
                                .filter(foilC::contains),
                        random);
                //foilC.get(random.nextInt(foilC.size()));
                ContrastiveExplanationProblem cep = new ContrastiveExplanationProblem(ont, cl, fact, foil);
                System.out.println("CEP: " + cep.toString(renderer));
                long startTime = System.currentTimeMillis();
                ContrastiveExplanationGenerator gen = new ContrastiveExplanationGenerator(manager.getOWLDataFactory());
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
                System.out.println("STATS: " + commonSize + " " + differenceSize + " " + " " + conflictSize + " " + freshIndividuals + " " + duration);
            } else {
                System.out.println("Skipped -- no foil candidate: "+cl+" "+fact);
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
