package nl.vu.kai.contrastive.experiments;

import nl.vu.kai.contrastive.ContrastiveExplanation;
import nl.vu.kai.contrastive.ContrastiveExplanationGenerator;
import nl.vu.kai.contrastive.ContrastiveExplanationProblem;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Experimenter {
    public static void main(String[] args) throws OWLOntologyCreationException {
        if(args.length!=2){
            System.out.println("Usage: ");
            System.out.println(Experimenter.class+ " ONTOLOGY NUMBER_OF_REPITITIONS");
            System.exit(0);
        }

        ManchesterOWLSyntaxOWLObjectRendererImpl renderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();

        int maxIterations = Integer.parseInt(args[1]);

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        OWLOntology ont = manager.loadOntologyFromOntologyDocument(new File(args[0]));

        Set<OWLNamedIndividual> allIndividuals = ont.individualsInSignature().collect(Collectors.toSet());

        List<OWLClass> candidates = new ArrayList<>();
        Map<OWLClass, List<OWLNamedIndividual>> facts = new HashMap<>();
        Map<OWLClass, List<OWLNamedIndividual>> foils = new HashMap<>();

        int maxFoils = 0;
        int maxFacts = 0;

        OWLReasoner reasoner = new ReasonerFactory().createReasoner(ont);
        reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
        for(OWLClass clazz : ont.classesInSignature().collect(Collectors.toSet()))  {
            Set<OWLNamedIndividual> pos = reasoner.getInstances(clazz).getFlattened();
            Set<OWLNamedIndividual> neg = new HashSet<>(allIndividuals);
            neg.removeAll(pos);
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

        System.out.println("STATS: common-size difference-size num-fresh-individuals computation-time");

        for(int i = 0; i<maxIterations; i++){
            OWLClass cl = candidates.get(random.nextInt(candidates.size()));
            List<OWLNamedIndividual> factC = facts.get(cl);
            List<OWLNamedIndividual> foilC = foils.get(cl);
            OWLNamedIndividual fact = factC.get(random.nextInt(factC.size()));
            OWLNamedIndividual foil = foilC.get(random.nextInt(foilC.size()));
            ContrastiveExplanationProblem cep = new ContrastiveExplanationProblem(ont,cl,fact,foil);
            System.out.println("CEP: "+cep.toString(renderer));
            long startTime = System.currentTimeMillis();
            ContrastiveExplanationGenerator gen = new ContrastiveExplanationGenerator(manager.getOWLDataFactory());
            ContrastiveExplanation ce = gen.computeExplanation(cep);
            System.out.println("CE: "+ce.toString(renderer));
            long duration = System.currentTimeMillis()-startTime;
            int commonSize = ce.getCommon().size();
            int differenceSize = ce.getDifferent().size();
            long freshIndividuals = ce.getFoilMapping().values().stream().filter(allIndividuals::contains).count();
            System.out.println("STATS: "+commonSize+" "+differenceSize+" "+freshIndividuals+" "+duration);
        }


    }
}
