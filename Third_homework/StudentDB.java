package ru.ifmo.rain.kramer.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {

    private static final int ZERO = 0;
    private static final String EMPTY_STRING = "";
    private static final Student DEFAULT_STUDENT = new Student(ZERO, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING);
    private static final Map.Entry<String, List<Student>> DEFAULT_ENTRY = new AbstractMap.SimpleEntry<>(EMPTY_STRING, Collections.singletonList(DEFAULT_STUDENT));

    private static final Comparator<Student> cmp = Comparator.comparing(Student::getLastName).thenComparing(Student::getFirstName).thenComparingInt(Student::getId);

    private <T extends Collection<String>> T mappedStudentsCollection(List<Student> students, Function<Student, String> mapping, Supplier<T> collection) {
        return students.stream().map(mapping).collect(Collectors.toCollection(collection));
    }

    private List<String> mappedStudentsList(List<Student> students, Function<Student, String> mapping) {
        return mappedStudentsCollection(students, mapping, ArrayList::new);
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mappedStudentsList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mappedStudentsList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return mappedStudentsList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mappedStudentsList(students, student -> student.getFirstName() + " " + student.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mappedStudentsCollection(students, Student::getFirstName, TreeSet::new);
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream().min(Student::compareTo).map(Student::getFirstName).orElse("");
    }

    private List<Student> sortStudents(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream().sorted(comparator).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudents(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudents(students, cmp);
    }

    private List<Student> findStudents(Collection<Student> students, Function<Student, Stream<Student>> function) {
        return students.stream().flatMap(function).sorted(cmp).collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String firstName) {
        return findStudents(students, student -> firstName.equals(student.getFirstName()) ? Stream.of(student) : Stream.empty());
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String lastName) {
        return findStudents(students, student -> lastName.equals(student.getLastName()) ? Stream.of(student) : Stream.empty());
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return findStudents(students, student -> group.equals(student.getGroup()) ? Stream.of(student) : Stream.empty());
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return students.stream().flatMap(student -> group.equals(student.getGroup()) ? Stream.of(student) : Stream.empty()).
                collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }

    @SafeVarargs
    private <T> Comparator<T> getComparatorByCriterion(Comparator<T> first, Comparator<T>... comparators) {
        return Arrays.stream(comparators).reduce(first, Comparator::thenComparing);
    }

    private Stream<Map.Entry<String, List<Student>>> getGroupsStream(Collection<Student> students, Supplier<Map<String, List<Student>>> generator) {
        return students.stream().collect(Collectors.groupingBy(Student::getGroup, generator, Collectors.toList())).entrySet().stream();
    }

    private List<Group> getGroupsBy(Comparator<? super Student> comparator, Stream<Map.Entry<String, List<Student>>> groupStream) {
        return groupStream.map(entry -> new Group(entry.getKey(), entry.getValue().stream().sorted(comparator).collect(Collectors.toList()))).collect(Collectors.toList());
    }

    private List<Group> getSortedGroups(Comparator<? super Student> comparator, Collection<Student> students, Supplier<Map<String, List<Student>>> generator) {
        return getGroupsBy(comparator, getGroupsStream(students, generator));
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getSortedGroups(getComparatorByCriterion(Comparator.comparing(Student::getLastName), Comparator.comparing(Student::getFirstName), Student::compareTo), students, TreeMap::new);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getSortedGroups(getComparatorByCriterion(Student::compareTo), students, TreeMap::new);
    }

    private String getGroupNameByCriteria(Comparator<Map.Entry<String, List<Student>>> criteria, Collection<Student> collection, Supplier<Map<String, List<Student>>> generator) {
        return getGroupsStream(collection, generator).min(criteria).orElse(DEFAULT_ENTRY).getKey();
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getGroupNameByCriteria(getComparatorByCriterion(Comparator.comparing(g -> -g.getValue().size()), Comparator.comparing(Map.Entry::getKey)), students, HashMap::new);
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getGroupNameByCriteria(getComparatorByCriterion(Comparator.comparing(g -> -getDistinctFirstNames(g.getValue()).size()), Comparator.comparing(Map.Entry::getKey)), students, HashMap::new);
    }

}
