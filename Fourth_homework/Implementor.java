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
 * Implementation class for {@link JarImpler} interface
 */
public class Implementor implements JarImpler {

    private final static String JAVA = "java";
    private final static String CLASS_NAME_SUFFIX = "Impl";

    private final static String EMPTY = "";
    private final static String SPACE = " ";
    private final static String TAB = "\t";
    private final static String NEWLINE = System.lineSeparator();
    private final static String DOUBLE_NEWLINE = NEWLINE + NEWLINE;
    private final static String COLON = ";";
    private final static String CURLY_OPEN = "{";
    private final static String CURLY_CLOSE = "}";
    private final static String OPEN = "(";
    private final static String CLOSE = ")";
    private final static String COMMA = ",";
    private final static String DOT = ".";
    private final static String LESS = "<";
    private final static String GREATER = ">";
    private final static String TYPE_T = LESS + "T" + GREATER;

    private final static String TEMP = "temp";
    private final static String PACKAGE = "package ";
    private final static String CLASS = "class ";
    private final static String IMPLEMENTS = "implements ";
    private final static String EXTENDS = "extends ";
    private final static String THROWS = "throws ";
    private final static String PUBLIC = "public ";
    private final static String PROTECTED = "protected ";
    private final static String RETURN = "return ";
    private final static String SUPER = "super ";

    private final static String DEPRECATED = "@Deprecated" + NEWLINE;

    private String className;

    private void setClassName(Class<?> clazz) {
        this.className = clazz.getSimpleName() + CLASS_NAME_SUFFIX;
    }

    private void validateClass(Class<?> clazz) throws ImplerException {
        if (clazz.isPrimitive() || clazz.isArray() || clazz == Enum.class || Modifier.isFinal(clazz.getModifiers())) {
            throw new ImplerException(String.format("Incorrect class: %s", clazz.getSimpleName()));
        }
    }

    private Path getFilePath(Path path, Class<?> clazz, String extension) {
        return path.resolve(clazz.getPackage().getName().replace('.', File.separatorChar))
                .resolve(clazz.getSimpleName() + CLASS_NAME_SUFFIX + DOT + extension.trim());
    }

    private String getPackageDeclaration(Class<?> clazz) {
        if (!clazz.getPackage().getName().equals(EMPTY)) {
            return PACKAGE + clazz.getPackageName() + COLON + DOUBLE_NEWLINE;
        }
        return EMPTY;
    }

    private String getClassDeclaration(Class<?> clazz) {
        var deriveKeyWord = clazz.isInterface() ? IMPLEMENTS : EXTENDS;
        return PUBLIC + CLASS + className + SPACE + deriveKeyWord + clazz.getSimpleName() +
                SPACE + CURLY_OPEN + DOUBLE_NEWLINE;
    }

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

    private class CustomMethod {
        private Method instance;
        private boolean isOverridden;

        CustomMethod(Method m, boolean isOverridden) {
            instance = m;
            this.isOverridden = isOverridden;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (obj instanceof CustomMethod) {
                return obj.hashCode() == hashCode();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(instance.getParameterTypes()) + instance.getReturnType().hashCode()
                    + instance.getName().hashCode();
        }
    }

    private void fill(Set<CustomMethod> methods, Class<?> clazz) {
        if (clazz == null) return;
        methods.addAll(Arrays.stream(clazz.getDeclaredMethods())
                .map(m -> new CustomMethod(m, Modifier.isFinal(m.getModifiers())
                        || !Modifier.isAbstract(m.getModifiers()))).collect(Collectors.toSet()));
        Arrays.stream(clazz.getInterfaces()).forEach(i -> fill(methods, i));
        fill(methods, clazz.getSuperclass());
    }

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

    private String getConstructorImplementation(Executable executable) {
        var args = new StringJoiner(COMMA + SPACE);
        Arrays.stream(executable.getParameters()).forEach(param -> args.add(param.getName()));
        return TAB + SUPER + OPEN + args.toString() + CLOSE + COLON;
    }

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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteDirectory(Path path) throws IOException {
        Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

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
     *  <li> 2 arguments: <tt>className rootPath</tt> - runs {@link #implement(Class, Path)} with given arguments</li>
     *  <li> 3 arguments: <tt>-jar className jarPath</tt> - runs {@link #implementJar(Class, Path)} with two second arguments</li>
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