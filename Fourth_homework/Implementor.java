package ru.ifmo.rain.kramer.implementor;

import info.kgeorgiy.java.advanced.implementor.JarImpler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * @author Kramer Konstantin
 * Implementation class for {@link JarImpler} interface
 */
public class Implementor implements JarImpler {

    /**
     * File name extension for source file.
     */
    private final static String JAVA = "java";
    /**
     * Suffix of generated class name.
     */
    private final static String CLASS_NAME_SUFFIX = "Impl";

    /**
     * Empty string token.
     */
    private final static String EMPTY = "";
    /**
     * Space token.
     */
    private final static String SPACE = " ";
    /**
     * Tabulation token.
     */
    private final static String TAB = "\t";
    /**
     * New line token.
     */
    private final static String NEWLINE = System.lineSeparator();
    /**
     * Double new line token.
     */
    private final static String DOUBLE_NEWLINE = NEWLINE + NEWLINE;
    /**
     * Colon token.
     */
    private final static String COLON = ";";
    /**
     * Open curly bracket token.
     */
    private final static String CURLY_OPEN = "{";
    /**
     * Close curly bracket token.
     */
    private final static String CURLY_CLOSE = "}";
    /**
     * Open bracket token.
     */
    private final static String OPEN = "(";
    /**
     * Close bracket token.
     */
    private final static String CLOSE = ")";
    /**
     * Comma token.
     */
    private final static String COMMA = ",";
    /**
     * Dot token.
     */
    private final static String DOT = ".";
    /**
     * Less token.
     */
    private final static String LESS = "<";
    /**
     * Greater token.
     */
    private final static String GREATER = ">";
    /**
     * Type token.
     */
    private final static String TYPE_T = LESS + "T" + GREATER;

    /**
     * String representation of keyword <code>temp</code>
     */
    private final static String TEMP = "temp";
    /**
     * String representation of keyword <code>package</code>
     */
    private final static String PACKAGE = "package ";
    /**
     * String representation of keyword <code>class</code>
     */
    private final static String CLASS = "class ";
    /**
     * String representation of keyword <code>implements</code>
     */
    private final static String IMPLEMENTS = "implements ";
    /**
     * String representation of keyword <code>extends</code>
     */
    private final static String EXTENDS = "extends ";
    /**
     * String representation of keyword <code>throws</code>
     */
    private final static String THROWS = "throws ";
    /**
     * String representation of keyword <code>public</code>
     */
    private final static String PUBLIC = "public ";
    /**
     * String representation of keyword <code>protected</code>
     */
    private final static String PROTECTED = "protected ";
    /**
     * String representation of keyword <code>return</code>
     */
    private final static String RETURN = "return ";
    /**
     * String representation of keyword <code>super</code>
     */
    private final static String SUPER = "super ";

    /**
     * String representation of annotation {@link Deprecated}
     */
    private final static String DEPRECATED = "@Deprecated" + NEWLINE;

    /**
     * String representation of generated class's {@link Class#getSimpleName()}
     */
    private String className;

    /**
     * Sets field {@link #className} to actual class name of target generated class.
     * This method is only used to set field {@link #className}.
     *
     * @param clazz target type token
     */
    private void setClassName(Class<?> clazz) {
        this.className = clazz.getSimpleName() + CLASS_NAME_SUFFIX;
    }

    /**
     * Creates new instance of {@link Implementor}
     */
    public Implementor() {}

    /**
     * Checks if a class can be extended.
     * Note: a class can't be extended if:
     * <ul>
     *     <li>It is a primitive</li>
     *     <li>It is final</li>
     *     <li>It is array</li>
     *     <li>It is enum</li>
     *     <li>It is {@link Enum}</li>
     * </ul>
     *
     * @param clazz target type token
     * @throws ImplerException if the class can't be extended
     */
    private void validateClass(Class<?> clazz) throws ImplerException {
        if (clazz.isPrimitive() || clazz.isArray() || clazz == Enum.class || Modifier.isFinal(clazz.getModifiers())) {
            throw new ImplerException(String.format("Incorrect class: %s", clazz.getSimpleName()));
        }
    }

    /**
     * Returns full path to the file with target class implementation.
     *
     * @param path initial path
     * @param clazz target type token
     * @param extension extension of target source file, e.g. {@value #JAVA}
     * @return full path to the file with target class implementation
     */
    private Path getFilePath(Path path, Class<?> clazz, String extension) {
        return path.resolve(clazz.getPackage().getName().replace(DOT.charAt(0), File.separatorChar))
                .resolve(clazz.getSimpleName() + CLASS_NAME_SUFFIX + DOT + extension.trim());
    }

    /**
     * Return package declaration of the generated class in the following format:
     * <code>package a.b.c;</code> with two line breaks at the end.
     *
     * @param clazz target type token
     * @return package declaration with two line breaks at the end
     */
    private String getPackageDeclaration(Class<?> clazz) {
        if (!clazz.getPackage().getName().equals(EMPTY)) {
            return PACKAGE + clazz.getPackageName() + COLON + DOUBLE_NEWLINE;
        }
        return EMPTY;
    }

    /**
     * Return class declaration of the generated class in the following format:
     * <code>class className;</code> with two line breaks at the end.
     *
     * @param clazz target type token
     * @return class declaration with two line breaks at the end
     */
    private String getClassDeclaration(Class<?> clazz) {
        var deriveKeyWord = clazz.isInterface() ? IMPLEMENTS : EXTENDS;
        return PUBLIC + CLASS + className + SPACE + deriveKeyWord + clazz.getSimpleName() +
                SPACE + CURLY_OPEN + DOUBLE_NEWLINE;
    }

    /**
     * Writes implementation of constructors of given {@link Class} via specified
     * {@link BufferedWriter}
     *
     * @param clazz target type token
     * @param writer given {@link BufferedWriter}
     * @throws ImplerException if there is no callable constructor in the target class.
     */
    private void generateConstructors(Class<?> clazz, BufferedWriter writer) throws ImplerException {
        var constructors = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers())).collect(Collectors.toList());
        if (constructors.isEmpty()) {
            throw new ImplerException(String.format("Class %s has no callable constructors", clazz.getSimpleName()));
        }
        for (var constructor : constructors) {
            generateExecutable(constructor, writer);
        }
    }

    /**
     * A wrapper of {@link Method} for proper method comparing.
     */
    private class CustomMethod {
        /**
         * Instance of the method.
         */
        private Method instance;
        /**
         * A flag that shows if we need to override the method.
         * It is set to <code>true</code> if we met a <code>final</code> or just not <code>abstract</code> method in
         * the <code>super</code> class.
         * Otherwise, it is set to <code>false</code>.
         */
        private boolean isOverridden;

        /**
         * Constructor for the wrapper that receives instance and the flag.
         *
         * @param m target instance of {@link Method}
         * @param isOverridden target flag
         */
        CustomMethod(Method m, boolean isOverridden) {
            instance = m;
            this.isOverridden = isOverridden;
        }

        /**
         * Custom {@link Object#equals(Object)} method to compare two instances of {@link Method}.
         * Methods are equal if:
         * <ul>
         *     <li>The other method is not <code>null</code></li>
         *     <li>The other method's {@link #hashCode()} is the same</li>
         * </ul>
         *
         * @param obj target instance
         * @return <code>true</code> if instances are equal, <code>false</code> otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj instanceof CustomMethod) {
                return obj.hashCode() == hashCode();
            }
            return false;
        }

        /**
         * Calculates hash code of a {@link Method} instance
         * Formula: {@link Arrays#hashCode(Object[])} of type parameters plus {@link Class#hashCode()} of
         * return type plus {@link String#hashCode()} of name.
         *
         * @return hash code of the {@link #instance}
         */
        @Override
        public int hashCode() {
            return Arrays.hashCode(instance.getParameterTypes()) + instance.getReturnType().hashCode()
                    + instance.getName().hashCode();
        }
    }

    /**
     * Adds all abstract methods of the target type to {@link Set} of {@link CustomMethod}
     *
     * @param methods target set
     * @param clazz target type token
     */
    private void fill(Set<CustomMethod> methods, Class<?> clazz) {
        if (clazz == null) return;
        methods.addAll(Arrays.stream(clazz.getDeclaredMethods())
                .map(m -> new CustomMethod(m, Modifier.isFinal(m.getModifiers())
                        || !Modifier.isAbstract(m.getModifiers()))).collect(Collectors.toSet()));
        Arrays.stream(clazz.getInterfaces()).forEach(i -> fill(methods, i));
        fill(methods, clazz.getSuperclass());
    }

    /**
     * Writes implementation of abstract methods of given {@link Class} via specified
     * {@link BufferedWriter}
     *
     * @param clazz given class to implement abstract methods
     * @param writer given {@link BufferedWriter}
     */
    private void generateAbstractMethods(Class<?> clazz, BufferedWriter writer) {
        var methods = new HashSet<CustomMethod>();
        fill(methods, clazz);
        methods.stream().filter(m -> !m.isOverridden).forEach(m -> {
            try {
                generateExecutable(m.instance, writer);
            } catch (ImplerException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Generates implementation of an {@link Executable}.
     * The executable must be either {@link Method} or {@link Constructor} to work correctly.
     * Implementation contains:
     * <ul>
     *     <li>[optional] {@link Deprecated} annotation if needed</li>
     *     <li>Access modifier</li>
     *     <li>[optional] Type parameters (if it is a method)</li>
     *     <li>[optional] Return type (if it is a method)</li>
     *     <li>Name ({@link #className} if it is a constructor</li>
     *     <li>List of all checked exceptions thrown</li>
     *     <li>Implementation formed by either {@link #getMethodImplementation(Class)} or
     *     {@link #getConstructorImplementation(Executable)}</li>
     * </ul>
     *
     * @param executable target executable
     * @param writer given {@link BufferedWriter}
     * @throws ImplerException if there is no callable constructor in the target class.
     *
     * @see "https://www.geeksforgeeks.org/checked-vs-unchecked-exceptions-in-java/"
     */
    private void generateExecutable(Executable executable, BufferedWriter writer) throws ImplerException {
        var isMethod = executable instanceof Method;
        var accessModifierMask = executable.getModifiers() & (Modifier.PROTECTED | Modifier.PUBLIC);
        var accessModifier = EMPTY;
        if (accessModifierMask > 0) {
            accessModifier = Modifier.isPublic(accessModifierMask) ? PUBLIC : PROTECTED;
        }
        var returnType = EMPTY;
        var name = className;
        var implementation = getConstructorImplementation(executable);
        var isDeprecated = executable.isAnnotationPresent(Deprecated.class);
        if (isMethod) {
            name = executable.getName();
            implementation = getMethodImplementation(((Method) executable).getReturnType());
            returnType = ((Method) executable).getGenericReturnType().getTypeName() + SPACE;
            var types = new ArrayList<String>();
            types.add(returnType);
            types.addAll(Arrays.stream(executable.getParameters()).map(Parameter::toString).collect(Collectors.toList()));
            for (var type : types) {
                if (type.contains(TYPE_T)) {
                    returnType = TYPE_T + SPACE + returnType;
                    break;
                }
            }
        }
        var args = getJoinedStrings(executable.getParameters());
        var exceptions = getJoinedStrings(executable.getExceptionTypes());
        if (exceptions.length() > 0) exceptions = THROWS + exceptions;
        try {
            if (isDeprecated) {
                writer.write(DEPRECATED);
            }
            writer.write(TAB + accessModifier + returnType + name + OPEN + args + CLOSE + SPACE + exceptions
                    + SPACE + CURLY_OPEN + NEWLINE + TAB + implementation + NEWLINE + TAB);
            writer.write(CURLY_CLOSE + DOUBLE_NEWLINE);
        } catch (IOException ignored) {
            throw new ImplerException(String.format("Can't write implementation of a following method: %s", executable.getName()));
        }
    }

    /**
     * Returns implementation of a constructor.
     * Implementation contains only call of the super constructor.
     *
     * @param executable target constructor
     * @return string representation of constructor's implementation
     */
    private String getConstructorImplementation(Executable executable) {
        var args = new StringJoiner(COMMA + SPACE);
        Arrays.stream(executable.getParameters()).forEach(param -> args.add(param.getName()));
        return TAB + SUPER + OPEN + args.toString() + CLOSE + COLON;
    }

    /**
     * Returns implementation of a method by a return type.
     * Implementation contains <code>false</code> for <code>boolean</code> type, {@link #EMPTY} for
     * <code>void</code> type, <code>0</code> for other primitives and <code>null</code> for
     * classes.
     *
     * @param returnType target return type
     * @return string representation of method's implementation
     */
    private String getMethodImplementation(Class<?> returnType) {
        String defaultValueStr;
        if (returnType.equals(Void.TYPE)) {
            return EMPTY;
        }
        try {
            defaultValueStr = getDefaultValue(returnType);
            if (defaultValueStr.charAt(0) == Character.MIN_VALUE) {
                defaultValueStr = "'\u0000'";
            } else if (defaultValueStr.equals("0.0")) {
                defaultValueStr += "f";
            }
        } catch (NullPointerException | IllegalArgumentException ignored) {
            defaultValueStr = "null";
        }
        return TAB + RETURN + defaultValueStr + COLON;
    }

    /**
     * Returns comma-separated elements of {@link AnnotatedElement}.
     * This method is most commonly used to get arguments list as a string but can be easily
     * used for other purposes.
     *
     * @param annotatedElements list of {@link AnnotatedElement}
     * @return string representation of comma-separated arguments
     */
    private String getJoinedStrings(AnnotatedElement[] annotatedElements) {
        var joiner = new StringJoiner(COMMA + SPACE);
        Arrays.stream(annotatedElements).forEach(ae -> {
            var element = ae.toString();
            if (element.matches("class .+")) {
                element = element.split(" ")[1];
            }
            joiner.add(element.replaceAll("\\$", "\\."));
        });
        return joiner.toString();
    }

    /**
     * Gets default value of given class
     * @param token class to get default value
     * @return {@link String} representing value
     */
    private static String getDefaultValue(Class<?> token) {
        if (token.equals(boolean.class)) {
            return " false";
        } else if (token.equals(void.class)) {
            return "";
        } else if (token.isPrimitive()) {
            return " 0";
        }
        return " null";
    }

    /**
     * Compiles a provided source file using system java compiler.
     * This method uses class path used when launching the program so make sure you specified
     * all the paths (including modules) in the <code>-classpath</code> flag.
     *
     * @param clazz target type token
     * @param path target source file
     * @throws ImplerException if compilation error has occurred when compiling target source file.
     */
    private void compile(Class<?> clazz, Path path) throws ImplerException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        var joiner = new StringJoiner(File.pathSeparator);
        var pathsStream = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(Paths::get);
        pathsStream.forEach(p -> Arrays.stream(Objects.requireNonNull(p.getParent().toFile().listFiles()))
                .forEach(f -> joiner.add(Paths.get(f.toURI()).toAbsolutePath().toString())));
        var classPath = joiner.toString();
        String[] args = new String[]{
                "-cp", path.toString() + File.pathSeparator + classPath, getFilePath(path, clazz, JAVA).toString()
        };
        if (compiler == null || compiler.run(null, null, null, args) != 0) {
            throw new ImplerException(String.format("Unable to compile %s.java", className));
        }
    }

    /**
     * Creates a <code>.jar</code> file.
     * Note, that the obtained file is not executable and contains only one <code>.class</code> file.
     *
     * @param clazz target type token
     * @param path target path for the output <code>jar</code> file
     * @param sourcePath source file of <code>.class</code> file
     * @throws ImplerException if an internal {@link IOException} has occurred
     */
    private void createJar(Class<?> clazz, Path path, Path sourcePath) throws ImplerException {
        var manifest = new Manifest();
        var attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (var writer = new JarOutputStream(Files.newOutputStream(path), manifest)) {
            writer.putNextEntry(new ZipEntry(clazz.getName().replace('.', '/')
                    + CLASS_NAME_SUFFIX + DOT + CLASS.trim()));
            Files.copy(getFilePath(sourcePath, clazz, CLASS), writer);
        } catch (IOException e) {
            throw new ImplerException("Unable to write to JAR file", e);
        }
    }

    /**
     * Recursively deletes directory represented by <code>path</code>
     *
     * @param path directory to be recursively deleted
     * @throws IOException if error occurred during deleting
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteDirectory(Path path) throws IOException {
        Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    /**
     * Generates implementation of a class denoted by the provided type token and creates a <code>.jar</code>
     * file which contains that implementation in the provided path.
     *
     * @param clazz target type token
     * @param path target path
     * @throws ImplerException if:
     * <ul>
     *     <li>One or more arguments are <code>null</code></li>
     *     <li>Target class can't be extended</li>
     *     <li>An internal {@link IOException} has occurred when handling I/O processes</li>
     *     <li>There are no callable constructors in the target class</li>
     * </ul>
     */
    @Override
    public void implement(Class<?> clazz, Path path) throws ImplerException {
        validateClass(clazz);
        path = getFilePath(path, clazz, JAVA);
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new ImplerException("Unable to create directories for output file", e);
            }
        }
        setClassName(clazz);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(getPackageDeclaration(clazz));
            writer.write(getClassDeclaration(clazz));
            if (!clazz.isInterface()) {
                generateConstructors(clazz, writer);
            }
            generateAbstractMethods(clazz, writer);
            writer.write(CURLY_CLOSE + DOUBLE_NEWLINE);
        } catch (IOException e) {
            throw new ImplerException("Unable to write to output file", e);
        }
    }

    /**
     * Generates implementation of a class denoted by the provided type token and creates a <code>.jar</code>
     * file which contains that implementation in the provided path.
     *
     * @param clazz target type token
     * @param path target path
     * @throws ImplerException if:
     * <ul>
     *     <li>One or more arguments are <code>null</code></li>
     *     <li>Target class can't be extended</li>
     *     <li>An internal {@link IOException} has occurred when handling I/O processes</li>
     *     <li>{@link javax.tools.JavaCompiler} failed to compile target source file</li>
     *     <li>There are no callable constructors in the target class</li>
     * </ul>
     */
    @Override
    public void implementJar(Class<?> clazz, Path path) throws ImplerException {
        validateClass(clazz);
        Path sourcePath;
        try {
            sourcePath = Files.createTempDirectory(path.toAbsolutePath().getParent(), TEMP);
        } catch (IOException e) {
            throw new ImplerException("Can't create a temporary directory.", e);
        }
        try {
            implement(clazz, sourcePath);
            compile(clazz, sourcePath);
            createJar(clazz, path, sourcePath);
        } finally {
            try {
                deleteDirectory(sourcePath);
            } catch (IOException e) {
                System.err.println("Can't delete the temporary directory.");
            }
        }
    }

    /**
     * This function is used to choose which way of implementation to execute.
     * Runs {@link Implementor} in two possible ways:
     *  <ul>
     *      <li> 2 arguments: className rootPath - runs {@link #implement(Class, Path)} with given arguments</li>
     *      <li> 3 arguments: -jar className jarPath - runs {@link #implementJar(Class, Path)} with two second arguments</li>
     *  </ul>
     *  If arguments are incorrect or an error occurs during implementation returns message with information about error
     *
     * @param args arguments for running an application
     */
    public static void main(String[] args) {
        if (args == null || !(args.length == 2 || args.length == 3)) {
            System.err.println("Two or three arguments expected");
            return;
        } else {
            for (String arg: args) {
                if (arg == null) {
                    System.err.println("All arguments must be non-null");
                }
            }
        }
        JarImpler implementor = new Implementor();
        try {
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            }
        } catch (InvalidPathException e) {
            System.err.println("Incorrect path: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Incorrect class name: " + e.getMessage());
        } catch (ImplerException e) {
            System.err.println("An error occurred during implementation: " + e.getMessage());
        }
    }
}