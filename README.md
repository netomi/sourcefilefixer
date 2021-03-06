# SourceFileFixer

[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

A simple utility to fix the SourceFile attribute of class files to match the actual classname.

Why would you need something like this? When using a tool like ProGuard for processing a library, there is no
way to ensure that the SourceFile attribute is being modified to match the actual classname. This is important
when you want to retain the source file information for kept classes, but remove it in all other cases. The main
use-case for something like this is when you obfuscate a library that is consumed by others and you want to ensure
that javadoc is accessible for the public API of the library. Most IDEs are only able to correctly link the sources
or javadoc entries if the source file information is pointing to the correct source file.

This is currently not possible with ProGuard: either rename the source file attribute of *all* classes to a fixed
value, keep it as is or remove it completely. This small utility comes to the rescue, as it fixes the SourceFile
attribute of all class files to its current name which might be obfuscated as well.

The tool can easily be integrated into a gradle build like this:

```shell script
afterEvaluate {
    android.libraryVariants.forEach { variant ->
        variant.outputs.forEach { output ->
            def t = task("sourceFixer${variant.name.capitalize()}", type: com.github.netomi.sourcefilefixer.SourceFileFixerTask) {
                inputFile = output.outputFile.absolutePath
            }

            variant.assemble.finalizedBy(t)
        }
    }
}
```

This will run the fixer on all outputs of all library variants.

License
-------
Code is under the [Apache Licence v2](https://www.apache.org/licenses/LICENSE-2.0.txt).
