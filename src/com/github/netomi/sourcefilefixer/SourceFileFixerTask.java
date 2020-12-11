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

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SourceFileFixerTask extends DefaultTask
{
    public String inputFile;

    @InputFile
    public File getInputFile()
    {
        return getProject().file(inputFile);
    }

    @TaskAction
    public void execute()
    {
        try {
            int extensionIndex = inputFile.lastIndexOf('.');
            String tmpFile = inputFile + "-tmp" + inputFile.substring(extensionIndex);
            Files.copy(Paths.get(inputFile), Paths.get(tmpFile));
            SourceFileFixer.execute(tmpFile, inputFile);
            Files.delete(Paths.get(tmpFile));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
