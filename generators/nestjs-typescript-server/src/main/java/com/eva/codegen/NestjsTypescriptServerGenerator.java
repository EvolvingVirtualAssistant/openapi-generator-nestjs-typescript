package com.eva.codegen;

import com.eva.codegen.lambda.NestJsPathResolveLambda;
import io.swagger.v3.oas.models.media.Schema;
import org.openapitools.codegen.*;
import org.openapitools.codegen.model.*;

import java.util.*;
import java.io.File;
import org.openapitools.codegen.templating.mustache.CamelCaseLambda;
import org.openapitools.codegen.templating.mustache.LowercaseLambda;
import org.openapitools.codegen.templating.mustache.TitlecaseLambda;
import org.openapitools.codegen.templating.mustache.UppercaseLambda;

public class NestjsTypescriptServerGenerator extends DefaultCodegen implements CodegenConfig {

  // source folder where to write the files
  protected String sourceFolder = "src";
  protected String apiVersion = "1.0.0";
  protected HashSet<String> languageGenericTypes;


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
    }

    return results;
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
    this.languageSpecificPrimitives = new HashSet(Arrays.asList("string", "String", "boolean", "Boolean", "Double", "Integer", "Long", "Float", "Object", "Array", "ReadonlyArray", "Date", "number", "any", "File", "Error", "Map", "object", "Set"));
    this.languageGenericTypes = new HashSet(Collections.singletonList("Array"));
    this.instantiationTypes.put("array", "Array");

    /**
     * TypeMapping
     */
    this.defaultIncludes = new HashSet(Arrays.asList("double", "int", "long", "short", "char", "float", "String", "boolean", "Boolean", "Double", "Void", "Integer", "Long", "Float"));
    this.typeMapping = new HashMap();
    this.typeMapping.put("array", "List");
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
}
