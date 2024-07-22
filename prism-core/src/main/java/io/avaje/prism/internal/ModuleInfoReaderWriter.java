package io.avaje.prism.internal;

import java.io.PrintWriter;

public class ModuleInfoReaderWriter {
  private ModuleInfoReaderWriter() {}

  public static void write(PrintWriter out, String packageName) {

    out.append(
        "package "
            + packageName
            + ";\n"
            + "\n"
            + "import static java.util.stream.Collectors.*;\n"
            + "\n"
            + "import java.io.*;\n"
            + "import java.util.*;\n"
            + "import java.util.regex.Matcher;\n"
            + "import java.util.regex.Pattern;\n"
            + "\n"
            + "import javax.annotation.processing.Generated;\n"
            + "import javax.lang.model.element.ModuleElement;\n"
            + "import javax.lang.model.element.ModuleElement.DirectiveKind;\n"
            + "import javax.lang.model.element.ModuleElement.RequiresDirective;\n"
            + "import javax.tools.StandardLocation;\n"
            + "import javax.lang.model.element.PackageElement;\n"
            + "\n"
            + "/**\n"
            + " * Helper Class to work with an application's root module-info.\n"
            + " *\n"
            + " * <p>Calling {@link ModuleElement#getDirectives()} on the application module can break compilation\n"
            + " * in some situations, so this class helps parse the module source file and get the relevant\n"
            + " * information without breaking anything.\n"
            + " */\n"
            + "@Generated(\"avaje-prism-generator\")\n"
            + "public class ModuleInfoReader {\n"
            + "\n"
            + "  private static final String SPLIT_PATTERN = \"\\\\s*,\\\\s*\";\n"
            + "  private static final Pattern IMPORT_PATTERN = Pattern.compile(\"import\\\\s+([\\\\w.$]+);\");\n"
            + "  private static final Pattern REQUIRES_PATTERN =\n"
            + "      Pattern.compile(\"requires\\\\s+(transitive\\\\s+)?(static\\\\s+)?([\\\\w.$]+);\");\n"
            + "  private static final Pattern PROVIDES_PATTERN =\n"
            + "      Pattern.compile(\"provides\\\\s+([\\\\w.$]+)\\\\s+with\\\\s+([\\\\w.$,\\\\s]+);\");\n"
            + "  private static final Pattern OPENS_PATTERN =\n"
            + "      Pattern.compile(\"opens\\\\s+([\\\\w.$]+)\\\\s+to\\\\s+([\\\\w.$,\\\\s]+);\");\n"
            + "  private static final Pattern EXPORTS_PATTERN =\n"
            + "      Pattern.compile(\"exports\\\\s+([\\\\w.$]+)\\\\s+to\\\\s+([\\\\w.$,\\\\s]+);\");\n"
            + "  private static final Pattern USES_PATTERN = Pattern.compile(\"uses\\\\s+([\\\\w.$]+);\");\n"
            + "\n"
            + "  private final List<Requires> requires = new ArrayList<>();\n"
            + "  private final List<Uses> uses = new ArrayList<>();\n"
            + "  private final List<Exports> exports = new ArrayList<>();\n"
            + "  private final List<Opens> opens = new ArrayList<>();\n"
            + "  private final List<Provides> provides = new ArrayList<>();\n"
            + "  private final ModuleElement moduleElement;\n"
            + "\n"
            + "  /**\n"
            + "   * Parse a module-info and create a new instance\n"
            + "   *\n"
            + "   * @param moduleElement the element representing the root module\n"
            + "   * @param reader a reader for the contents of the module-info.java\n"
            + "   */\n"
            + "  public ModuleInfoReader(ModuleElement moduleElement, BufferedReader reader) throws IOException {\n"
            + "    this(moduleElement, reader.lines().collect(joining(\"\\n\")));\n"
            + "    reader.close();\n"
            + "  }\n"
            + "\n"
            + "  /**\n"
            + "   * Parse a module-info and create a new instance\n"
            + "   *\n"
            + "   * @param moduleElement the element representing the root module\n"
            + "   * @param moduleString a string containing the contents of the module-info.java\n"
            + "   */\n"
            + "  public ModuleInfoReader(ModuleElement moduleElement, CharSequence moduleString) {\n"
            + "    this.moduleElement = moduleElement;\n"
            + "    Matcher importMatcher = IMPORT_PATTERN.matcher(moduleString);\n"
            + "    Matcher requiresMatcher = REQUIRES_PATTERN.matcher(moduleString);\n"
            + "    Matcher providesMatcher = PROVIDES_PATTERN.matcher(moduleString);\n"
            + "    Matcher opensMatcher = OPENS_PATTERN.matcher(moduleString);\n"
            + "    Matcher exportsMatcher = EXPORTS_PATTERN.matcher(moduleString);\n"
            + "    Matcher usesMatcher = USES_PATTERN.matcher(moduleString);\n"
            + "\n"
            + "    while (requiresMatcher.find()) {\n"
            + "      boolean transitive = requiresMatcher.group(1) != null;\n"
            + "      boolean isStatic = requiresMatcher.group(2) != null;\n"
            + "      String dep = requiresMatcher.group(3);\n"
            + "      requires.add(new Requires(APContext.elements().getModuleElement(dep), transitive, isStatic));\n"
            + "    }\n"
            + "\n"
            + "    while (opensMatcher.find()) {\n"
            + "      String packageName = opensMatcher.group(1);\n"
            + "      String targets = opensMatcher.group(2);\n"
            + "      List<ModuleElement> openTargets =\n"
            + "          Arrays.stream(targets.split(SPLIT_PATTERN))\n"
            + "              .map(APContext.elements()::getModuleElement)\n"
            + "              .collect(toList());\n"
            + "      opens.add(new Opens(moduleElement, packageName, openTargets));\n"
            + "    }\n"
            + "\n"
            + "    while (exportsMatcher.find()) {\n"
            + "      String packageName = exportsMatcher.group(1);\n"
            + "      String targets = exportsMatcher.group(2);\n"
            + "      List<ModuleElement> exportTargets =\n"
            + "          Arrays.stream(targets.split(SPLIT_PATTERN))\n"
            + "              .map(APContext.elements()::getModuleElement)\n"
            + "              .collect(toList());\n"
            + "      exports.add(new Exports(moduleElement, packageName, exportTargets));\n"
            + "    }\n"
            + "\n"
            + "    var imports = new ArrayList<String>();\n"
            + "\n"
            + "    while (importMatcher.find()) {\n"
            + "      imports.add(importMatcher.group(1));\n"
            + "    }\n"
            + "\n"
            + "    while (providesMatcher.find()) {\n"
            + "      String providedInterface = resolveImport(imports, providesMatcher.group(1));\n"
            + "      String implementationClasses = providesMatcher.group(2);\n"
            + "\n"
            + "      List<String> implementations =\n"
            + "          Arrays.stream(implementationClasses.split(SPLIT_PATTERN))\n"
            + "              .map(s -> resolveImport(imports, s))\n"
            + "              .collect(toList());\n"
            + "      provides.add(new Provides(providedInterface, implementations));\n"
            + "    }\n"
            + "    while (usesMatcher.find()) {\n"
            + "      String usedInterface = resolveImport(imports, usesMatcher.group(1));\n"
            + "\n"
            + "      uses.add(new Uses(usedInterface));\n"
            + "    }\n"
            + "  }\n"
            + "\n"
            + "  private String resolveImport(List<String> imports, String providedInterface) {\n"
            + "    return imports.stream()\n"
            + "        .filter(s -> s.contains(providedInterface))\n"
            + "        .findFirst()\n"
            + "        .orElse(providedInterface)\n"
            + "        .replaceAll(\"\\\\s\", \"\");\n"
            + "  }\n"
            + "\n"
            + "  /**\n"
            + "   * Check to see whether the given module is on the module path as a non-static dependency\n"
            + "   *\n"
            + "   * @param moduleName\n"
            + "   * @return whether the given module is on the path\n"
            + "   */\n"
            + "  public boolean containsOnModulePath(String moduleName) {\n"
            + "    if (requires.isEmpty()) {\n"
            + "      return false;\n"
            + "    }\n"
            + "    var surfaceCheck =\n"
            + "        requires.stream()\n"
            + "            .filter(r -> !r.isStatic)\n"
            + "            .anyMatch(r -> r.dependency.getQualifiedName().contentEquals(moduleName));\n"
            + "\n"
            + "    if (surfaceCheck) {\n"
            + "      return true;\n"
            + "    }\n"
            + "\n"
            + "    var seen = new HashSet<String>();\n"
            + "    return requires.parallelStream()\n"
            + "        .filter(r -> !r.isStatic)\n"
            + "        .anyMatch(r -> hasNonStaticModule(moduleName, r.dependency, seen));\n"
            + "  }\n"
            + "\n"
            + "  private boolean hasNonStaticModule(String name, ModuleElement element, Set<String> seen) {\n"
            + "    if (!seen.add(element.getQualifiedName().toString())) {\n"
            + "      return false;\n"
            + "    }\n"
            + "\n"
            + "    var directives =\n"
            + "        element.getDirectives().stream()\n"
            + "            .filter(d -> d.getKind() == DirectiveKind.REQUIRES)\n"
            + "            .map(RequiresDirective.class::cast)\n"
            + "            .filter(r -> !r.isStatic())\n"
            + "            .collect(toList());\n"
            + "    if (directives.isEmpty()) {\n"
            + "      return false;\n"
            + "    }\n"
            + "    var surfaceCheck =\n"
            + "        directives.stream().anyMatch(r -> r.getDependency().getQualifiedName().contentEquals(name));\n"
            + "\n"
            + "    if (surfaceCheck) {\n"
            + "      return true;\n"
            + "    }\n"
            + "\n"
            + "    return requires.stream().anyMatch(r -> hasNonStaticModule(name, r.getDependency(), seen));\n"
            + "  }\n"
            + "\n"
            + "  private String replace$(String k) {\n"
            + "    return k.replace('$', '.');\n"
            + "  }\n"
            + "\n"
            + "  /**\n"
            + "   * Checks whether the module-info has the defined provides directive and all their implementations\n"
            + "   * Will register an error message compilation\n"
            + "   *\n"
            + "   * @param providesType the provides directive to check\n"
            + "   * @param implementations the implementations to verify the presence of\n"
            + "   */\n"
            + "  public void validateServices(String providesType, Collection<String> implementations) {\n"
            + "    if (buildPluginAvailable() || moduleElement.isUnnamed()) {\n"
            + "      return;\n"
            + "    }\n"
            + "    var implSet = new TreeSet<>(implementations);\n"
            + "    try (final var file =\n"
            + "            APContext.filer()\n"
            + "                .getResource(StandardLocation.CLASS_OUTPUT, \"\", \"META-INF/services/\" + providesType)\n"
            + "                .toUri()\n"
            + "                .toURL()\n"
            + "                .openStream();\n"
            + "        final var buffer = new BufferedReader(new InputStreamReader(file)); ) {\n"
            + "\n"
            + "      String line;\n"
            + "      while ((line = buffer.readLine()) != null) {\n"
            + "        line.replaceAll(\"\\\\s\", \"\").replace(\",\", \"\\n\").lines().forEach(implSet::add);\n"
            + "      }\n"
            + "    } catch (Exception e) {\n"
            + "      // not a critical error\n"
            + "    }\n"
            + "    final var missingImpls = implSet.stream().map(this::replace$).collect(toSet());\n"
            + "\n"
            + "    provides()\n"
            + "        .forEach(\n"
            + "            p -> {\n"
            + "              final var contract = replace$(providesType);\n"
            + "              if (!providesType.equals(contract)) {\n"
            + "                return;\n"
            + "              }\n"
            + "              var impls = p.implementations();\n"
            + "              if (missingImpls.size() > impls.size()) {\n"
            + "                return;\n"
            + "              }\n"
            + "              impls.stream().map(this::replace$).forEach(missingImpls::remove);\n"
            + "            });\n"
            + "\n"
            + "    if (!missingImpls.isEmpty()) {\n"
            + "      var message = implementations.stream().collect(joining(\", \"));\n"
            + "\n"
            + "      APContext.logError(moduleElement, \"Missing `provides %s with %s;`\", providesType, message);\n"
            + "    }\n"
            + "  }\n"
            + "\n"
            + "    private static boolean buildPluginAvailable() {\n"
            + "    return isPresent(\"avaje-plugin-exists.txt\");\n"
            + "  }\n"
            + "\n"
            + "  private static boolean isPresent(String path) {\n"
            + "    try {\n"
            + "\n"
            + "      return APContext.getBuildResource(path).toFile().exists();\n"
            + "    } catch (Exception e) {\n"
            + "      return false;\n"
            + "    }\n"
            + "  }\n"
            + "\n"
            + "  /** The requires directives associated with this module */\n"
            + "  public List<Requires> requires() {\n"
            + "    return requires;\n"
            + "  }\n"
            + "\n"
            + "  /** The uses directives associated with this module */\n"
            + "  public List<Uses> uses() {\n"
            + "    return uses;\n"
            + "  }\n"
            + "\n"
            + "  /** The exports directives associated with this module */\n"
            + "  public List<Exports> exports() {\n"
            + "    return exports;\n"
            + "  }\n"
            + "\n"
            + "  /** The opens directives associated with this module */\n"
            + "  public List<Opens> opens() {\n"
            + "    return opens;\n"
            + "  }\n"
            + "\n"
            + "  /** The provides directives associated with this module */\n"
            + "  public List<Provides> provides() {\n"
            + "    return provides;\n"
            + "  }\n"
            + "\n"
            + "  /** A dependency of a module. */\n"
            + "  public static class Requires {\n"
            + "    private final ModuleElement dependency;\n"
            + "    private final boolean isTransitive;\n"
            + "    private final boolean isStatic;\n"
            + "\n"
            + "    public Requires(ModuleElement dependency, boolean isTransitive, boolean isStatic) {\n"
            + "      this.dependency = dependency;\n"
            + "      this.isTransitive = isTransitive;\n"
            + "      this.isStatic = isStatic;\n"
            + "    }\n"
            + "\n"
            + "    /** {@return whether or not this is a static dependency} */\n"
            + "    public boolean isStatic() {\n"
            + "      return isStatic;\n"
            + "    }\n"
            + "\n"
            + "    /** {@return whether or not this is a transitive dependency} */\n"
            + "    public boolean isTransitive() {\n"
            + "      return isTransitive;\n"
            + "    }\n"
            + "\n"
            + "    /** {@return the module that is required} */\n"
            + "    public ModuleElement getDependency() {\n"
            + "      return dependency;\n"
            + "    }\n"
            + "  }\n"
            + "\n"
            + "  /** An implementation of a service provided by a module. */\n"
            + "  public static class Provides {\n"
            + "\n"
            + "    private final String type;\n"
            + "    private final List<String> impls;\n"
            + "\n"
            + "    public Provides(String type, List<String> impls) {\n"
            + "      this.type = type;\n"
            + "      this.impls = impls;\n"
            + "    }\n"
            + "\n"
            + "    /** {@return the service being provided} */\n"
            + "    public String service() {\n"
            + "      return type;\n"
            + "    }\n"
            + "\n"
            + "    /** {@return the implementations of the service being provided} */\n"
            + "    public List<String> implementations() {\n"
            + "      return impls;\n"
            + "    }\n"
            + "  }\n"
            + "\n"
            + "  /** A reference to a service used by a module. */\n"
            + "  public static class Uses {\n"
            + "    private final String service;\n"
            + "\n"
            + "    public Uses(String usedInterface) {\n"
            + "      this.service = usedInterface;\n"
            + "    }\n"
            + "\n"
            + "    /** {@return the service that is used} */\n"
            + "    public String service() {\n"
            + "      return service;\n"
            + "    }\n"
            + "  }\n"
            + "\n"
            + "  /** An opened package of a module. */\n"
            + "  public static class Opens {\n"
            + "\n"
            + "    private final ModuleElement parent;\n"
            + "\n"
            + "    private final String packageName;\n"
            + "    private final List<ModuleElement> targets;\n"
            + "\n"
            + "    public Opens(ModuleElement parent, String packageName, List<ModuleElement> targets) {\n"
            + "      this.parent = parent;\n"
            + "      this.packageName = packageName;\n"
            + "      this.targets = targets.isEmpty() ? null : targets;\n"
            + "    }\n"
            + "\n"
            + "    /** {@return the name of the package being opened} */\n"
            + "    public String packageName() {\n"
            + "      return packageName;\n"
            + "    }\n"
            + "\n"
            + "    /** {@return the package being opened} */\n"
            + "    public PackageElement getPackage() {\n"
            + "      return APContext.elements().getPackageElement(parent, packageName);\n"
            + "    }\n"
            + "\n"
            + "    /**\n"
            + "     * Returns the specific modules to which the package is being open or {@code null}, if the\n"
            + "     * package is open all modules which have readability to this module.\n"
            + "     *\n"
            + "     * @return the specific modules to which the package is being opened\n"
            + "     */\n"
            + "    public List<ModuleElement> getTargetModules() {\n"
            + "      return targets;\n"
            + "    }\n"
            + "  }\n"
            + "\n"
            + "  /** An exported package of a module. */\n"
            + "  public static class Exports {\n"
            + "\n"
            + "    private final ModuleElement parent;\n"
            + "\n"
            + "    private final String packageName;\n"
            + "    private final List<ModuleElement> targets;\n"
            + "\n"
            + "    public Exports(ModuleElement parent, String packageName, List<ModuleElement> targets) {\n"
            + "      this.parent = parent;\n"
            + "      this.packageName = packageName;\n"
            + "      this.targets = targets.isEmpty() ? null : targets;\n"
            + "    }\n"
            + "\n"
            + "    /** {@return the name of the package being exported} */\n"
            + "    public String packageName() {\n"
            + "      return packageName;\n"
            + "    }\n"
            + "\n"
            + "    /** {@return the package being exported} */\n"
            + "    public PackageElement getPackage() {\n"
            + "      return APContext.elements().getPackageElement(parent, packageName);\n"
            + "    }\n"
            + "\n"
            + "    /**\n"
            + "     * Returns the specific modules to which the package is being exported, or {@code null}, if the\n"
            + "     * package is exported to all modules which have readability to this module.\n"
            + "     *\n"
            + "     * @return the specific modules to which the package is being exported\n"
            + "     */\n"
            + "    public List<ModuleElement> getTargetModules() {\n"
            + "      return targets;\n"
            + "    }\n"
            + "  }\n"
            + "}");
  }
}
