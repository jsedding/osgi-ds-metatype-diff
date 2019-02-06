package net.distilledcode.tools.osgi;

import org.osgi.framework.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class InvocationHandlers {

    public static Bundle getJarFileAsBundle(final JarFile jarFile) throws MalformedURLException {
        BundleImplementedMethods implementedMethods = new BundleImplementedMethods(jarFile);
        Bundle bundle = DelegatingInvocationHandler.createProxy(implementedMethods);
        implementedMethods.bundleContext = getBundleContextFromBundle(bundle);
        return bundle;
    }

    private static BundleContext getBundleContextFromBundle(final Bundle bundle) {
        BundleContextImplementedMethods implementedMethods = new BundleContextImplementedMethods(bundle);
        return DelegatingInvocationHandler.createProxy(implementedMethods);
    }

    public static class DelegatingInvocationHandler<T extends ImplementedMethods<?>> implements InvocationHandler {

        private final T implementedMethods;

        private final Method[] methods;

        public static <S, T extends ImplementedMethods<S>> S createProxy(T implementedMethods) {
            Class<S> iface = implementedMethods.getImplementedInterface();
            return iface.cast(Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{ iface },
                    new DelegatingInvocationHandler<>(implementedMethods)));
        }

        public DelegatingInvocationHandler(T implementedMethods) {
            this.implementedMethods = implementedMethods;
            this.methods = implementedMethods.getClass().getDeclaredMethods();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            for (final Method m : methods) {
                if (m.getName().equals(method.getName())
                            && m.getReturnType() == method.getReturnType()
                            && Arrays.equals(m.getParameterTypes(), method.getParameterTypes())) {
                    return m.invoke(implementedMethods, args);
                }
            }
            throw new UnsupportedOperationException(method.getReturnType().getSimpleName() + " " +
                implementedMethods.getClass().getSimpleName() + "#" + method.getName() + "(" +
                Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", ")) +
                ") is not implemented");
        }
    }

    public static abstract class ImplementedMethods<T> {

        private final Class<T> iface;

        public ImplementedMethods(Class<T> iface) {
            this.iface = iface;
        }

        public final Class<T> getImplementedInterface() {
            return iface;
        }
    }

    // this object implements selected Bundle methods
    private static class BundleImplementedMethods extends ImplementedMethods<Bundle> {

        private JarFile jarFile;

        private final URL jarFileUrl;

        private BundleContext bundleContext;

        BundleImplementedMethods(final JarFile jarFile) throws MalformedURLException {
            super(Bundle.class);
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

        String getSymbolicName() {
            return "symbolic-name";
        }

        Version getVersion() {
            return new Version(0, 0, 0);
        }

        long getBundleId() {
            return 0;
        }

        BundleContext getBundleContext() {
            return bundleContext;
        }
    }


    // this object implements selected BundleContext methods
    private static class BundleContextImplementedMethods extends ImplementedMethods<BundleContext> {

        private Bundle bundle;

        BundleContextImplementedMethods(final Bundle bundle) {
            super(BundleContext.class);
            this.bundle = bundle;
        }

        Bundle getBundle() {
            return bundle;
        }

        Filter createFilter(String filter) throws InvalidSyntaxException {
            return FrameworkUtil.createFilter(filter);
        }

        void addServiceListener(ServiceListener listener, String types) {

        }

        ServiceReference[] getServiceReferences(String a, String b) {
            return null;
        }
    }
}
