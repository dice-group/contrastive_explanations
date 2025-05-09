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

import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import java.util.*;


public class AllJustificationGenerator {

    private static OWLOntologyManager owlOntologyManager;
    private static OWLOntology ontology;
    private static OWLReasoner reasoner;
    private static OWLReasonerFactory reasonerFactory;
    private static OWLDataFactory dataFactory;

    public static Set<OWLAxiom> core = new HashSet<>();
    public static Set<Set<OWLAxiom>> allJustifications = new HashSet<Set<OWLAxiom>>();
    public static Set<OWLAxiom> union_allJustifications = new HashSet<OWLAxiom>();
    private static Set<List<OWLAxiom>> HS = new HashSet<List<OWLAxiom>>();

    public AllJustificationGenerator(OWLOntology ontology, OWLReasonerFactory reasonerFactory, OWLReasoner reasoner, Set<OWLAxiom> preComputeCore) {
        this.ontology = ontology;
        this.reasonerFactory = reasonerFactory;
        this.reasoner = reasoner;
        this.owlOntologyManager = ontology.getOWLOntologyManager();
        this.dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
        this.core = preComputeCore;
    }

    public AllJustificationGenerator(OWLOntology ontology, OWLReasonerFactory reasonerFactory, OWLReasoner reasoner) {
        this.ontology = ontology;
        this.reasonerFactory = reasonerFactory;
        this.reasoner = reasoner;
        this.owlOntologyManager = ontology.getOWLOntologyManager();
        this.dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
    }

    public AllJustificationGenerator(Set<OWLAxiom> axioms, Set<OWLAxiom> preComputeCore) throws OWLOntologyCreationException {
        this.owlOntologyManager = OWLManager.createOWLOntologyManager();
        this.ontology = this.owlOntologyManager.createOntology(axioms);
        this.reasonerFactory = new Reasoner.ReasonerFactory();
        Configuration configuration=new Configuration();
        configuration.throwInconsistentOntologyException=false;
        this.reasoner=this.reasonerFactory.createReasoner(this.ontology, configuration);
        this.dataFactory = this.ontology.getOWLOntologyManager().getOWLDataFactory();
        this.core = preComputeCore;
    }



    private static List<OWLAxiom> copyList(List<OWLAxiom> path) {

        List<OWLAxiom> toReturn = new LinkedList<OWLAxiom>();
        Iterator<OWLAxiom> it = path.iterator();
        while(it.hasNext()) {
            toReturn.add(it.next());
        }

        return toReturn;
    }

    private static boolean isPrixPath(List<OWLAxiom> path)
    {
        for (List<OWLAxiom> oldPath : HS) {
            if(path.equals(oldPath)) {
                continue;
            }
            if(areAxiomsContained(oldPath, path)) {
//                if(areAxiomsContained(path, oldPath)) {
                if(oldPath.size() == path.size()){
                    return true;
                }
                else if(canJustificationBeReused(oldPath) == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Set<OWLAxiom> canJustificationBeReused(List<OWLAxiom> path)
    {
        for (Set<OWLAxiom> min : allJustifications) {
            if(isSetIntersectionEmpty(min, path)) {
                return min;
            }
        }
        return null;
    }


    public static <T> boolean isSetIntersectionEmpty(Collection<T> s1, Collection<T> s2)
    {
        if(s1.size() <= s2.size()) {
            for(T t : s1) {
                if(s2.contains(t)) {
                    return false;
                }
            }
        }
        else {
            for(T t : s2) {
                if(s1.contains(t)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean areAxiomsContained(List<OWLAxiom> axioms1, List<OWLAxiom> axioms2)
    {
        if(axioms1.size() == 0) {
            return true;
        }
        Set<OWLAxiom> set2 = new HashSet<OWLAxiom>(axioms2);
        for(OWLAxiom ax : axioms1) {
            if(!set2.contains(ax)) {
                return false;
            }
        }
        return true;
    }

    public void computeAllJustifications(OWLSubClassOfAxiom ax, int bubbleSize,Boolean computeCore) throws OWLOntologyCreationException {
        if(computeCore){
            OWLOntology newOntology = this.getOntologyManager().createOntology(this.getOntology().getAxioms());
            SingleJustGenerator generator = new SingleJustGenerator(newOntology);
            this.core = generator.computeCoreofAllJustifications(ax);
            generator.dispose();
        }
        else {
            this.core = Collections.emptySet();
        }
            this.computeAllJustifications(ax,Collections.emptyList(),bubbleSize);
    }

    public void computeUnionOfAllJustifications(OWLSubClassOfAxiom ax, int bubbleSize,Boolean computeCore) throws OWLOntologyCreationException {
        if(computeCore){
            OWLOntology newOntology = this.getOntologyManager().createOntology(this.getOntology().getAxioms());
            SingleJustGenerator generator = new SingleJustGenerator(newOntology);
            this.core = generator.computeCoreofAllJustifications(ax);
            generator.dispose();
        }
        else {
            this.core = Collections.emptySet();
        }
        this.computeUnionOfAllJustifications(ax,Collections.emptyList(),bubbleSize);
    }

    public void computeUnionOfAllJustifications(OWLSubClassOfAxiom ax, int bubbleSize) throws OWLOntologyCreationException {
        List<OWLAxiom> path = new ArrayList<>();
        computeUnionOfAllJustifications(ax,path,bubbleSize);
    }


    public void computeUnionOfAllJustifications(OWLSubClassOfAxiom ax, List<OWLAxiom> path, int bubbleSize) throws OWLOntologyCreationException {

//        System.out.println("------ontology size-----"+this.getOntology().getAxiomCount());
        if (isPrixPath(path)) {
            return;
        }

        // newly added
        if(union_allJustifications.containsAll(this.getOntology().getAxioms())){
            return;
        }



        Set<OWLAxiom> localityModuleMinusPathAxioms = new HashSet<OWLAxiom>(this.getOntology().getAxioms());
        localityModuleMinusPathAxioms.removeAll(path);



        OWLOntology localityModuleMinusPathAxiomsOntology = this.getOntologyManager().createOntology(localityModuleMinusPathAxioms);


        Configuration configuration=new Configuration();
        configuration.throwInconsistentOntologyException=false;
        OWLReasoner reasoner2=this.getReasonerFactory().createReasoner(localityModuleMinusPathAxiomsOntology, configuration);

        if(!reasoner2.isEntailed(ax)) {
//            this.getOntologyManager().removeOntology(localityModuleMinusPathAxiomsOntology);
//            reasoner2.dispose();
            return;
        }
        reasoner2.dispose();

        Set<OWLAxiom> singleJustification = null;

        singleJustification = canJustificationBeReused(path);

        if(singleJustification == null) {

            SingleJustGenerator s = new SingleJustGenerator(localityModuleMinusPathAxiomsOntology,this.getReasonerFactory(),this.getReasoner());
            singleJustification = s.computeSingleJustificationWithCore(ax,this.core,bubbleSize);

           // System.out.println(singleJustification);
            allJustifications.add(singleJustification);
            union_allJustifications.addAll(singleJustification); //add newly computed justification to the union
            this.getOntologyManager().removeOntology(localityModuleMinusPathAxiomsOntology);
            s.dispose();
            if (singleJustification.size() == this.core.size()) {
                //System.out.println("min.size() == core.size()");

                return;
            }
        }

        for(OWLAxiom a : singleJustification) {
            if(this.core.contains(a)) {
                continue;
            }
//            if(union_allJustifications.contains(a)){
//                continue;
//            }

            List<OWLAxiom> extendedPath = copyList(path);
            extendedPath.add(a);
            HS.add(extendedPath);

            this.computeUnionOfAllJustifications(ax, extendedPath, bubbleSize);
        }

        return;
    }

//    public void computeAllJustificationsByAD(OWLSubClassOfAxiom ax, List<OWLAxiom> path, AtomicDecomposition<OWLAxiom, OWLAxiom> ad) throws OWLOntologyCreationException {
//
////        System.out.println("------ontology size-----"+this.getOntology().getAxiomCount());
//        if (isPrixPath(path)) {
//            return;
//        }
//
//        Set<OWLAxiom> localityModuleMinusPathAxioms = new HashSet<OWLAxiom>(this.getOntology().getAxioms());
//        localityModuleMinusPathAxioms.removeAll(path);
//        OWLOntology localityModuleMinusPathAxiomsOntology = this.getOntologyManager().createOntology(localityModuleMinusPathAxioms);
//
//
//        Configuration configuration=new Configuration();
//        configuration.throwInconsistentOntologyException=false;
//        OWLReasoner reasoner2=this.getReasonerFactory().createReasoner(localityModuleMinusPathAxiomsOntology, configuration);
//
//        if(!reasoner2.isEntailed(ax)) {
//            this.getOntologyManager().removeOntology(localityModuleMinusPathAxiomsOntology);
//            reasoner2.dispose();
//            return;
//        }
//        reasoner2.dispose();
//
//        Set<OWLAxiom> singleJustification = null;
//
//        singleJustification = canJustificationBeReused(path);
//
//        if(singleJustification == null) {
//            SingleJustGenerator s = new SingleJustGenerator(localityModuleMinusPathAxiomsOntology,this.getReasonerFactory(),this.getReasoner());
//            singleJustification = s.computeSingleJustificationWithCore(ax,this.core);
//            allJustifications.add(singleJustification);
//            s.dispose();
//            if (singleJustification.size() == this.core.size()) {
//                //System.out.println("min.size() == core.size()");
//                this.getOntologyManager().removeOntology(localityModuleMinusPathAxiomsOntology);
//                return;
//            }
//        }
//
//        this.getOntologyManager().removeOntology(localityModuleMinusPathAxiomsOntology);
//
//        for(OWLAxiom a : singleJustification) {
//            if(this.core.contains(a)) {
//                continue;
//            }
//
//            List<OWLAxiom> extendedPath = copyList(path);
//            extendedPath.add(a);
//            HS.add(extendedPath);
//
//            this.computeAllJustificationsByAd(ax, extendedPath, bubbleSize);
//        }
//
//        return;
//    }


    public void computeAllJustifications(OWLSubClassOfAxiom ax, List<OWLAxiom> path, int bubbleSize) throws OWLOntologyCreationException {

//        System.out.println("------ontology size-----"+this.getOntology().getAxiomCount());
        if (isPrixPath(path)) {
            return;
        }

        Set<OWLAxiom> localityModuleMinusPathAxioms = new HashSet<OWLAxiom>(this.getOntology().getAxioms());
        localityModuleMinusPathAxioms.removeAll(path);
        OWLOntology localityModuleMinusPathAxiomsOntology = this.getOntologyManager().createOntology(localityModuleMinusPathAxioms);


        Configuration configuration=new Configuration();
        configuration.throwInconsistentOntologyException=false;
        OWLReasoner reasoner2=this.getReasonerFactory().createReasoner(localityModuleMinusPathAxiomsOntology, configuration);

        if(!reasoner2.isEntailed(ax)) {
            this.getOntologyManager().removeOntology(localityModuleMinusPathAxiomsOntology);
            reasoner2.dispose();
            return;
        }
        reasoner2.dispose();

        Set<OWLAxiom> singleJustification = null;

        singleJustification = canJustificationBeReused(path);

        if(singleJustification == null) {
            SingleJustGenerator s = new SingleJustGenerator(localityModuleMinusPathAxiomsOntology,this.getReasonerFactory(),this.getReasoner());
            singleJustification = s.computeSingleJustificationWithCore(ax,this.core,bubbleSize);
            allJustifications.add(singleJustification);
            s.dispose();
            if (singleJustification.size() == this.core.size()) {
                //System.out.println("min.size() == core.size()");
                this.getOntologyManager().removeOntology(localityModuleMinusPathAxiomsOntology);
                return;
            }
        }

        this.getOntologyManager().removeOntology(localityModuleMinusPathAxiomsOntology);

        for(OWLAxiom a : singleJustification) {
            if(this.core.contains(a)) {
                continue;
            }

            List<OWLAxiom> extendedPath = copyList(path);
            extendedPath.add(a);
            HS.add(extendedPath);

            this.computeAllJustifications(ax, extendedPath, bubbleSize);
        }

        return;
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
        HS.clear();
        union_allJustifications.clear();
        allJustifications.clear();

        this.getReasoner().dispose();
    }

}
