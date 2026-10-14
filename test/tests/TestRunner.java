package tests;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;

// Runs every static method starting with "test" in the test classes
public final class TestRunner {
    private static final Class<?>[] TEST_CLASSES = {
        BoardTest.class, UpgradeTest.class, PlayManagerTest.class, BossTest.class,
        RendererSmokeTest.class,
    };

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;
        for (Class<?> testClass : TEST_CLASSES) {
            Method[] methods = testClass.getDeclaredMethods();
            Arrays.sort(methods, Comparator.comparing(Method::getName));
            for (Method method : methods) {
                if (!method.getName().startsWith("test")
                        || !Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                String name = testClass.getSimpleName() + "." + method.getName();
                try {
                    method.setAccessible(true);
                    method.invoke(null);
                    ++passed;
                    System.out.println("PASS " + name);
                } catch (InvocationTargetException e) {
                    ++failed;
                    System.out.println("FAIL " + name + ": " + e.getCause());
                    e.getCause().printStackTrace(System.out);
                } catch (IllegalAccessException e) {
                    ++failed;
                    System.out.println("FAIL " + name + ": " + e);
                }
            }
        }
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
