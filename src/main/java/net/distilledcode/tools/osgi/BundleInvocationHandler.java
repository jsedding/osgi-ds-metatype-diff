package net.distilledcode.tools.osgi;

import org.osgi.framework.Bundle;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class BundleInvocationHandler implements InvocationHandler {

    private final ImplementedMethods implementedMethods;

    public static Bundle getJarFileAsBundle(final JarFile jarFile) throws MalformedURLException {
        return (Bundle) Proxy.newProxyInstance(BundleInvocationHandler.class.getClassLoader(), new Class<?>[]{ Bundle.class }, new BundleInvocationHandler(jarFile));
    }

    private BundleInvocationHandler(final JarFile jarFile) throws MalformedURLException {
        this.implementedMethods = new ImplementedMethods(jarFile);
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        Method[] methods = ImplementedMethods.class.getDeclaredMethods();
        for (final Method m : methods) {
            if (m.getName().equals(method.getName())
                && m.getReturnType() == method.getReturnType()
                && Arrays.equals(m.getParameterTypes(), method.getParameterTypes())) {
                return m.invoke(implementedMethods, args);
            }
        }
        throw new UnsupportedOperationException(method.getName() + " is not implemented");
    }

    // this object implements selected bundle methods
    private static class ImplementedMethods {

        private JarFile jarFile;
        
        private final URL jarFileUrl;

        ImplementedMethods(final JarFile jarFile) throws MalformedURLException {
            this.jarFile = jarFile;
            this.jarFileUrl = new URL("file:" + jarFile.getName());
        }

        URL getEntry(String path) {
            try {
                JarEntry jarEntry = jarFile.getJarEntry(path);
                if (jarEntry != null) {
                    return new URL("jar:" + jarFileUrl + "!/" + path);
                }
                return null;
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        String getLocation() {
            return jarFile.getName();
        }
    }
}
