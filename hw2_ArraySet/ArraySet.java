package ru.ifmo.rain.kramer.arrayset;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> data;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        comparator = null;
        data = Collections.emptyList();
    }

    public ArraySet(Collection<? extends T> collection) {
        comparator = null;
        data = new ArrayList<>(new TreeSet<>(collection));
    }

    public ArraySet(Comparator<? super T> cmp) {
        comparator = cmp;
        data = Collections.emptyList();
    }

    public ArraySet(Collection<? extends T> collection, Comparator<? super T> cmp) {
        comparator = cmp;
        TreeSet<T> ts = new TreeSet<>(cmp);
        ts.addAll(collection);
        data = new ArrayList<>(ts);
    }

    private ArraySet(List<T> list, Comparator<? super T> cmp) {
        comparator = cmp;
        data = list;
        if (list instanceof ReversedList) {
            ((ReversedList) list).reverse();
        }
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(data).iterator();
    }

    @Override
    public boolean contains(Object obj) {
        return Collections.binarySearch(data, (T) Objects.requireNonNull(obj), comparator) >= 0;
    }

    private void checkNonEmpty() {
        if (data.isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public T first() {
        checkNonEmpty();
        return data.get(0);
    }

    @Override
    public T last() {
        checkNonEmpty();
        return data.get(size() - 1);
    }

    private T getElem(int index) {
        return (index < 0) ? null : data.get(index);
    }

    private boolean validInd(int index) {
        return 0 <= index && index < size();
    }

    private int indexGetter(T t, int found, int notFound) {
        int res = Collections.binarySearch(data, Objects.requireNonNull(t), comparator);
        if (res < 0) {
            res = -res - 1;
            return validInd(res + notFound) ? res + notFound : -1;
        }
        return validInd(res + found) ? res + found : -1;
    }

    private int lowerInd(T t) {
        return indexGetter(t, -1, -1);
    }

    private int higherInd(T t) {
        return indexGetter(t, 1, 0);
    }

    private int floorInd(T t) {
        return indexGetter(t, 0, -1);
    }

    private int ceilingInd(T t) {
        return indexGetter(t, 0, 0);
    }

    @Override
    public T lower(T t) {
        return getElem(lowerInd(t));
    }

    @Override
    public T higher(T t) {
        return getElem(higherInd(t));
    }

    @Override
    public T floor(T t) {
        return getElem(floorInd(t));
    }

    @Override
    public T ceiling(T t) {
        return getElem(ceilingInd(t));
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new ReversedList<>(data), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        int l = fromInclusive ? ceilingInd(fromElement) : higherInd(fromElement);
        int r = toInclusive ? floorInd(toElement) : lowerInd(toElement);
        if (l == -1 || r == -1 || l > r) {
            return new ArraySet<>(comparator);
        } else {
            return new ArraySet<>(data.subList(l, r + 1), comparator);
        }
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (data.isEmpty()) {
            return new ArraySet<>(comparator);
        }
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (data.isEmpty()) {
            return new ArraySet<>(comparator);
        }
        return subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        if (comparator.compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("Left border should be less or equal than right.");
        }
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }
}
