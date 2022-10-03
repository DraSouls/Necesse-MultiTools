// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main.extern;

import org.jetbrains.java.decompiler.util.InterpreterUtil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/*/
 |  Modification made by Lyrthras <github.com/Lyrth> :
 |  set default DUMP_ORIGINAL_LINES to 1
 |  as it wasn't allowed to be set from the command line flags.
 |  Also print notice that this alternate class is being used.
/*/
public interface IFernflowerPreferences {
    @IFernflowerPreferences.Name("Remove Bridge Methods")
    @IFernflowerPreferences.Description("Removes any methods that are marked as bridge from the decompiled output.")
    String REMOVE_BRIDGE = "rbr";
    @IFernflowerPreferences.Name("Remove Synthetic Methods And Fields")
    @IFernflowerPreferences.Description("Removes any methods and fields that are marked as synthetic from the decompiled output.")
    String REMOVE_SYNTHETIC = "rsy";
    @IFernflowerPreferences.Name("Decompile Inner Classes")
    @IFernflowerPreferences.Description("Process inner classes and add them to the decompiled output.")
    String DECOMPILE_INNER = "din";
    @IFernflowerPreferences.Name("Decompile Java 4 class references")
    @IFernflowerPreferences.Description("Java 1 to Java 4 had a different class reference format. This resugars them properly.")
    String DECOMPILE_CLASS_1_4 = "dc4";
    @IFernflowerPreferences.Name("Decompile Assertions")
    @IFernflowerPreferences.Description("Decompile assert statements.")
    String DECOMPILE_ASSERTIONS = "das";
    @IFernflowerPreferences.Name("Hide Empty super()")
    @IFernflowerPreferences.Description("Hide super() calls with no parameters.")
    String HIDE_EMPTY_SUPER = "hes";
    @IFernflowerPreferences.Name("Hide Default Constructor")
    @IFernflowerPreferences.Description("Hide constructors with no parameters and no code.")
    String HIDE_DEFAULT_CONSTRUCTOR = "hdc";
    @IFernflowerPreferences.Name("Decompile Generics")
    @IFernflowerPreferences.Description("Decompile generics in variables, fields, and statements.")
    String DECOMPILE_GENERIC_SIGNATURES = "dgs";
    @IFernflowerPreferences.Name("No Exceptions In Return")
    @IFernflowerPreferences.Description("Integrate returns better in try-catch blocks.")
    String NO_EXCEPTIONS_RETURN = "ner";
    @IFernflowerPreferences.Name("Ensure synchronized ranges are complete")
    @IFernflowerPreferences.Description("If a synchronized block has a monitorenter without any corresponding monitorexit, try to deduce where one should be to ensure the synchronized is proper.")
    String ENSURE_SYNCHRONIZED_MONITOR = "esm";
    @IFernflowerPreferences.Name("Decompile Enums")
    @IFernflowerPreferences.Description("Decompile enums.")
    String DECOMPILE_ENUM = "den";
    @IFernflowerPreferences.Name("Remove reference getClass()")
    @IFernflowerPreferences.Description("obj.new Inner() or calling invoking a method on a method reference will create a synthetic getClass() call. This removes it.")
    String REMOVE_GET_CLASS_NEW = "rgn";
    @IFernflowerPreferences.Name("Keep Literals As Is")
    @IFernflowerPreferences.Description("Keep NaN, infinties, and pi values as is without resugaring them.")
    String LITERALS_AS_IS = "lit";
    @IFernflowerPreferences.Name("Represent boolean as 0/1")
    @IFernflowerPreferences.Description("The JVM represents booleans as integers 0 and 1. This decodes 0 and 1 as boolean when it makes sense.")
    String BOOLEAN_TRUE_ONE = "bto";
    @IFernflowerPreferences.Name("ASCII String Characters")
    @IFernflowerPreferences.Description("Encode non-ASCII characters in string and character literals as Unicode escapes.")
    String ASCII_STRING_CHARACTERS = "asc";
    @IFernflowerPreferences.Name("Synthetic Not Set")
    @IFernflowerPreferences.Description("Treat some known structures as synthetic even when not explicitly set.")
    String SYNTHETIC_NOT_SET = "nns";
    @IFernflowerPreferences.Name("Treat Undefined Param Type As Object")
    @IFernflowerPreferences.Description("Treat nameless types as java.lang.Object.")
    String UNDEFINED_PARAM_TYPE_OBJECT = "uto";
    @IFernflowerPreferences.Name("Use LVT Names")
    @IFernflowerPreferences.Description("Use LVT names for local variables and parameters instead of var<index>_<version>.")
    String USE_DEBUG_VAR_NAMES = "udv";
    @IFernflowerPreferences.Name("Use Method Parameters")
    @IFernflowerPreferences.Description("Use method parameter names, as given in the MethodParameters attribute.")
    String USE_METHOD_PARAMETERS = "ump";
    @IFernflowerPreferences.Name("Remove Empty try-catch blocks")
    @IFernflowerPreferences.Description("Remove try-catch blocks with no code.")
    String REMOVE_EMPTY_RANGES = "rer";
    @IFernflowerPreferences.Name("Decompile Finally")
    @IFernflowerPreferences.Description("Decompile finally blocks.")
    String FINALLY_DEINLINE = "fdi";
    @IFernflowerPreferences.Name("Resugar Intellij IDEA @NotNull")
    @IFernflowerPreferences.Description("Resugar Intellij IDEA's code generated by @NotNull annotations.")
    String IDEA_NOT_NULL_ANNOTATION = "inn";
    @IFernflowerPreferences.Name("Decompile Lambdas as Anonymous Classes")
    @IFernflowerPreferences.Description("Decompile lambda expressions as anonymous classes.")
    String LAMBDA_TO_ANONYMOUS_CLASS = "lac";
    @IFernflowerPreferences.Name("Bytecode to Source Mapping")
    @IFernflowerPreferences.Description("Map Bytecode to source lines.")
    String BYTECODE_SOURCE_MAPPING = "bsm";
    @IFernflowerPreferences.Name("Dump Code Lines")
    @IFernflowerPreferences.Description("Dump line mappings to output archive zip entry extra data")
    String DUMP_CODE_LINES = "dcl";
    @IFernflowerPreferences.Name("Ignore Invalid Bytecode")
    @IFernflowerPreferences.Description("Ignore bytecode that is malformed.")
    String IGNORE_INVALID_BYTECODE = "iib";
    @IFernflowerPreferences.Name("Verify Anonymous Classes")
    @IFernflowerPreferences.Description("Verify that anonymous classes are local.")
    String VERIFY_ANONYMOUS_CLASSES = "vac";
    @IFernflowerPreferences.Name("Ternary Constant Simplification")
    @IFernflowerPreferences.Description("Fold branches of ternary expressions that have boolean true and false constants.")
    String TERNARY_CONSTANT_SIMPLIFICATION = "tcs";
    @IFernflowerPreferences.Name("Pattern Matching")
    @IFernflowerPreferences.Description("Decompile with if and switch pattern matching enabled.")
    String PATTERN_MATCHING = "pam";
    @IFernflowerPreferences.Name("[Experimental] Try-Loop fix")
    @IFernflowerPreferences.Description("Code with a while loop inside of a try-catch block sometimes is malformed. This attempts to fix it, but may cause other issues.")
    String EXPERIMENTAL_TRY_LOOP_FIX = "tlf";
    @IFernflowerPreferences.Name("[Experimental] Ternary In If Conditions")
    @IFernflowerPreferences.Description("Tries to collapse if statements that have a ternary in their condition.")
    String TERNARY_CONDITIONS = "tco";
    @IFernflowerPreferences.Name("Decompile Switch Expressions")
    @IFernflowerPreferences.Description("Decompile switch expressions in modern Java class files.")
    String SWITCH_EXPRESSIONS = "swe";
    @IFernflowerPreferences.Name("[Debug] Show hidden statements")
    @IFernflowerPreferences.Description("Display code blocks hidden, for debugging purposes")
    String SHOW_HIDDEN_STATEMENTS = "shs";
    @IFernflowerPreferences.Name("Override Annotation")
    @IFernflowerPreferences.Description("Display override annotations for methods known to the decompiler.")
    String OVERRIDE_ANNOTATION = "ovr";
    @IFernflowerPreferences.Name("Second-Pass Stack Simplficiation")
    @IFernflowerPreferences.Description("Simplify variables across stack bounds to resugar complex statements.")
    String SIMPLIFY_STACK_SECOND_PASS = "ssp";
    @IFernflowerPreferences.Name("Include Entire Classpath")
    @IFernflowerPreferences.Description("Give the decompiler information about every jar on the classpath.")
    String INCLUDE_ENTIRE_CLASSPATH = "iec";
    @IFernflowerPreferences.Name("Include Java Runtime")
    @IFernflowerPreferences.Description("Give the decompiler information about the Java runtime.")
    String INCLUDE_JAVA_RUNTIME = "jrt";
    @IFernflowerPreferences.Name("Explicit Generic Arguments")
    @IFernflowerPreferences.Description("Put explicit diamond generic arguments on method calls.")
    String EXPLICIT_GENERIC_ARGUMENTS = "ega";
    @IFernflowerPreferences.Name("Inline Simple Lambdas")
    @IFernflowerPreferences.Description("Remove braces on simple, one line, lambda expressions.")
    String INLINE_SIMPLE_LAMBDAS = "isl";
    @IFernflowerPreferences.Name("Logging Level")
    @IFernflowerPreferences.Description("Logging level. Must be one of: 'info', 'debug', 'warn', 'error'.")
    String LOG_LEVEL = "log";
    @IFernflowerPreferences.Name("[DEPRECATED] Max time to process method")
    @IFernflowerPreferences.Description("Maximum time in seconds to process a method. This is deprecated, do not use.")
    String MAX_PROCESSING_METHOD = "mpm";
    @IFernflowerPreferences.Name("Rename Members")
    @IFernflowerPreferences.Description("Rename classes, fields, and methods with a number suffix to help in deobfuscation.")
    String RENAME_ENTITIES = "ren";
    @IFernflowerPreferences.Name("User Renamer Class")
    @IFernflowerPreferences.Description("Path to a class that implements IIdentifierRenamer.")
    String USER_RENAMER_CLASS = "urc";
    @IFernflowerPreferences.Name("New Line Seperator")
    @IFernflowerPreferences.Description("Character that seperates lines in the decompiled output.")
    String NEW_LINE_SEPARATOR = "nls";
    @IFernflowerPreferences.Name("Indent String")
    @IFernflowerPreferences.Description("A string of spaces or tabs that is placed for each indent level.")
    String INDENT_STRING = "ind";
    @IFernflowerPreferences.Name("Preferred line length")
    @IFernflowerPreferences.Description("Max line length before formatting is applied.")
    String PREFERRED_LINE_LENGTH = "pll";
    @IFernflowerPreferences.Name("User Renamer Class")
    @IFernflowerPreferences.Description("Path to a class that implements IIdentifierRenamer.")
    String BANNER = "ban";
    @IFernflowerPreferences.Name("Error Message")
    @IFernflowerPreferences.Description("Message to display when an error occurs in the decompiler.")
    String ERROR_MESSAGE = "erm";
    @IFernflowerPreferences.Name("Thread Count")
    @IFernflowerPreferences.Description("How many threads to use to decompile.")
    String THREADS = "thr";
    String DUMP_ORIGINAL_LINES = "__dump_original_lines__";
    String UNIT_TEST_MODE = "__unit_test_mode__";
    String LINE_SEPARATOR_WIN = "\r\n";
    String LINE_SEPARATOR_UNX = "\n";
    @IFernflowerPreferences.Name("JAD-Style Variable Naming")
    @IFernflowerPreferences.Description("Use JAD-style variable naming for local variables, instead of var<index>_<version>A.")
    String USE_JAD_VARNAMING = "jvn";
    String SKIP_EXTRA_FILES = "sef";
    String WARN_INCONSISTENT_INNER_CLASSES = "win";
    @IFernflowerPreferences.Name("Dump Bytecode On Error")
    @IFernflowerPreferences.Description("Put the bytecode in the method body when an error occurs.")
    String DUMP_BYTECODE_ON_ERROR = "dbe";
    @IFernflowerPreferences.Name("Dump Exceptions On Error")
    @IFernflowerPreferences.Description("Put the exception message in the method body when an error occurs.")
    String DUMP_EXCEPTION_ON_ERROR = "dee";
    @IFernflowerPreferences.Name("Decompiler Comments")
    @IFernflowerPreferences.Description("Sometimes, odd behavior of the bytecode or unfixable problems occur. This enables or disables the adding of those to the decompiled output.")
    String DECOMPILER_COMMENTS = "dec";
    Map<String, Object> DEFAULTS = getDefaults();


    static Map<String, Object> getDefaults() {
        System.out.println("(The IFernflowerPreferences class has been overriden to enable DUMP_ORIGINAL_LINES)");
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("rbr", "1");
        defaults.put("rsy", "1");
        defaults.put("din", "1");
        defaults.put("dc4", "1");
        defaults.put("das", "1");
        defaults.put("hes", "1");
        defaults.put("hdc", "1");
        defaults.put("dgs", "1");
        defaults.put("ner", "1");
        defaults.put("esm", "1");
        defaults.put("den", "1");
        defaults.put("rgn", "1");
        defaults.put("lit", "0");
        defaults.put("bto", "1");
        defaults.put("asc", "0");
        defaults.put("nns", "0");
        defaults.put("uto", "1");
        defaults.put("udv", "1");
        defaults.put("ump", "1");
        defaults.put("rer", "1");
        defaults.put("fdi", "1");
        defaults.put("inn", "1");
        defaults.put("lac", "0");
        defaults.put("bsm", "0");
        defaults.put("dcl", "0");
        defaults.put("iib", "0");
        defaults.put("vac", "0");
        defaults.put("tcs", "0");
        defaults.put("ovr", "1");
        defaults.put("pam", "1");
        defaults.put("tlf", "0");
        defaults.put("tco", "0");
        defaults.put("swe", "1");
        defaults.put("shs", "0");
        defaults.put("ssp", "1");
        defaults.put("iec", "0");
        defaults.put("jrt", "0");
        defaults.put("ega", "0");
        defaults.put("isl", "1");
        defaults.put("log", IFernflowerLogger.Severity.INFO.name());
        defaults.put("mpm", "0");
        defaults.put("ren", "0");
        defaults.put("nls", InterpreterUtil.IS_WINDOWS ? "0" : "1");
        defaults.put("ind", "   ");
        defaults.put("pll", "160");
        defaults.put("ban", "");
        defaults.put("erm", "Please report this to the Quiltflower issue tracker, at https://github.com/QuiltMC/quiltflower/issues with a copy of the class file (if you have the rights to distribute it!)");
        defaults.put("__unit_test_mode__", "0");
        defaults.put("__dump_original_lines__", "1");
        defaults.put("thr", String.valueOf(Runtime.getRuntime().availableProcessors()));
        defaults.put("jvn", "0");
        defaults.put("sef", "0");
        defaults.put("win", "1");
        defaults.put("dbe", "1");
        defaults.put("dee", "1");
        defaults.put("dec", "1");
        return Collections.unmodifiableMap(defaults);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Description {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Name {
        String value();
    }
}
