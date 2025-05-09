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
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;



import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Example {

    public static OWLOntologyManager manager;
    public static OWLDataFactory dataFactory;
    public static Reasoner.ReasonerFactory factory;

    private static Set<Set<OWLAxiom>> allJustifications = new HashSet<Set<OWLAxiom>>();
    private static Set<OWLAxiom> union_allJustifications = new HashSet<OWLAxiom>();
    private static Set<List<OWLAxiom>> HS = new HashSet<List<OWLAxiom>>();
    private static Set<OWLAxiom> core = new HashSet<>();


    public static void main(String[] args) throws Exception {

        manager= OWLManager.createOWLOntologyManager();
        dataFactory=manager.getOWLDataFactory();

        //File inputOntologyFile = new File(args[0]);


        File inputOntologyFile = new File("examples/ontologies/pizza.owl");

        OWLOntology ontology=manager.loadOntologyFromOntologyDocument(inputOntologyFile);
        System.out.println("- Name of the Ontology: "+inputOntologyFile);
        System.out.println("- Number of Ontology axioms: "+ontology.getAxiomCount());
        manager.removeAxioms(ontology, ontology.getABoxAxioms(Imports.INCLUDED));
        System.out.println("- Number of Ontology TBox axioms: "+ontology.getAxiomCount());

        OWLClass hot = dataFactory.getOWLClass(IRI.create("http://www.co-ode.org/ontologies/pizza/pizza.owl#PizzaBase"));
        OWLClass spiceness = dataFactory.getOWLClass(IRI.create("http://www.co-ode.org/ontologies/pizza/pizza.owl#DeepPanBase"));
        OWLClass food = dataFactory.getOWLClass(IRI.create("http://www.co-ode.org/ontologies/pizza/pizza.owl#Food"));
        OWLClass pizza = dataFactory.getOWLClass(IRI.create("http://www.co-ode.org/ontologies/pizza/pizza.owl#Pizza"));
        OWLClass american = dataFactory.getOWLClass(IRI.create("http://www.co-ode.org/ontologies/pizza/pizza.owl#American"));
        //OWLSubClassOfAxiom ax = dataFactory.getOWLSubClassOfAxiom(spiceness,hot);



        factory = new Reasoner.ReasonerFactory();
        Configuration configuration=new Configuration();
        configuration.throwInconsistentOntologyException=false;
        OWLReasoner re=factory.createReasoner(ontology, configuration);

        SyntacticLocalityModuleExtractor sm = new SyntacticLocalityModuleExtractor(manager, ontology, ModuleType.STAR);


        OWLSubClassOfAxiom axiom = dataFactory.getOWLSubClassOfAxiom(american,pizza);
        if(re.isEntailed(axiom)){

            //compute star modules

            Set<OWLAxiom> moduleAxioms = sm.extract(axiom.getSignature());

            OWLOntology moduleOnto= manager.createOntology(moduleAxioms);


            for(OWLAxiom axiom1: moduleOnto.getAxioms()){
                if(!(axiom1 instanceof OWLLogicalAxiom)){
                    manager.applyChange(new RemoveAxiom(moduleOnto, axiom1));
                    moduleAxioms.remove(axiom1);
                }

            }
            System.out.println("- The size of star module (logical axioms): "+moduleAxioms.size());
            int bubbleSize = moduleOnto.getAxiomCount()/10;
            OWLReasoner reasoner=factory.createReasoner(moduleOnto, configuration);


            SingleJustGenerator si = new SingleJustGenerator(moduleAxioms);
            Set<OWLAxiom> singleJust = si.computeSingleJustification(axiom,bubbleSize);
            si.dispose();
            System.out.println("- A single justification:"+axiom);

            Long startCore = System.currentTimeMillis();
            //compute the core/intersection of all justifications
            SingleJustGenerator s = new SingleJustGenerator(moduleAxioms);
            core = s.computeCoreofAllJustifications(axiom);
            Long endCore = System.currentTimeMillis();
            System.out.println("- The size of the core/intersection of all justification (s) is: "+core.size());
            System.out.println("- The core/intersection of all justification is: "+core);
            System.out.println("- Time for computing the core (s) is "+ (float) (endCore - startCore) / 1000);

            s.dispose();

            if(core.size()==singleJust.size()){
                System.out.println("@@@only 1 justification");
            }


            Long startUnion = System.currentTimeMillis();
            //compute union of all justifications
            AllJustificationGenerator allGen = new AllJustificationGenerator(moduleOnto.getAxioms(),core);
            allGen.computeUnionOfAllJustifications(axiom,bubbleSize);
            union_allJustifications=new HashSet<>(allGen.union_allJustifications);
            Long endTimeUnion = System.currentTimeMillis();

            System.out.println("- The size of union (black-box method) is: "+union_allJustifications.size());
            System.out.println("- The union is: "+union_allJustifications);
            System.out.println("- Time for computing the union (s) is "+ (float) (endTimeUnion - startUnion) / 1000);
            allGen.dispose();

            clearGlobal();
            manager.removeOntology(moduleOnto);
        }


    }

    private static void clearGlobal(){

        HS.clear();
        allJustifications.clear();
        core.clear();
        union_allJustifications.clear();
    }

}
