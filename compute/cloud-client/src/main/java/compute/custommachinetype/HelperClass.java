/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package compute.custommachinetype;

// [START compute_custom_machine_type_helper_class]

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class HelperClass {

  /*
   * This class allows you to create custom machine types to be used with the VM instances.
   */

  static class CustomMachineType {
    String zone;
    String cpuSeries;
    int memory;
    int coreCount;
    TypeLimits typeLimit;

    CustomMachineType(String zone, String cpuSeries, int memory, int coreCount,
        TypeLimits typeLimit) {
      this.zone = zone;
      this.cpuSeries = cpuSeries;
      this.memory = memory;
      this.coreCount = coreCount;
      // Shared machine types (e2-small, e2-medium and e2-micro) always have
      // 2 vCPUs: https://cloud.google.com/compute/docs/general-purpose-machines#e2_limitations
      this.typeLimit = typeLimit;
    }

    @Override
    public String toString() {
      if (cpuSeries.equalsIgnoreCase(CpuSeries.E2_SMALL.cpuSeries)
          || cpuSeries.equalsIgnoreCase(CpuSeries.E2_MICRO.cpuSeries)
          || cpuSeries.equalsIgnoreCase(CpuSeries.E2_MEDIUM.cpuSeries)) {
        return String.format("zones/%s/machineTypes/%s-%d", zone, cpuSeries, memory);
      }

      if (memory > typeLimit.maxMemPerCore * coreCount && typeLimit.allowExtraMemory) {
        return String.format("zones/%s/machineTypes/%s-%d-%d-ext", zone, cpuSeries, coreCount,
            memory);
      }

      return String.format("zones/%s/machineTypes/%s-%d-%d", zone, cpuSeries, coreCount, memory);
    }

    public String shortString() {
      String cmt = this.toString();
      return cmt.substring(cmt.lastIndexOf("/") + 1);
    }
  }

  // This class defines the configurable parameters for a custom VM.
  static final class TypeLimits {

    int[] allowedCores;
    int minMemPerCore;
    int maxMemPerCore;
    int extraMemoryLimit;
    boolean allowExtraMemory;

    TypeLimits(int[] allowedCores, int minMemPerCore, int maxMemPerCore, boolean allowExtraMemory,
        int extraMemoryLimit) {
      this.allowedCores = allowedCores;
      this.minMemPerCore = minMemPerCore;
      this.maxMemPerCore = maxMemPerCore;
      this.allowExtraMemory = allowExtraMemory;
      this.extraMemoryLimit = extraMemoryLimit;
    }
  }

  public enum CpuSeries {
    N1("custom"),
    N2("n2-custom"),
    N2D("n2d-custom"),
    E2("e2-custom"),
    E2_MICRO("e2-custom-micro"),
    E2_SMALL("e2-custom-small"),
    E2_MEDIUM("e2-custom-medium");

    private static final Map<String, CpuSeries> ENUM_MAP;

    static {
      ENUM_MAP = init();
    }

    // Build an immutable map of String name to enum pairs.
    public static Map<String, CpuSeries> init() {
      Map<String, CpuSeries> map = new ConcurrentHashMap<>();
      for (CpuSeries instance : CpuSeries.values()) {
        map.put(instance.name().toLowerCase(), instance);
      }
      return Collections.unmodifiableMap(map);
    }

    private final String cpuSeries;

    CpuSeries(String cpuSeries) {
      this.cpuSeries = cpuSeries;
    }

    public static CpuSeries get(String name) {
      return ENUM_MAP.get(name.toLowerCase());
    }

    public String getCpuSeries() {
      return this.cpuSeries;
    }
  }

  // This enum correlates a machine type with its limits.
  // The limits for various CPU types are described in:
  // https://cloud.google.com/compute/docs/general-purpose-machines
  enum Limits {
    CPUSeries_E2(new TypeLimits(getNumsInRangeWithStep(2, 33, 2), 512, 8192, false, 0)),
    CPUSeries_E2MICRO(new TypeLimits(new int[]{}, 1024, 2048, false, 0)),
    CPUSeries_E2SMALL(new TypeLimits(new int[]{}, 2048, 4096, false, 0)),
    CPUSeries_E2MEDIUM(new TypeLimits(new int[]{}, 4096, 8192, false, 0)),
    CPUSeries_N2(
        new TypeLimits(concat(getNumsInRangeWithStep(2, 33, 2), getNumsInRangeWithStep(36, 129, 4)),
            512, 8192, true, gbToMb(624))),
    CPUSeries_N2D(
        new TypeLimits(new int[]{2, 4, 8, 16, 32, 48, 64, 80, 96}, 512, 8192, true, gbToMb(768))),
    CPUSeries_N1(
        new TypeLimits(concat(new int[]{1}, getNumsInRangeWithStep(2, 97, 2)), 922, 6656, true,
            gbToMb(624)));

    private final TypeLimits typeLimits;

    Limits(TypeLimits typeLimits) {
      this.typeLimits = typeLimits;
    }

    public TypeLimits getTypeLimits() {
      return typeLimits;
    }
  }

  // Returns the array of integers within the given range, incremented by the specified step.
  // start (inclusive): starting number of the range
  // stop (inclusive): ending number of the range
  // step : increment value
  static int[] getNumsInRangeWithStep(int start, int stop, int step) {
    return IntStream.range(start, stop).filter(x -> (x - start) % step == 0).toArray();
  }

  static int gbToMb(int value) {
    return value << 10;
  }

  static int[] concat(int[] a, int[] b) {
    int[] result = new int[a.length + b.length];
    System.arraycopy(a, 0, result, 0, a.length);
    System.arraycopy(b, 0, result, a.length, b.length);
    return result;
  }

  // Return the custom machine type in the form of a string acceptable by Compute Engine API.
  public static String returnCustomMachineTypeString(CustomMachineType cmt) {
    // Check if the requested CPU belongs to E2 series.
    if (Arrays.asList(CpuSeries.E2_SMALL.name(), CpuSeries.E2_MICRO.name(),
        CpuSeries.E2_MEDIUM.name()).contains(cmt.cpuSeries)) {
      return String.format("zones/%s/machineTypes/%s-%s", cmt.zone, cmt.cpuSeries, cmt.memory);
    }

    // Check if extended memory was requested.
    if (cmt.memory > cmt.coreCount * cmt.typeLimit.maxMemPerCore) {
      return String.format("zones/%s/machineTypes/%s-%s-%s-ext", cmt.zone, cmt.cpuSeries,
          cmt.coreCount,
          cmt.memory);
    }

    return String.format("zones/%s/machineTypes/%s-%s-%s", cmt.zone, cmt.cpuSeries, cmt.coreCount,
        cmt.memory);
  }

  // Returns machine type in a format without the zone. For example, n2-custom-0-10240.
  // This format is used to create instance templates.
  public static String machineType(CustomMachineType cmt) {
    String[] machineType = returnCustomMachineTypeString(cmt).split("/");
    return machineType[machineType.length - 1];
  }

  // Validate whether the requested parameters are allowed.
  // Find more information about limitations of custom machine types at:
  // https://cloud.google.com/compute/docs/general-purpose-machines#custom_machine_types
  public static String validate(CustomMachineType cmt) {

    // Check the number of cores and if the coreCount is present in allowedCores.
    if (cmt.typeLimit.allowedCores.length > 0 && Arrays.stream(cmt.typeLimit.allowedCores)
        .noneMatch(x -> x == cmt.coreCount)) {
      throw new Error(String.format(
          "Invalid number of cores requested. Allowed number of cores for %s is: %s",
          cmt.cpuSeries,
          Arrays.toString(cmt.typeLimit.allowedCores)));
    }

    // Memory must be a multiple of 256 MB.
    if (cmt.memory % 256 != 0) {
      throw new Error("Requested memory must be a multiple of 256 MB");
    }

    // Check if the requested memory isn't too little.
    if (cmt.memory < cmt.coreCount * cmt.typeLimit.minMemPerCore) {
      throw new Error(
          String.format("Requested memory is too low. Minimum memory for %s is %s MB per core",
              cmt.cpuSeries, cmt.typeLimit.minMemPerCore));
    }

    // Check if the requested memory isn't too much.
    if (cmt.memory > cmt.coreCount * cmt.typeLimit.maxMemPerCore
        && !cmt.typeLimit.allowExtraMemory) {
      throw new Error(String.format(
          "Requested memory is too large.. Maximum memory allowed for %s is %s MB per core",
          cmt.cpuSeries, cmt.typeLimit.extraMemoryLimit));
    }

    // Check if the requested memory isn't too large.
    if (cmt.memory > cmt.typeLimit.extraMemoryLimit && cmt.typeLimit.allowExtraMemory) {
      throw new Error(
          String.format("Requested memory is too large.. Maximum memory allowed for %s is %s MB",
              cmt.cpuSeries, cmt.typeLimit.extraMemoryLimit));
    }

    return null;
  }

  // Create a custom machine type.
  public static CustomMachineType createCustomMachineType(String zone, String cpuSeries, int memory,
      int coreCount, TypeLimits typeLimit) {
    if (Arrays.asList(CpuSeries.E2_SMALL.getCpuSeries(), CpuSeries.E2_MICRO.getCpuSeries(),
        CpuSeries.E2_MEDIUM.getCpuSeries()).contains(cpuSeries)) {
      coreCount = 2;
    }

    CustomMachineType cmt = new CustomMachineType(zone, cpuSeries, memory, coreCount, typeLimit);

    try {
      validate(cmt);
    } catch (Error e) {
      // Error in validation.
      System.out.printf("Error in validation: %s", e);
      return null;
    }
    return cmt;
  }

}
// [END compute_custom_machine_type_helper_class]