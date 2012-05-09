/*
 * Copyright (c) 2012 Bohemian Wrappsody AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.wrapp.localization;

import java.io.*;

public class LocalizedStrings2Android {
  private static class LocalizedStringsConverter {
    public void convertLocalizationFiles(String localizedStringsRoot, String androidProjectRoot) {
      File androidProjectResPath = new File(new File(androidProjectRoot), "res");
      File localizedStringsRootPath = new File(localizedStringsRoot);
      for(String childLocaleFilename : localizedStringsRootPath.list()) {
        if(childLocaleFilename.contains(".lproj")) {
          File localeDirectory = new File(localizedStringsRootPath, childLocaleFilename);
          final String[] childLocaleFilenameParts = childLocaleFilename.split("\\.");
          String localeName = childLocaleFilenameParts[0];
          File outputValuesPath;
          // Slightly special treatment for English, as that should be the fallback language
          if(localeName.equals("en")) {
            outputValuesPath = new File(androidProjectResPath, "values");
          }
          else {
            outputValuesPath = new File(androidProjectResPath, "values-" + localeName);
          }
          if(!outputValuesPath.exists()) {
            outputValuesPath.mkdir();
          }

          for(String childFile : localeDirectory.list()) {
            final String childFileParts[] = childFile.split("\\.");
            String extension = childFileParts[1];
            if(extension.equals("strings")) {
              String inputFilename = childFileParts[0];
              String outputFilename;
              if(inputFilename.equals("Localizable")) {
                outputFilename = "strings";
              }
              else {
                outputFilename = inputFilename.toLowerCase();
              }
              File localizedStringsFile = new File(localeDirectory, childFile);
              File androidOutputStringsFile = new File(outputValuesPath, outputFilename + ".xml");
              convertLocalizationFile(localizedStringsFile, androidOutputStringsFile);
            }
            else {
              // Unknown type, skip
            }
          }
        }
      }
    }

    private void convertLocalizationFile(File localizedStringsFile, File androidOutputStringsFile) {
      InputStreamReader inputStreamReader = null;
      BufferedReader bufferedReader;
      OutputStreamWriter outputStreamWriter = null;
      BufferedWriter bufferedWriter;

      try {
        inputStreamReader = new InputStreamReader(new FileInputStream(localizedStringsFile), "UTF-8");
        bufferedReader = new BufferedReader(inputStreamReader);
        outputStreamWriter = new OutputStreamWriter(new FileOutputStream(androidOutputStringsFile), "UTF-8");
        bufferedWriter = new BufferedWriter(outputStreamWriter);

        bufferedWriter.write("<resources>\n");
        boolean inComment = false;
        int lineNumber = 0;
        int numStringsConverted = 0;
        while(true) {
          String line = bufferedReader.readLine();
          lineNumber++;
          if(line == null) {
            break;
          }
          if(line.startsWith("/*")) {
            inComment = true;
          }
          if(line.endsWith("*/")) {
            inComment = false;
            continue;
          }
          if(inComment || line.length() == 0) {
            continue;
          }
          final String[] lineParts = line.split("\\s*=\\s*");
          if(lineParts.length != 2) {
            System.err.println(localizedStringsFile.getAbsolutePath() + ":" + lineNumber + ":Could not parse line contents '" + line + "'");
            continue;
          }
          final String keyString = lineParts[0].substring(1, lineParts[0].length() - 1);
          final String convertedKeyString = convertKeyString(keyString);
          if(!isValidKeyString(convertedKeyString)) {
            System.err.println(localizedStringsFile.getAbsolutePath() + ":" + lineNumber + ":Invalid key string '" + convertedKeyString + "'");
            continue;
          }
          // Get rid of the first quotation mark, keep everything except the last quote and the semicolon
          final String value = lineParts[1].substring(1, lineParts[1].length() - 2);
          final String convertedValueString = convertValueString(value);
          final boolean skipStringFormatting = valueStringHasNamedParameters(convertedValueString);
          if(skipStringFormatting) {
            bufferedWriter.write("  <string name=\"" + convertedKeyString + "\" formatted=\"false\">" + convertedValueString + "</string>\n");
          }
          else {
            bufferedWriter.write("  <string name=\"" + convertedKeyString + "\">" + convertedValueString + "</string>\n");
          }
          numStringsConverted++;
        }

        bufferedWriter.write("</resources>\n");
        bufferedWriter.flush();
        System.out.println("Wrote " + numStringsConverted + " strings to " + androidOutputStringsFile.getAbsolutePath());
      }
      catch(FileNotFoundException e) {
        e.printStackTrace();
      }
      catch(UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      catch(IOException e) {
        e.printStackTrace();
      }
      finally {
        try {
          if(inputStreamReader != null) {
            inputStreamReader.close();
          }
        }
        catch(IOException e) {
          e.printStackTrace();
        }
        if(outputStreamWriter != null) {
          try {
            outputStreamWriter.close();
          }
          catch(IOException e) {
            e.printStackTrace();
          }
        }
      }
    }

    private String convertKeyString(String keyString) {
      return keyString
        // Replace dots with underscores
        .replace(".", "_")
        // Same with spaces
        .replace(" ", "_");
    }

    private String convertValueString(String value) {
      return value
        // Escape single quotes
        .replace("'", "\\'")
        // Replace single string
        .replace("%@", "$1%s")
        // Replace notation for multiple substrings
        .replace("$@", "%s");
    }
  }

  public static void main(String[] args) {
    if(args.length != 2) {
      printHelp();
      return;
    }
    LocalizedStringsConverter converter = new LocalizedStringsConverter();
    converter.convertLocalizationFiles(args[0], args[1]);
  }

  private static void printHelp() {
    System.out.println("Usage: LocalizedStrings2Android [Localized Strings Root] [Android Project Root]");
  }
}
