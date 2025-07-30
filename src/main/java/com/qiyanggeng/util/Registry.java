package com.qiyanggeng.util;

import java.util.*;
import java.util.function.Predicate;

public record Registry<K, V>(Map<K, Collection<V>> registry, CollisionPolicy policy) {
    private static final CollisionPolicy DEFAULT_POLICY = CollisionPolicy.DUPLICATE;
    
    public boolean register(K k, V v) {
        return registry.computeIfAbsent(k, _ -> new Registration<>(new ArrayList<>(), policy)).add(v);
    }
    
    public boolean register(K k, Collection<V> v) {
        return registry.computeIfAbsent(k, _ -> new Registration<>(new ArrayList<>(), policy)).addAll(v);
    }
    
    public Collection<V> deregisterKey(K k) {
        return registry.remove(k);
    }
    
    public boolean deregister(K k, V v) {
        if(!registry.containsKey(k))
            return false;
        
        final Collection<V> target = registry.get(k);
        final boolean result = target.remove(v);
        
        if(target.isEmpty())
            deregisterKey(k);
        
        return result;
    }
    
    public boolean deregister(V v) {
        return registry.keySet().stream()
                .map(k -> deregister(k, v))
                .reduce(false, (b1, b2) -> b1 || b2);
    }
    
    public boolean deregister(K k, Collection<V> v) {
        return v.stream()
                .map(vv -> deregister(k, vv))
                .reduce(false, (b1, b2) -> b1 || b2);
    }
    
    public boolean deregisterIf(Predicate<V> condition) {
        return registry.keySet().stream()
                .map(k -> deregisterIf(k, condition))
                .reduce(false, (b1, b2) -> b1 || b2);
    }
    
    public boolean deregisterIf(K k, Predicate<V> condition) {
        return registry.get(k).stream()
                .filter(condition)
                .map(v -> deregister(k, v))
                .reduce(false, (b1, b2) -> b1 || b2);
    }
    
    public Collection<V> get(K k) {
        return registry.get(k);
    }
    
    public int size() {
        return registry.values().stream().mapToInt(Collection::size).sum();
    }
    
    public int size(K k) {
        return registry.getOrDefault(k, List.of()).size();
    }
    
    public boolean isEmpty() {
        return size() == 0;
    }
    
    public boolean isEmpty(K k) {
        return size(k) == 0;
    }
    
    public enum CollisionPolicy {
        DUPLICATE {
            @Override
            <R> boolean handleAdd(List<R> list, R element) {
                return list.add(element);
            }
        },
        DISCARD {
            @Override
            <R> boolean handleAdd(List<R> list, R element) {
                if(list.contains(element))
                    return false;
                
                return list.add(element);
            }
        },
        ABORT {
            @Override
            <R> boolean handleAdd(List<R> list, R element) {
                if(list.contains(element))
                    throw new RuntimeException("Aborted per policy when attempting to register under a key which already had a value");
                
                return list.add(element);
            }
        };
        
        abstract <R> boolean handleAdd(List<R> list, R element);
        
        <R> boolean handleAddAll(List<R> list, Collection<? extends R> elements) {
            return elements.stream().map(e -> handleAdd(list, e)).reduce(false, (a, b) -> a || b);
        }
    }
    
    private record Registration<R>(List<R> registrations, CollisionPolicy policy) implements Collection<R> {
        @Override
        public boolean add(R r) {
            return policy.handleAdd(registrations, r);
        }
        
        @Override
        public boolean addAll(Collection<? extends R> c) {
            return policy.handleAddAll(registrations, c);
        }
        
        @Override
        public boolean remove(Object o) {
            return registrations.remove(o);
        }
        
        @Override
        public boolean removeAll(Collection<?> c) {
            return registrations.removeAll(c);
        }
        
        @Override
        public boolean contains(Object o) {
            return registrations.contains(o);
        }
        
        @Override
        public boolean containsAll(Collection<?> c) {
            return new HashSet<>(registrations).containsAll(c);
        }
        
        @Override
        public boolean retainAll(Collection<?> c) {
            return registrations.retainAll(c);
        }
        
        @Override
        public void clear() {
            registrations.clear();
        }
        
        @Override
        public Object[] toArray() {
            return registrations.toArray();
        }
        
        @Override
        public <T> T[] toArray(T[] a) {
            return registrations.toArray(a);
        }
        
        @Override
        public Iterator<R> iterator() {
            return registrations.iterator();
        }
        
        @Override
        public int size() {
            return registrations.size();
        }
        
        @Override
        public boolean isEmpty() {
            return registrations.isEmpty();
        }
    }
    
    public Registry(Map<K, Collection<V>> registry) {
        this(registry, DEFAULT_POLICY);
    }
}

