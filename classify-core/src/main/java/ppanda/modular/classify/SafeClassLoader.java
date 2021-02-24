package ppanda.modular.classify;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;
import ppanda.modular.classify.utils.ClassPathProvider;

// Predictive class loader with a parent last approach - 100% deterministic class loading
public class SafeClassLoader extends URLClassLoader {

  private final ClassLoader parent;
  private final Set<String> parentOnlyClasses;

  public SafeClassLoader(URL[] urls, ClassLoader parent, Stream<String> parentOnlyClasses) {
    super(urls, null);
    this.parent = parent;
    this.parentOnlyClasses = parentOnlyClasses.collect(toSet());
  }

  public SafeClassLoader(Stream<String> parentOnlyClasses, ClassPathProvider... providers) {
    this(
        stream(providers)
            .map(ClassPathProvider::getClassPath)
            .flatMap(Collection::stream)
            .toArray(URL[]::new),
        Thread.currentThread().getContextClassLoader(),
        parentOnlyClasses
    );
  }

  public SafeClassLoader(ClassPathProvider... providers) {
    this(Stream.empty(), providers);
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (parentOnlyClasses.contains(name)) {
      return parent.loadClass(name);
    }
    try {
      return super.loadClass(name, resolve);
    } catch (ClassNotFoundException e) {
      return parent.loadClass(name);
    }
  }


  public SafeClassLoaderWrapper createWrapper() {
    return new SafeClassLoaderWrapper(this);
  }
}
