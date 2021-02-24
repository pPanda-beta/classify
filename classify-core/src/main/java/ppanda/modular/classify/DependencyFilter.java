package ppanda.modular.classify;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;

import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.SneakyThrows;

public class DependencyFilter implements UnaryOperator<Stream<Path>> {

  private final Path dependencyFilePath;
  private final Map<String, String> jarToDep;

  public DependencyFilter(Path dependencyFilePath) {
    this.dependencyFilePath = dependencyFilePath;
    jarToDep = extractDeps(dependencyFilePath)
        .collect(toMap(d -> d[1], d -> d[0]));
  }

  @SneakyThrows
  protected Stream<String[]> extractDeps(Path dependencyFilePath) {
    return Files.readAllLines(dependencyFilePath)
        .stream()
        .map(dep -> dep.split("\\|"));
  }

  @SneakyThrows
  public static DependencyFilter fromResourceFile(String filename) {
    return new DependencyFilter(
        toPath(getResource(filename).toURI())
    );
  }

  @Override
  public Stream<Path> apply(Stream<Path> pathStream) {
    return pathStream.filter(p ->
        Files.isDirectory(p) || jarToDep.containsKey(p.getFileName().toString())
    );
  }

  @SneakyThrows
  private static URL toURL(Path path) {
    return path.toUri().toURL();
  }

  private static Path toPath(URI uri) {
    return getOrCreateFSProvider(uri)
        .getPath(uri);
  }

  private static FileSystemProvider getOrCreateFSProvider(URI uri) {
    return FileSystemProvider.installedProviders()
        .stream()
        .filter(p -> p.getScheme().equalsIgnoreCase(uri.getScheme()))
        .filter(provider -> {
          try {
            provider.getPath(uri);
            return true;
          } catch (FileSystemNotFoundException e) {
            return false;
          }
        })
        .findAny()
        .orElseGet(() -> createFs(uri).provider());
  }

  @SneakyThrows
  private static FileSystem createFs(URI uri) {
    return FileSystems.newFileSystem(uri, singletonMap("create", "true"));
  }

  public static URL getResource(String resourceName) {
    ClassLoader loader = Stream.of(
        Thread.currentThread().getContextClassLoader(),
        DependencyFilter.class.getClassLoader()
    )
        .filter(Objects::nonNull)
        .findFirst()
        .get();
    return loader.getResource(resourceName);
  }
}
