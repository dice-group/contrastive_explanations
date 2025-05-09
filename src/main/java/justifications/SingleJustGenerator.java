/***********************************************************
 *  Copyright (C) 2022 - 2026                              *
 *  Jieying Chen (jieyingc@ifi.uio.no)                     *
 *                                                         *
 *                                                         *
 *  This program is free software; you can redistribute    *
 *  it and/or modify it under the terms of the GNU         *
 *  General Public License as published by the Free        *
 *  Software Foundation; either version 3 of the License,  *
 *  or (at your option) any later version.                 *
 *                                                         *
 *  This program is distributed in the hope that it will   *
 *  be useful, but WITHOUT ANY WARRANTY; without even      *
 *  the implied warranty of MERCHANTABILITY or FITNESS     *
 *  FOR A PARTICULAR PURPOSE.  See the GNU General Public  *
 *  License for more details.                              *
 *                                                         *
 *  You should have received a copy of the GNU General     *
 *  Public License along with this program; if not, see    *
 *  <http://www.gnu.org/licenses/>.                        *
 ***********************************************************/
package justifications;

import com.clarkparsia.owlapi.explanation.BlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;
import com.clarkparsia.owlapi.explanation.util.DefinitionTracker;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.util.*;

public class SingleJustGenerator {
    private static  OWLOntologyManager owlOntologyManager;
    private static OWLOntology ontology;
    private static OWLReasoner reasoner;
    private static OWLReasonerFactory reasonerFactory;
    private static OWLDataFactory dataFactory;
    public static Set<OWLAxiom> core = new HashSet<>();

    public SingleJustGenerator(OWLOntology ontology) {
        this.ontology = ontology;
        this.reasonerFactory =  new Reasoner.ReasonerFactory();
        this.reasoner = this.getReasonerFactory().createNonBufferingReasoner(this.getOntology());
        this.owlOntologyManager = ontology.getOWLOntologyManager();
        this.dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
    }
    public SingleJustGenerator(Set<OWLAxiom> axioms) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        this.ontology = manager.createOntology(axioms);
        this.reasonerFactory =  new Reasoner.ReasonerFactory();
        this.reasoner = this.getReasonerFactory().createNonBufferingReasoner(this.getOntology());
        this.owlOntologyManager = manager;
        this.dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
    }




    public SingleJustGenerator(OWLOntology ontology, OWLReasonerFactory reasonerFactory, OWLReasoner reasoner) {
        this.ontology = ontology;
        this.reasonerFactory = reasonerFactory;
        this.reasoner = reasoner;
        this.owlOntologyManager = ontology.getOWLOntologyManager();
        this.dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
    }

    public OWLOntologyManager getOntologyManager() {
        return this.owlOntologyManager;
    }

    public OWLOntology getOntology() {
        return this.ontology;
    }

    public OWLReasoner getReasoner() {
        return this.reasoner;
    }

    public OWLReasonerFactory getReasonerFactory() {
        return this.reasonerFactory;
    }

    public OWLDataFactory getDataFactory() {
        return this.dataFactory;
    }

    public void dispose() {
        core.clear();
        this.getReasoner().dispose();
        this.reasoner.dispose();
        this.getOntologyManager().removeOntology(this.getOntology());
    }

    public static Set<OWLAxiom> computeCoreofAllJustifications(OWLSubClassOfAxiom axiom) {
//        if(!reasoner.isEntailed(axiom)){
//            System.out.println("!!! The following conclusion is not entailed by the ontology: "+axiom );
//            return Collections.emptySet();
//        }

        Set<OWLAxiom> toReturn = ontology.getAxioms();
//        System.out.println(ontology.getAxioms().size());
        for(OWLAxiom a: ontology.getAxioms()){
            owlOntologyManager.applyChange(new RemoveAxiom(ontology, a));
            reasoner.flush();
//            System.out.println(a);
//            System.out.println(reasoner.isEntailed(axiom));
            if(reasoner.isEntailed(axiom)){
                toReturn.remove(a);
            }
            owlOntologyManager.applyChange(new AddAxiom(ontology, a));
//            reasoner.flush();
        }
//        System.out.println(toReturn.size());
        if(toReturn.size()<1){
            return Collections.emptySet();
        }
        return toReturn;
    }

    public static OWLClassExpression get_C_or_NotD(OWLDataFactory dataFactory, OWLSubClassOfAxiom ax) {
        OWLClassExpression lhs = ax.getSubClass();
        OWLClassExpression rhs = ax.getSuperClass();
        OWLClassExpression negRHS = dataFactory.getOWLObjectComplementOf(rhs);
        OWLClassExpression toReturn = dataFactory.getOWLObjectIntersectionOf(lhs,negRHS);

        return toReturn;
    }

    /*public static Set<OWLAxiom> computeSingleJustificationByAD(OWLSubClassOfAxiom axiom, AtomicDecomposition<OWLAxiom, OWLAxiom> ad){

        Configuration configuration=new Configuration();
        configuration.throwInconsistentOntologyException=false;
        OWLReasoner reasoner=reasonerFactory.createReasoner(ontology, configuration);

        if(!reasoner.isEntailed(axiom)){
            System.out.println("!!! The following conclusion is not entailed by the ontology: "+axiom );
            return Collections.emptySet();
        }

        HashSet<OWLAxiom> toReturn = new HashSet<>(ontology.getAxioms());
//        HashSet<OWLAxiom> copy = new HashSet<>(ontology.getAxioms());

        //do atomic decomposition

        HashMap<Integer, LinkedList<OWLAxiom>> atomMap = getAtomChainMap(ad);
        LinkedList<LinkedList<OWLAxiom>> bubbleList = getBigAtomBubbleList(atomMap,ad);

        System.out.println("----root:"+getRootAtomChainMap(ad).keySet().size());
        for(LinkedList<OWLAxiom>a: bubbleList){
            System.out.println(a.size());
        }

        LinkedList<OWLAxiom> axioms = new LinkedList<OWLAxiom>(ontology.getAxioms());

        int nrReasonerCalls = 0;
        Set<OWLAxiom> checkedAxiom = new HashSet<>();


        while (!bubbleList.isEmpty()) {

            List<OWLAxiom> bubble = bubbleList.poll();
            bubble.removeAll(checkedAxiom);
            if(bubble.size()==0){
                continue;
            }
            toReturn.removeAll(bubble);

            for(OWLAxiom a: bubble){
                owlOntologyManager.applyChange(new RemoveAxiom(ontology, a));
            }
            reasoner.flush();
            if(reasoner.isEntailed(axiom)){
                toReturn.removeAll(bubble);
                checkedAxiom.addAll(bubble);
            }else{
                for(OWLAxiom a: bubble){
                    owlOntologyManager.applyChange(new AddAxiom(ontology, a));
                }
                toReturn.addAll(bubble);


                Integer index = checkIndex(bubble,ad,atomMap);
                if(index == null){
                    System.out.println("!!!cannot find original atom index of "+bubble);
                }
                System.out.println("root:"+ad.get_atom(index));
                bubbleList.addFirst(new LinkedList<>(ad.get_atom(index)));
                System.out.println(" ad.get_successor_indices(index)"+ ad.get_successor_indices(index));
                for(Integer i: ad.get_successor_indices(index)){

                    bubbleList.addFirst(atomMap.get(i));
                }
//  split bubble into half
//                final int thebubbleSize = bubble.size();
//                if (thebubbleSize > 1) {
//                    int halfBubbleSize = thebubbleSize / 2;
//                    LinkedList<OWLAxiom> newLeftBubble = new LinkedList<OWLAxiom>();
//                    LinkedList<OWLAxiom> newRightBubble = new LinkedList<OWLAxiom>(bubble);
//                    for (int i = 0; i < halfBubbleSize; ++i) {
//                        newLeftBubble.add(newRightBubble.poll());
//                    }
//                    bubbleList.addFirst(newLeftBubble);
//                    bubbleList.addFirst(newRightBubble);
//                }

            }
            ++nrReasonerCalls;
//            reasoner.flush();
        }
        reasoner.dispose();

//        System.out.println("- Number of reasoner call: "+nrReasonerCalls);
        return toReturn;
    }

    private static Integer checkIndex(List<OWLAxiom> bubble, AtomicDecomposition<OWLAxiom, OWLAxiom> ad, HashMap<Integer, LinkedList<OWLAxiom>> atomMap) {
        Integer toReturn;
        for(Integer i: atomMap.keySet()){
            atomMap.get(i).equals(bubble);
            return i;
        }
        return null;
    }

    private static LinkedList<LinkedList<OWLAxiom>> getBigAtomBubbleList(HashMap<Integer, LinkedList<OWLAxiom>> bubbleMap,AtomicDecomposition<OWLAxiom, OWLAxiom> ad) {

        LinkedList<LinkedList<OWLAxiom>> toReturn = new LinkedList();

        for(Integer i: bubbleMap.keySet()){
            if(ad.get_root_atom_indices().contains(i)){
                toReturn.addFirst(bubbleMap.get(i));
            }
        }
        return toReturn;
    }

    private static HashMap<Integer, LinkedList<OWLAxiom>> getRootAtomChainMap(AtomicDecomposition<OWLAxiom, OWLAxiom> ad) {

        HashMap<Integer, LinkedList<OWLAxiom>> toReturn = new HashMap<>();

        for(Integer rootAtomIndex: ad.get_root_atom_indices()){
            System.out.println("atom("+rootAtomIndex+").size() = "+ad.get_atom(rootAtomIndex).size());
            LinkedList<OWLAxiom> atomChain = new LinkedList<>(ad.get_atom(rootAtomIndex));
            for(Integer succ: ad.get_decentant_indices(rootAtomIndex)){
                atomChain.addAll(ad.get_atom(succ));
            }
            toReturn.put(rootAtomIndex,atomChain);
        }
        return toReturn;
    }
    private static HashMap<Integer, LinkedList<OWLAxiom>> getAtomChainMap(AtomicDecomposition<OWLAxiom, OWLAxiom> ad) {

        HashMap<Integer, LinkedList<OWLAxiom>> toReturn = new HashMap<>();

        for(Integer index = 0; index<ad.get_number_of_atoms()-1;index++){
            System.out.println("atom("+index+").size() = "+ad.get_atom(index).size());
            LinkedList<OWLAxiom> atomChain = new LinkedList<>(ad.get_atom(index));
            for(Integer succ: ad.get_decentant_indices(index)){
                atomChain.addAll(ad.get_atom(succ));
            }
//            System.out.println("--size of atoms:"+atomChain.size());
            toReturn.put(index,atomChain);
        }
        return toReturn;
    }*/


    public static Set<OWLAxiom> computeSingleJustification(OWLSubClassOfAxiom axiom,  int bubbleSize) {
        return computeSingleJustificationWithCore(axiom,Collections.emptySet(),bubbleSize);
    }

    public static Set<OWLAxiom> computeSingleJustification(OWLSubClassOfAxiom axiom) {
        Set<OWLAxiom> toReturn = ontology.getAxioms();
//        System.out.println(ontology.getAxioms().size());
        for(OWLAxiom a: ontology.getAxioms()){
            owlOntologyManager.applyChange(new RemoveAxiom(ontology, a));
            reasoner.flush();
//            System.out.println(a);
//            System.out.println(reasoner.isEntailed(axiom));
            if(reasoner.isEntailed(axiom)){
                toReturn.remove(a);
            }
            owlOntologyManager.applyChange(new AddAxiom(ontology, a));
//            reasoner.flush();
        }
//        System.out.println(toReturn.size());
        if(toReturn.size()<1){
            return Collections.emptySet();
        }
        return toReturn;
    }



    public static Set<OWLAxiom> computeSingleJustificationWithCore(OWLSubClassOfAxiom axiom, Set<OWLAxiom> core, int bubbleSize) {
        Configuration configuration=new Configuration();
        configuration.throwInconsistentOntologyException=false;
        OWLReasoner reasoner=reasonerFactory.createReasoner(ontology, configuration);

        if(!reasoner.isEntailed(axiom)){
            System.out.println("!!! The following conclusion is not entailed by the ontology: "+axiom );
            return Collections.emptySet();
        }

        HashSet<OWLAxiom> toReturn = new HashSet<>(ontology.getAxioms());
//        HashSet<OWLAxiom> copy = new HashSet<>(ontology.getAxioms());


        LinkedList<LinkedList<OWLAxiom>> bubbleList = new LinkedList<LinkedList<OWLAxiom>>();
        LinkedList<OWLAxiom> axioms = new LinkedList<OWLAxiom>(ontology.getAxioms());


        int nrReasonerCalls = 0;
        {
            LinkedList<OWLAxiom> bubble = null;

            while (axioms.size() > 0) {
                OWLAxiom ax = axioms.removeFirst();

                if(core.contains(ax)) {
                    continue;
                }
                if(bubble == null) {
                    bubble = new LinkedList<OWLAxiom>();
                }
                bubble.add(ax);
                if(bubble.size() >= bubbleSize) {
                    bubbleList.add(bubble);
                    bubble = null;
                }
            }

            if(bubble != null) {
                bubbleList.add(bubble);
            }
        }


        while (!bubbleList.isEmpty()) {

            List<OWLAxiom> bubble = bubbleList.poll();
            toReturn.removeAll(bubble);

            for(OWLAxiom a: bubble){
                owlOntologyManager.applyChange(new RemoveAxiom(ontology, a));
            }
            reasoner.flush();
            if(reasoner.isEntailed(axiom)){
                toReturn.removeAll(bubble);
            }else{
                for(OWLAxiom a: bubble){
                    owlOntologyManager.applyChange(new AddAxiom(ontology, a));
                }
                toReturn.addAll(bubble);
                final int thebubbleSize = bubble.size();
                if (thebubbleSize > 1) {
                    int halfBubbleSize = thebubbleSize / 2;
                    LinkedList<OWLAxiom> newLeftBubble = new LinkedList<OWLAxiom>();
                    LinkedList<OWLAxiom> newRightBubble = new LinkedList<OWLAxiom>(bubble);
                    for (int i = 0; i < halfBubbleSize; ++i) {
                        newLeftBubble.add(newRightBubble.poll());
                    }
                    bubbleList.addFirst(newLeftBubble);
                    bubbleList.addFirst(newRightBubble);
                }
            }
            ++nrReasonerCalls;
//            reasoner.flush();
        }
        reasoner.dispose();

//        System.out.println("- Number of reasoner call: "+nrReasonerCalls);
        return toReturn;
    }
}
