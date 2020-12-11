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
import proguard.io.*;
import proguard.util.ExtensionMatcher;

import java.io.File;

public class DataEntryFactory {

    public static DataEntryReader createDataEntryReader(String          input,
                                                        DataEntryReader reader)
    {
        boolean isJar  = input.endsWith(".jar");
        boolean isAar  = input.endsWith(".aar");
        boolean isZip  = input.endsWith(".zip");

        // Unzip any jars, if necessary.
        reader = wrapInJarReader(reader, isJar, ".jar");
        if (!isJar)
        {
            // Unzip any aars, if necessary.
            reader = wrapInJarReader(reader, isAar, ".aar");
            if (!isAar)
            {
                // Unzip any zips, if necessary.
                reader = wrapInJarReader(reader, isZip, ".zip");
            }
        }

        return reader;
    }

    private static DataEntryReader wrapInJarReader(DataEntryReader reader,
                                                   boolean         isJar,
                                                   String          jarExtension)
    {
        DataEntryReader jarReader = new JarReader(reader);

        if (isJar) {
            return jarReader;
        } else {
            // Only unzip the right type of jars.
            return new FilteredDataEntryReader(
                   new DataEntryNameFilter(
                   new ExtensionMatcher(jarExtension)),
                   jarReader,
                   reader);
        }
    }

    public static DataEntryWriter createDataEntryWriter(File file, ClassPool programClassPool)
    {
        String input = file.getName();

        boolean isJar  = input.endsWith(".jar");
        boolean isAar  = input.endsWith(".aar");
        boolean isZip  = input.endsWith(".zip");

        DataEntryWriter writer = new FixedFileWriter(file);

        writer = wrapInJarWriter(writer, isZip, ".zip");
        writer = wrapInJarWriter(writer, isAar, ".aar");
        writer = wrapInJarWriter(writer, isJar, ".jar");

        // Set up for writing out the program classes.
        writer = new ClassDataEntryWriter(programClassPool, writer);

        return writer;
    }

    private static DataEntryWriter wrapInJarWriter(DataEntryWriter writer,
                                                   boolean         isJar,
                                                   String          jarExtension)
    {
        DataEntryWriter jarWriter = new JarWriter(new ZipWriter(writer));

        if (isJar) {
            return  jarWriter;
        } else {
            return new FilteredDataEntryWriter(
                   new DataEntryParentFilter(
                   new DataEntryNameFilter(
                   new ExtensionMatcher(jarExtension))),
                   jarWriter,
                   writer);
        }
    }
}
