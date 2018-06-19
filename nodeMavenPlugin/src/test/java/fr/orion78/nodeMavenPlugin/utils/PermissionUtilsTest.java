package fr.orion78.nodeMavenPlugin.utils;

import edu.emory.mathcs.backport.java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

class PermissionUtilsTest {
  @NotNull
  private static Stream<Arguments> permissionSource() {
    return Stream.of(
        Arguments.of(0740, Stream.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ).collect(Collectors.toSet())),
        Arguments.of(0400, Collections.singleton(OWNER_READ)),
        Arguments.of(0004, Collections.singleton(OTHERS_READ))
    );
  }

  @ParameterizedTest
  @MethodSource("permissionSource")
  void testModeToPermissions(int mode, @NotNull Set<PosixFilePermission> expected) {
    Assertions.assertEquals(expected, PermissionUtils.modeToPermissionSet(mode));
  }
}