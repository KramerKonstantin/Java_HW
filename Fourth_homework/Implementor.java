package ru.ifmo.rain.kramer.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Implementor implements Impler {

    private final static String SPACE = " ";
    private final static String TAB = "    ";
    private final static String EOLN = System.lineSeparator();

    private static String getPackage(Class<?> token) {
        StringBuilder res = new StringBuilder();
        if (!token.getPackage().getName().equals("")) {
            res.append("package" + SPACE).append(token.getPackage().getName()).append(";").append(EOLN);
        }
        res.append(EOLN);
        return res.toString();
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

    private static String getParams(Executable exec, boolean typedNeeded) {
        return Arrays.stream(exec.getParameters())
                .map(param -> (typedNeeded ? param.getType().getCanonicalName() + SPACE : "") + param.getName())
                .collect(Collectors.joining("," + SPACE, "(", ")"));
    }

    private static String getExceptions(Executable exec) {
        StringBuilder res = new StringBuilder();
        Class<?>[] exceptions = exec.getExceptionTypes();
        if (exceptions.length > 0) {
            res.append(SPACE + "throws" + SPACE);
        }
        res.append(Arrays.stream(exceptions)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining("," + SPACE))
        );
        return res.toString();
    }

    private static String getReturnTypeAndName(Executable exec) {
        return exec instanceof Method ? ((Method) exec).getReturnType().getCanonicalName() + SPACE + exec.getName() :
                ((Constructor<?>) exec).getDeclaringClass().getSimpleName() + "Impl";
    }

    private static String getBody(Executable exec) {
        return exec instanceof Method ? "return" + getDefaultValue(((Method) exec).getReturnType()) :
                "super" + getParams(exec, false);
    }

    private static String getExecutable(Executable exec) {
        StringBuilder res = new StringBuilder(TAB);
        final int mods = exec.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.NATIVE & ~Modifier.TRANSIENT;
        res.append(Modifier.toString(mods))
                .append(mods > 0 ? SPACE : "")
                .append(getReturnTypeAndName(exec))
                .append(getParams(exec, true))
                .append(getExceptions(exec))
                .append(SPACE)
                .append("{")
                .append(EOLN)
                .append(TAB + TAB)
                .append(getBody(exec))
                .append(";")
                .append(EOLN)
                .append(TAB)
                .append("}")
                .append(EOLN);
        return res.toString();
    }

    private static void implementConstructors(Class<?> token, Writer writer) throws IOException, ImplerException {
        Constructor<?>[] constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .toArray(Constructor<?>[]::new);
        if (constructors.length == 0) {
            throw new ImplerException("No non-private constructors in class");
        }
        for (Constructor<?> constructor : constructors) {
            writer.write(getExecutable(constructor));
        }
    }

    private static class CustomMethod {

        private final Method instance;

        CustomMethod(Method m) {
            instance = m;
        }

        Method getInstance() {
            return instance;
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
            return Arrays.hashCode(instance.getParameterTypes()) + instance.getReturnType().hashCode() + instance.getName().hashCode();
        }
    }

    private static void getAbstractMethods(Method[] methods, Set<CustomMethod> storage) {
        Arrays.stream(methods)
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .map(CustomMethod::new)
                .collect(Collectors.toCollection(() -> storage));
    }

    private static void implementAbstractMethods(Class<?> token, Writer writer) throws IOException {
        HashSet<CustomMethod> methods = new HashSet<>();
        getAbstractMethods(token.getMethods(), methods);
        while (token != null) {
            getAbstractMethods(token.getDeclaredMethods(), methods);
            token = token.getSuperclass();
        }
        for (CustomMethod method : methods) {
            writer.write(getExecutable(method.getInstance()));
        }
    }

    @Override
    public void implement(Class<?> token, Path output) throws ImplerException {
        if (token == null || output == null) {
            throw new ImplerException("Not-null arguments expected");
        }
        if (token.isPrimitive() || token.isArray() || Modifier.isFinal(token.getModifiers()) || token == Enum.class) {
            throw new ImplerException("Incorrect class token");
        }
        output = output.resolve(token.getPackage().getName().replace('.', File.separatorChar))
                .resolve(token.getSimpleName() + "Impl" + ".java");
        if (output.getParent() != null) {
            try {
                Files.createDirectories(output.getParent());
            } catch (IOException e) {
                throw new ImplerException("Unable to create directories for output file", e);
            }
        }
        try (Writer writer = Files.newBufferedWriter(output)) {
            writer.write(getPackage(token) + "public class " + token.getSimpleName() + "Impl" + SPACE +
                    (token.isInterface() ? "implements" : "extends") + SPACE +
                    token.getSimpleName() + SPACE + "{" + EOLN);
            if (!token.isInterface()) {
                implementConstructors(token, writer);
            }
            implementAbstractMethods(token, writer);
            writer.write("}" + EOLN);
        } catch (IOException e) {
            throw new ImplerException("Unable to write to output file", e);
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("Two arguments expected");
            return;
        } else if (args[0] == null || args[1] == null) {
            System.out.println("Two arguments must be not-null");
            return;
        }
        Impler implementor = new Implementor();
        try {
            implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
        } catch (InvalidPathException e) {
            System.out.println("Incorrect path: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Incorrect class name: " + e.getMessage());
        } catch (ImplerException e) {
            System.out.println("An error occurred during implementation: " + e.getMessage());
        }
    }
}
