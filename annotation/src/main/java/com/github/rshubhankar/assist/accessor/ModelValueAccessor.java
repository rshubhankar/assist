package com.github.rshubhankar.assist.accessor;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface ModelValueAccessor<T> {
  Map<String, Function<T, Object>> getGetterValueMap();

  Map<String, BiConsumer<T, Object>> getSetterValueMap();
}
