package nl.vu.kai.tools;

import java.util.*;

public class MultiMap<K,V> {
    private Map<K, Set<V>> inner = new HashMap<>();

    public void add(K key, V value) {
        if(!inner.containsKey(key))
            inner.put(key, new HashSet<>());
        inner.get(key).add(value);
    }

    public Set<V> get(K key){
        return inner.getOrDefault(key, Collections.emptySet());
    }

    public boolean contains(K key) {
        return inner.containsKey(key);
    }

    public void remove(K key, V value){
        inner.get(key).remove(value);
    }

    public Set<K> keys(){
        return inner.keySet();
    }
}
