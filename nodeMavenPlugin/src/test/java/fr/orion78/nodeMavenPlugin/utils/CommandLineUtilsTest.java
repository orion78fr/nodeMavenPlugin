package fr.orion78.nodeMavenPlugin.utils;

import edu.emory.mathcs.backport.java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

class CommandLineUtilsTest {
  @NotNull
  private static Stream<Arguments> standardArgsSource() {
    return Stream.of(
        Arguments.of(null, Collections.emptyList()),
        Arguments.of("", Collections.emptyList()),
        Arguments.of("-test", Collections.singletonList("-test")),
        Arguments.of("\"'\"", Collections.singletonList("'")),
        Arguments.of("'\"'", Collections.singletonList("\"")),
        Arguments.of("-test test2", Arrays.asList("-test", "test2")),
        Arguments.of("\"-test test2\"", Collections.singletonList("-test test2")),
        Arguments.of("'-test test2'", Collections.singletonList("-test test2")),
        Arguments.of("   \n  trailing    \t  whitespace  \r  ", Arrays.asList("trailing", "whitespace")),
        //TODO Arguments.of("\\\"-test test2", Arrays.asList("\"-test", "test2")),
        Arguments.of("-f 'spaces and \"quotes\"' -DtestValue=toto \"single'quote\" \"this has spaces\"",
            Arrays.asList("-f", "spaces and \"quotes\"", "-DtestValue=toto", "single'quote", "this has spaces"))
    );
  }

  @ParameterizedTest
  @MethodSource("standardArgsSource")
  void testTranslateCommandLine(@Nullable String toProcess,
                                @NotNull List<String> expected) throws IOException {
    Assertions.assertEquals(expected, CommandLineUtils.translateCommandline(toProcess));
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"", "\"test", "test\"", "'", "'test test2", "\"test'"})
  void testTranslateMalformedArgs(@NotNull String faultyArgs) {
    Assertions.assertThrows(IOException.class, () -> CommandLineUtils.translateCommandline(faultyArgs));
  }
}
