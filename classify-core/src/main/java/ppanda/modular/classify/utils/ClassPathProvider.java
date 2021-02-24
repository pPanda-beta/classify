package ppanda.modular.classify.utils;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

public interface ClassPathProvider {

  List<URL> getClassPath();

  static ClassPathProvider filterBy(
      UnaryOperator<Stream<Path>> filter, Comparator<Path> comparator) {
    return new FilteredClassPaths(filter, comparator);
  }

  static ClassPathProvider self() {
    return new SelfSource();
  }

  static ClassPathProvider self(Class<?> clazz) {
    return new SelfSource(clazz);
  }
}


@AllArgsConstructor
@NoArgsConstructor
class SelfSource implements ClassPathProvider {

  private Class<?> clazz = SelfSource.class;

  @Override
  public List<URL> getClassPath() {
    return singletonList(clazz.getProtectionDomain()
        .getCodeSource()
        .getLocation());
  }
}

@AllArgsConstructor
@RequiredArgsConstructor
class FilteredClassPaths implements ClassPathProvider {

  private final UnaryOperator<Stream<Path>> filter;
  private Comparator<Path> comparator = Comparator.naturalOrder();

  @Override
  public List<URL> getClassPath() {
    val jvmClassPath = Arrays.stream(
        AccessController.doPrivileged(
            (PrivilegedAction<String>) (() -> System.getProperty("java.class.path")))
            .split(File.pathSeparator)
    )
        .map(Paths::get)
        .map(Path::toAbsolutePath);
    return filter.apply(jvmClassPath)
        .sorted(comparator)
        .map(FilteredClassPaths::toUrl)
        .collect(toList());
  }

  @SneakyThrows
  private static URL toUrl(Path p) {
    return p.toUri().toURL();
  }
}


