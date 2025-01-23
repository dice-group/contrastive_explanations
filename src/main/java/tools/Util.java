package tools;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {

    private Util(){
        // utilities class
    }

    public static <T> T randomItem(List<T> list, Random random){
        return list.get(random.nextInt(list.size()));
    }
    public static <T> T randomItem(Stream<T> stream, Random random) {
        return randomItem(stream.collect(Collectors.toList()), random);
    }

    public static <T> T randomItem(Set<T> collection, Random random) {
        return randomItem(new ArrayList<>(collection), random);
    }

}
