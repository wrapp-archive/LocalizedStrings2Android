package com.wrapp.localization;

import java.io.*;

public class LocalizedStrings2Android {
  private static class LocalizedStringsConverter {
    public void convertLocalizationFiles(String localizedStringsRoot, String androidProjectRoot) {
      File androidProjectResPath = new File(new File(androidProjectRoot), "res");
      File localizedStringsRootPath = new File(localizedStringsRoot);
      for(String childFile : localizedStringsRootPath.list()) {
        if(childFile.contains(".lproj")) {
          File localeDirectory = new File(localizedStringsRootPath, childFile);
          File localizedStringsFile = new File(localeDirectory, "Localizable.strings");

          final String[] childFileParts = childFile.split("\\.");
          String localeName = childFileParts[0];
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

          File androidOutputStringsFile = new File(outputValuesPath, "strings.xml");
          convertLocalizationFile(localizedStringsFile, androidOutputStringsFile);
        }
      }
    }

    private void convertLocalizationFile(File localizedStringsFile, File androidOutputStringsFile) {
      InputStreamReader inputStreamReader = null;
      BufferedReader bufferedReader = null;
      OutputStreamWriter outputStreamWriter = null;
      BufferedWriter bufferedWriter = null;

      try {
        inputStreamReader = new InputStreamReader(new FileInputStream(localizedStringsFile), "UTF-16");
        bufferedReader = new BufferedReader(inputStreamReader);
        outputStreamWriter = new OutputStreamWriter(new FileOutputStream(androidOutputStringsFile), "UTF-8");
        bufferedWriter = new BufferedWriter(outputStreamWriter);

        bufferedWriter.write("<resources>\n");
        boolean inComment = false;
        while(true) {
          String line = bufferedReader.readLine();
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
          final String[] lineParts = line.split("\\s=\\s");
          // Get rid of the first quotation mark, keep everything except the last quote and the semicolon
          final String value = lineParts[1].substring(1, lineParts[1].length() - 2);
          final String escapedValueString = value.replace("'", "\\'");
          // Keep the quotes for the key string, though
          bufferedWriter.write("  <string name=" + lineParts[0] + ">" + escapedValueString + "</string>\n");
        }

        bufferedWriter.write("</resources>\n");
        bufferedWriter.flush();
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

  }
}
