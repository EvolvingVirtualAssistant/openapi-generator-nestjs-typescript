package com.eva.codegen;

import static org.openapitools.codegen.utils.StringUtils.camelize;

import com.eva.codegen.lambda.NestJsPathResolveLambda;
import io.swagger.v3.oas.models.media.Schema;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.model.*;

import java.util.*;
import java.io.File;
import org.openapitools.codegen.templating.mustache.CamelCaseLambda;
import org.openapitools.codegen.templating.mustache.LowercaseLambda;
import org.openapitools.codegen.templating.mustache.TitlecaseLambda;
import org.openapitools.codegen.templating.mustache.UppercaseLambda;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NestjsTypescriptServerGenerator extends DefaultCodegen implements CodegenConfig {

  private final Logger LOGGER = LoggerFactory.getLogger(NestjsTypescriptServerGenerator.class);
  private static final String X_DISCRIMINATOR_TYPE = "x-discriminator-value";

  // source folder where to write the files
  protected String sourceFolder = "src";
  protected String apiVersion = "1.0.0";
  protected HashSet<String> languageGenericTypes;
  protected String classEnumSeparator = ".";


  /**
   * Configures the type of generator.
   *
   * @return  the CodegenType for this generator
   * @see     org.openapitools.codegen.CodegenType
   */
  public CodegenType getTag() {
    return CodegenType.OTHER;
  }

  /**
   * Configures a friendly name for the generator.  This will be used by the generator
   * to select the library with the -g flag.
   *
   * @return the friendly name for the generator
   */
  public String getName() {
    return "nestjs-typescript-server";
  }

  /**
   * Provides an opportunity to inspect and modify operation data before the code is generated.
   */
  @Override
  public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {

    // to try debugging your code generator:
    // set a break point on the next line.
    // then debug the JUnit test called LaunchGeneratorInDebugger

    OperationsMap results = super.postProcessOperationsWithModels(objs, allModels);

    OperationMap ops = results.getOperations();
    List<CodegenOperation> opList = ops.getOperation();

    // iterate over the operation and perhaps modify something
    for(CodegenOperation co : opList){
      // example:
      // co.httpMethod = co.httpMethod.toLowerCase();
      co.returnType = toModelName(typeMapping().getOrDefault(co.returnType, co.returnType));
      co.returnBaseType = toModelName(typeMapping().getOrDefault(co.returnBaseType, co.returnBaseType));
      co.allParams.forEach(param -> {
        final String paramDataType = param.dataType;
        param.dataType = toModelName(paramDataType);

        updateParamsNestedItemsDataType(param.items);
      });
    }

    objs.getImports().forEach(imports -> System.out.println("Map:"+imports.entrySet().stream()
        .map(entry -> String.format("{key: %s, value: %s}", entry.getKey(), entry.getValue()))
        .collect(Collectors.joining(" ; "))));

    //final List<String> languagePrimitives = languageSpecificPrimitives.stream().map(String::toLowerCase).collect(Collectors.toList());
    //typeMapping.keySet().stream().map(String::toLowerCase).forEach(languagePrimitives::add);

    List<Map<String, String>> operationsImports = objs.getImports().stream()
        .map(getLanguageSpecificsImportsFilterMap())
        .filter(importMap -> !importMap.isEmpty())
        .map(importMap -> {
          String classname = importMap.get("classname");
          String modelName = toModelName(classname);
          importMap.put("classname", modelName);
          importMap.put("filename", importMap.get("import"));
          return importMap;
        })
        .collect(Collectors.toList());
    objs.put("operationsImports", operationsImports);

    return results;
  }

  private void updateParamsNestedItemsDataType(CodegenProperty items) {
    if(items == null) {
      return;
    }

    items.setDatatype(toModelName(items.getDataType()));

    updateParamsNestedItemsDataType(items.getItems());
  };

  private Function<Map<String, String>, Map<String, String>> getLanguageSpecificsImportsFilterMap() {
      final List<String> languagePrimitives = languageSpecificPrimitives.stream().map(String::toLowerCase).collect(Collectors.toList());
      typeMapping.keySet().stream().map(String::toLowerCase).forEach(languagePrimitives::add);

      return importMap -> {
          Map<String, String> newImportMap = new HashMap<>();
          final Set<String> filesToBeFiltered = importMap.entrySet().stream()
              .filter(entry -> entry.getKey().equals("classname"))
              .filter(entry -> languagePrimitives.contains(entry.getValue().toLowerCase()))
              .map(Map.Entry::getValue)
              .map(String::toLowerCase)
              .peek(value -> System.out.println("value:"+value))
              .collect(Collectors.toSet());

          importMap.entrySet().stream()
              .peek(entry -> System.out.println("entry:"+String.format("{key: %s, value: %s}", entry.getKey(), entry.getValue())))
              .filter(entry -> !filesToBeFiltered.contains(entry.getValue().toLowerCase().replace(this.modelPackage() + "/", "")))
              .peek(entry -> System.out.println("entry_after:"+String.format("{key: %s, value: %s}", entry.getKey(), entry.getValue())))
              .forEach(entry -> newImportMap.put(entry.getKey(), entry.getValue()));
          return newImportMap;
      };
  }

  @Override
  public ModelsMap postProcessModels(ModelsMap objs) {
    // process enum in models
    List<ModelMap> models = postProcessModelsEnum(objs).getModels();
    for (ModelMap mo : models) {
      CodegenModel cm = mo.getModel();
      cm.imports = new TreeSet<>(cm.imports);
      // name enum with model name, e.g. StatusEnum => Pet.StatusEnum
      for (CodegenProperty var : cm.vars) {
        if (Boolean.TRUE.equals(var.isEnum)) {
          var.datatypeWithEnum = var.datatypeWithEnum.replace(var.enumName, cm.classname + classEnumSeparator + var.enumName);
        }
      }
      if (cm.parent != null) {
        for (CodegenProperty var : cm.allVars) {
          if (Boolean.TRUE.equals(var.isEnum)) {
            var.datatypeWithEnum = var.datatypeWithEnum
                .replace(var.enumName, cm.classname + classEnumSeparator + var.enumName);
          }
        }
      }
    }
    for (ModelMap mo : models) {
      CodegenModel cm = mo.getModel();
      System.out.println("Model:"+cm.name);
      // Add additional filename information for imports
      List<Map<String,String>> tsImports = toTsImports(cm, cm.imports, objs.getImportsOrEmpty()).stream()
          .map(getLanguageSpecificsImportsFilterMap())
          .filter(importMap -> !importMap.isEmpty())
          .map(importMap -> {
              Optional.ofNullable(importMap.get("filename"))
                  .map(filePath -> filePath.replace(this.modelPackage + "/", ""))
                  .ifPresent(filePath -> importMap.put("filename", filePath));
              return importMap;
          })
          .peek(importMap -> System.out.println(importMap.entrySet().stream()
              .map(im -> String.format("{key:%s,value:%s}",im.getKey(), im.getValue()))
              .collect(Collectors.joining(";"))))
          .collect(Collectors.toList());
      mo.put("tsImports", tsImports);
    }
    return objs;
  }

  private List<Map<String, String>> toTsImports(CodegenModel cm, Set<String> modelImports, List<Map<String,String>> allImports) {
    List<Map<String, String>> tsImports = new ArrayList<>();
    Set<String> allImportsNames = allImports.stream()
        .map(imports -> imports.get("import"))
        .map(im -> im.replace(modelPackage()+"/",""))
        .collect(Collectors.toSet());
    for (String im: modelImports) {
      String modelName = toModelName(im);

      System.out.println(String.format("Import:%s ; cm.className:%s", modelName, cm.classname));
      if (!modelName.equals(cm.classname) && allImportsNames.contains(modelName)) {
        HashMap<String, String> tsImport = new HashMap<>();
        // TVG: This is used as class name in the import statements of the model file
        cm.getVars().stream()
            .forEach(var -> {
              if(im.equals(var.getDataType())) {
                var.setDatatype(modelName);
                return;
              }

              updateVarNestedItemsDataType(var.getItems(), im, modelName);
            });
        tsImport.put("classname", modelName);
        tsImport.put("filename", importMapping.getOrDefault(im, toModelImport(im)));
        System.out.println(String.format("classname:%s ; filename:%s", im, tsImport.get("filename")));
        tsImports.add(tsImport);
      }
    }
    return tsImports;
  }

  private void updateVarNestedItemsDataType(CodegenProperty items, String importName, String modelName) {
    if(items == null) {
      return;
    }

    if(importName.equals(items.getDataType())) {
      items.setDatatype(modelName);
      return;
    }
    updateVarNestedItemsDataType(items.getItems(), importName, modelName);
  }

  @Override
  public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
    Map<String, ModelsMap> result = super.postProcessAllModels(objs);

    for (ModelsMap entry : result.values()) {
      for (ModelMap mo : entry.getModels()) {
        CodegenModel cm = mo.getModel();
        if (cm.discriminator != null && cm.children != null) {
          for (CodegenModel child : cm.children) {
            this.setDiscriminatorValue(child, cm.discriminator.getPropertyName(), this.getDiscriminatorValue(child));
          }
        }
      }
    }
    return result;
  }

  /**
   * Returns human-friendly help for the generator.  Provide the consumer with help
   * tips, parameters here
   *
   * @return A string value for the help message
   */
  public String getHelp() {
    return "Generates a nestjs-typescript-server server library.";
  }

  public NestjsTypescriptServerGenerator() {
    super();

    // set the output folder here
    outputFolder = "generated-code/nestjs-typescript-server";

    apiNameSuffix = "BaseController";
    modelNameSuffix = "Dto";

    /**
     * Models.  You can write model files using the modelTemplateFiles map.
     * if you want to create one template for file, you can do so here.
     * for multiple files for model, just put another entry in the `modelTemplateFiles` with
     * a different extension
     */
    modelTemplateFiles.put(
      "model.mustache", // the template to use
      ".ts");       // the extension for each file to write

    /**
     * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
     * as with models, add multiple entries with different extensions for multiple files per
     * class
     */
    apiTemplateFiles.put(
      "api.mustache",   // the template to use
      ".ts");       // the extension for each file to write

    /**
     * Template Location.  This is the location which templates will be read from.  The generator
     * will use the resource stream to attempt to read the templates.
     */
    templateDir = "nestjs-typescript-server";

    /**
     * Api Package.  Optional, if needed, this can be used in templates
     */
    apiPackage = "api";

    /**
     * Model Package.  Optional, if needed, this can be used in templates
     */
    modelPackage = "model";

    /**
     * Reserved words.  Override this with reserved words specific to your language
     */
    reservedWords = new HashSet<String> (
      Arrays.asList(
        "sample1",  // replace with static values
        "sample2")
    );

    /**
     * Additional Properties.  These values can be passed to the templates and
     * are available in models, apis, and supporting files
     */
    additionalProperties.put("apiVersion", apiVersion);
    additionalProperties.put("lambda",
        Map.of("titlecase", new TitlecaseLambda(),
            "lowercase", new LowercaseLambda(),
            "uppercase", new UppercaseLambda(),
            "camelcase", new CamelCaseLambda(),
            "pathresolve", new NestJsPathResolveLambda()));

    /**
     * Supporting Files.  You can write single files for the generator with the
     * entire object tree available.  If the input file has a suffix of `.mustache
     * it will be processed by the template engine.  Otherwise, it will be copied
     */
    supportingFiles.add(new SupportingFile("myFile.mustache",   // the input template or file
      "",                                                       // the destination folder, relative `outputFolder`
      "myFile.sample")                                          // the output file
    );

    /**
     * Language Specific Primitives.  These types will not trigger imports by
     * the client generator
     */
    this.languageSpecificPrimitives = new HashSet(Arrays.asList("string", "String", "boolean", "Boolean", "Double", "Integer", "Long", "Float", "Object", "Array", "ReadonlyArray", "Date", "number", "bigint", "any", "File", "Error", "Map", "object", "Set"));
    this.languageGenericTypes = new HashSet(Collections.singletonList("Array"));
    this.instantiationTypes.put("array", "Array");

    /**
     * TypeMapping
     */
    this.defaultIncludes = new HashSet(Arrays.asList("double", "int", "long", "short", "char", "float", "String", "boolean", "Boolean", "Double", "Void", "Integer", "Long", "Float"));
    this.typeMapping = new HashMap();
    this.typeMapping.put("array", "Array");
    this.typeMapping.put("set", "Set");
    this.typeMapping.put("map", "Map");
    this.typeMapping.put("boolean", "boolean");
    this.typeMapping.put("string", "string");
    this.typeMapping.put("int", "number");
    this.typeMapping.put("float", "number");
    this.typeMapping.put("double", "number");
    this.typeMapping.put("number", "BigDecimal");
    this.typeMapping.put("decimal", "BigDecimal");
    this.typeMapping.put("DateTime", "Date");
    this.typeMapping.put("long", "bigint");
    this.typeMapping.put("short", "number");
    this.typeMapping.put("char", "string");
    this.typeMapping.put("object", "Object");
    this.typeMapping.put("integer", "number");
    this.typeMapping.put("ByteArray", "byte[]");
    this.typeMapping.put("binary", "File");
    this.typeMapping.put("file", "File");
    this.typeMapping.put("UUID", "UUID");
    this.typeMapping.put("URI", "URI");
    this.typeMapping.put("AnyType", "oas_any_type_not_mapped");
  }

  @Override
  public String toModelName(final String name) {
    if(StringUtils.isEmpty(name)) {
      return name;
    }

    String fullModelName = name;

    final List<String> languagePrimitives = languageSpecificPrimitives.stream()
          .map(String::toLowerCase)
          .collect(Collectors.toList());
    typeMapping.keySet().stream()
          .map(String::toLowerCase)
          .forEach(languagePrimitives::add);

    if(languagePrimitives.contains(name.toLowerCase())) {
        return name;
    }

    if(!StringUtils.isEmpty(modelNamePrefix) && !name.toLowerCase().startsWith(modelNamePrefix.toLowerCase())) {
        fullModelName = addPrefix(fullModelName, modelNamePrefix);
    }

    if(!StringUtils.isEmpty(modelNameSuffix) && !name.toLowerCase().endsWith(modelNameSuffix.toLowerCase())) {
        fullModelName = addSuffix(fullModelName, modelNameSuffix);
    }

    return toTypescriptTypeName(fullModelName, "Model");
  }

  protected String addPrefix(String name, String prefix) {
    if (!StringUtils.isEmpty(prefix)) {
      name = prefix + "_" + name;
    }
    return name;
  }

  protected String addSuffix(String name, String suffix) {
    if (!StringUtils.isEmpty(suffix)) {
      name = name + "_" + suffix;
    }

    return name;
  }

  protected String toTypescriptTypeName(final String name, String safePrefix) {
    ArrayList<String> exceptions = new ArrayList<>(Arrays.asList("\\|", " "));
    String sanName = sanitizeName(name, "(?![| ])\\W", exceptions);

    sanName = camelize(sanName);

    // model name cannot use reserved keyword, e.g. return
    // this is unlikely to happen, because we have just camelized the name, while reserved words are usually all lowercase
    if (isReservedWord(sanName)) {
      String modelName = safePrefix + sanName;
      System.out.println();
      LOGGER.warn("{} (reserved word) cannot be used as model name. Renamed to {}", sanName, modelName);
      return modelName;
    }

    // model name starts with number
    if (sanName.matches("^\\d.*")) {
      String modelName = safePrefix + sanName; // e.g. 200Response => Model200Response
      LOGGER.warn("{} (model name starts with number) cannot be used as model name. Renamed to {}", sanName,
          modelName);
      return modelName;
    }

    if (languageSpecificPrimitives.contains(sanName)) {
      String modelName = safePrefix + sanName;
      LOGGER.warn("{} (model name matches existing language type) cannot be used as a model name. Renamed to {}",
          sanName, modelName);
      return modelName;
    }

    return sanName;
  }

  @Override
  public String toModelFilename(String name) {
    // should be the same as the model name
    return toModelName(name);
  }

  /**
   * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
   * those terms here.  This logic is only called if a variable matches the reserved words
   *
   * @return the escaped term
   */
  @Override
  public String escapeReservedWord(String name) {
    return "_" + name;  // add an underscore to the name
  }

  /**
   * Location to write model files.  You can use the modelPackage() as defined when the class is
   * instantiated
   */
  public String modelFileFolder() {
    return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
  }

  /**
   * Location to write api files.  You can use the apiPackage() as defined when the class is
   * instantiated
   */
  @Override
  public String apiFileFolder() {
    return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', File.separatorChar);
  }

  @Override
  public String toModelImport(String name) {
    return "".equals(this.modelPackage()) ? name : this.modelPackage() + "/" + toModelName(name);
  }

  @Override
  public String toApiImport(String name) {
    return this.apiPackage() + "/" + name;
  }

  /**
   * override with any special text escaping logic to handle unsafe
   * characters so as to avoid code injection
   *
   * @param input String to be cleaned up
   * @return string with unsafe characters removed or escaped
   */
  @Override
  public String escapeUnsafeCharacters(String input) {
    //TODO: check that this logic is safe to escape unsafe characters to avoid code injection
    return input;
  }

  /**
   * Escape single and/or double quote to avoid code injection
   *
   * @param input String to be cleaned up
   * @return string with quotation mark removed or escaped
   */
  public String escapeQuotationMark(String input) {
    //TODO: check that this logic is safe to escape quotation mark to avoid code injection
    return input.replace("\"", "\\\"");
  }

  public String getSchemaType(Schema p) {
    String openAPIType = super.getSchemaType(p);
    if (!this.isLanguagePrimitive(openAPIType) && !this.isLanguageGenericType(openAPIType)) {
      this.applyLocalTypeMapping(openAPIType);
      return openAPIType;
    } else {
      return openAPIType;
    }
  }

  private String applyLocalTypeMapping(String type) {
    if (this.typeMapping.containsKey(type)) {
      type = (String)this.typeMapping.get(type);
    }

    return type;
  }

  private boolean isLanguagePrimitive(String type) {
    return this.languageSpecificPrimitives.contains(type);
  }

  private boolean isLanguageGenericType(String type) {
    Iterator var2 = this.languageGenericTypes.iterator();

    String genericType;
    do {
      if (!var2.hasNext()) {
        return false;
      }

      genericType = (String)var2.next();
    } while(!type.startsWith(genericType + "<"));

    return true;
  }

  public void postProcessParameter(CodegenParameter parameter) {
    super.postProcessParameter(parameter);
    parameter.dataType = this.applyLocalTypeMapping(parameter.dataType);
  }

  private void setDiscriminatorValue(CodegenModel model, String baseName, String value) {
    for (CodegenProperty prop : model.allVars) {
      if (prop.baseName.equals(baseName)) {
        prop.discriminatorValue = value;
      }
    }
    if (model.children != null) {
      final boolean newDiscriminator = model.discriminator != null;
      for (CodegenModel child : model.children) {
        this.setDiscriminatorValue(child, baseName, newDiscriminator ? value : this.getDiscriminatorValue(child));
      }
    }
  }

  private String getDiscriminatorValue(CodegenModel model) {
    return model.vendorExtensions.containsKey(X_DISCRIMINATOR_TYPE) ?
        (String) model.vendorExtensions.get(X_DISCRIMINATOR_TYPE) : model.classname;
  }

}
