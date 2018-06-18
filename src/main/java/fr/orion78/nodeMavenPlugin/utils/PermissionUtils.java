package fr.orion78.nodeMavenPlugin.utils;

import org.jetbrains.annotations.NotNull;

import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

public class PermissionUtils {
  /**
   * Transforms unix fs mode (e.g int 0755) to the corresponding set of PosixFilePermission
   */
  @NotNull
  public static Set<PosixFilePermission> modeToPermissionSet(int mode) {
    Set<PosixFilePermission> permissions = new HashSet<>();

    if ((mode & 1) != 0) {
      permissions.add(PosixFilePermission.OTHERS_EXECUTE);
    }
    if ((mode & (1 << 1)) != 0) {
      permissions.add(PosixFilePermission.OTHERS_WRITE);
    }
    if ((mode & (1 << 2)) != 0) {
      permissions.add(PosixFilePermission.OTHERS_READ);
    }
    if ((mode & (1 << 3)) != 0) {
      permissions.add(PosixFilePermission.GROUP_EXECUTE);
    }
    if ((mode & (1 << 4)) != 0) {
      permissions.add(PosixFilePermission.GROUP_WRITE);
    }
    if ((mode & (1 << 5)) != 0) {
      permissions.add(PosixFilePermission.GROUP_READ);
    }
    if ((mode & (1 << 6)) != 0) {
      permissions.add(PosixFilePermission.OWNER_EXECUTE);
    }
    if ((mode & (1 << 7)) != 0) {
      permissions.add(PosixFilePermission.OWNER_WRITE);
    }
    if ((mode & (1 << 8)) != 0) {
      permissions.add(PosixFilePermission.OWNER_READ);
    }

    return permissions;
  }
}
