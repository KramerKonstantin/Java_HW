package ru.ifmo.rain.kramer.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {

    private int threadsNumber;
    private int eachCount;
    private int restCount;
    private List<Thread> threads;

    private <T> void init(int threads, List<? extends T> list) {
        if (threads <= 0) {
            throw new IllegalArgumentException("The number of threads must be at least 1.");
        }
        Objects.requireNonNull(list);
        threadsNumber = Math.max(1, Math.min(threads, list.size()));
        this.threads = new ArrayList<>();
        int count = list.size();
        eachCount = count / threadsNumber;
        restCount = count % threadsNumber;
    }

    private <T, R> void addThread(List<Thread> threads, List<R> threadValues, int index, Stream<? extends T> stream,
                                  Function<? super Stream<? extends T>, ? extends R> task) {
        Thread thread = new Thread(() -> threadValues.set(index, task.apply(stream)));
        thread.start();
        threads.add(thread);
    }

    private <T, R> void addThreads(List<R> threadValues, List<? extends T> list, Function<? super Stream<? extends T>,
            ? extends R> task) {
        for (int i = 0, l, r = 0; i < threadsNumber; i++) {
            l = r;
            r = l + eachCount + (restCount-- > 0 ? 1 : 0);
            addThread(threads, threadValues, i, list.subList(l, r).stream(), task);
        }
    }

    private void joinThreads(List<Thread> threads) throws InterruptedException {
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private <T, R> R task(int threads, final List<? extends T> list,
                              final Function<? super Stream<? extends T>, ? extends R> task,
                              final Function<? super Stream<? extends R>, ? extends R> ansCollector)
            throws InterruptedException {
        init(threads, list);
        List<R> threadValues = new ArrayList<>(Collections.nCopies(threadsNumber, null));
        addThreads(threadValues, list, task);
        joinThreads(this.threads);
        return ansCollector.apply(threadValues.stream());
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Unable to handle empty list");
        }
        final Function<Stream<? extends T>, ? extends T> streamMax = stream -> stream.max(comparator).orElseThrow();
        return task(threads, list, streamMax, streamMax);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, list, Collections.reverseOrder(comparator));
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return task(threads, list, stream -> stream.allMatch(predicate), stream -> stream.allMatch(item -> item));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, list, predicate.negate());
    }

    @Override
    public String join(int threads, List<?> list) throws InterruptedException {
        return task(threads, list, stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return task(threads, list, stream -> stream.filter(predicate).collect(Collectors.toList()),
                listStream -> listStream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> list, Function<? super T, ? extends U> function) throws InterruptedException {
        return task(threads, list, stream -> stream.map(function).collect(Collectors.toList()),
                listStream -> listStream.flatMap(Collection::stream).collect(Collectors.toList()));
    }
}
