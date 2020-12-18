/*
 * Copyright (c) 2020 Thomas Neidhart
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.netomi.sourcefilefixer;

import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.ProgramClass;
import proguard.classfile.TypeConstants;
import proguard.classfile.attribute.Attribute;
import proguard.classfile.attribute.SourceFileAttribute;
import proguard.classfile.attribute.visitor.AllAttributeVisitor;
import proguard.classfile.attribute.visitor.AttributeVisitor;
import proguard.classfile.constant.Utf8Constant;
import proguard.classfile.util.ClassUtil;
import proguard.classfile.visitor.ClassPoolFiller;
import proguard.classfile.visitor.MultiClassVisitor;
import proguard.io.*;

import java.io.File;

public class SourceFileFixer {

    private static class AttributeFixer
    implements AttributeVisitor
    {
        @Override
        public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}

        @Override
        public void visitSourceFileAttribute(Clazz clazz, SourceFileAttribute sourceFileAttribute) {
            String sourceFile = clazz.getString(sourceFileAttribute.u2sourceFileIndex);

            String newSourceFile;
            String shortClassName = ClassUtil.internalShortClassName(clazz.getName());

            if (sourceFile.endsWith(".java")) {
                // java classes usually are in separate source files matching their classname.
                // inner classes are in the same file as the outer class they belong to.

                int innerClassSeparatorIndex = shortClassName.indexOf(TypeConstants.INNER_CLASS_SEPARATOR);
                if (innerClassSeparatorIndex != -1) {
                    shortClassName = shortClassName.substring(0, innerClassSeparatorIndex);
                }

                int suffixIndex = sourceFile.lastIndexOf('.');
                newSourceFile = shortClassName + sourceFile.substring(suffixIndex);
            } else if (sourceFile.endsWith(".kt")) {
                // kotlin classes might be in totally unrelated source files.
                // apply some simple heuristic: if the classname is <= 3 characters long,
                // assume its obfuscated and modify the source file, otherwise leave it as is.
                if (shortClassName.length() <= 3) {
                    newSourceFile = shortClassName;
                } else {
                    newSourceFile = sourceFile;
                }
            } else {
                // for anything else, we just replace the source file with the obfuscated classname.
                newSourceFile = shortClassName;
            }

            ((Utf8Constant) ((ProgramClass) clazz).constantPool[sourceFileAttribute.u2sourceFileIndex]).setString(newSourceFile);
        }
    }

    public static void execute(String inputFile, String outputFile){
        try {
            ClassPool classPool    = new ClassPool();
            DataEntrySource source = new FileSource(new File(inputFile));

            DataEntryReader classReader =
                new ClassFilter(
                new ClassReader(false, false, false, false, null,
                new MultiClassVisitor(
                new AllAttributeVisitor(
                new AttributeFixer()),

                new ClassPoolFiller(classPool))));

            source.pumpDataEntries(DataEntryFactory.createDataEntryReader(inputFile, classReader));

            DataEntryWriter writer = DataEntryFactory.createDataEntryWriter(new File(outputFile), classPool);
            DataEntryReader resourceCopier = new DataEntryCopier(writer);
            DataEntryReader reader = new ClassFilter(new IdleRewriter(writer), resourceCopier);

            source.pumpDataEntries(DataEntryFactory.createDataEntryReader(inputFile, reader));

            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String inputFileName  = args[0];
        String outputFileName = args[1];

        execute(inputFileName, outputFileName);
    }
}
