package ppanda.modular.classify;

import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@RequiredArgsConstructor
public class SafeClassLoaderWrapper {

  private final ClassLoader loader;
  private final ThreadLocal<ClassLoader> outsider = new ThreadLocal<>();

  @SneakyThrows
  public <T> T doSafely(Callable<T> operation) {
    outsider.set(Thread.currentThread().getContextClassLoader());
    try {
      Thread.currentThread().setContextClassLoader(loader);
      return operation.call();
    } finally {
      Thread.currentThread().setContextClassLoader(outsider.get());
    }
  }

  @SneakyThrows
  public <T> T callStatic(String className, String methodName,
      Class<?>[] parameterTypes, Object... args) {
    val result = doSafely(() -> loader.loadClass(className)
        .getMethod(methodName, parameterTypes)
        .invoke(null, args));

    //TODO: This is too dangerous, could cause LinkageError, or unexpected ClassCastExceptions
    // Try using either of
    // 1. https://jonnyzzz.com/blog/2016/08/29/classloader-proxy/
    // 2. https://www.genuitec.com/using-dynamic-proxies/
    return (T) result;
  }

  @SneakyThrows
  public <T> T callStatic(Class<?> clazz, String methodName,
      Class<?>[] parameterTypes, Object... args) {
    return callStatic(clazz.getName(), methodName, parameterTypes, args);
  }

}
