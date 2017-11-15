package com.kangka.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;

public class DictionaryTreeBuilder {

  public static void main(String[] args) {
    // TODO Auto-generated method stub
    
    DictionaryTreeBuilder builder = new DictionaryTreeBuilder();
    builder.buildDictionaryTree();
  }
  
  public JSONObject buildDictionaryTree() {
    //read all of the words from dictionary and group by length
    Map<Integer, Set<String>> wordsLengthMap = readFromDictionary();
    if (wordsLengthMap != null) {
      JSONObject tree = new JSONObject();
      
      for (Integer length : wordsLengthMap.keySet()) {
        Set<String> rawWordSet = wordsLengthMap.get(length);
        List<Word> wordList = new ArrayList<Word>();
        for (String word : rawWordSet) {
          wordList.add(buildWord(word));
        }
        
        List<String> excludeLetters = new ArrayList<String>();
        Object entry = buildTree(wordList, excludeLetters);
        tree.put(String.valueOf(length), entry);
        System.out.println(length + ": complete.");
      }
      
      writeToFile(tree.toJSONString(), "tree.json");
      return tree;
    }
    
    return null;
  }
  
  public void writeToFile(String val, String filename) {
    BufferedWriter out = null;
    try {
        out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, true)));
        out.write(val);
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
  }
  
  public Object buildTree(List<Word> wordList, List<String> excludeLetters) {
    //find out the max frequency letter
    String c = getMaxFrequencyLetter(wordList, excludeLetters);
    List<String> excludeLettersCopy = new ArrayList<String>(excludeLetters);
    if (c==null) {
      return 0;
    } else {
      excludeLettersCopy.add(c);
    }
    
    Map<String, List<Word>> wordsGroupByIndexes = new HashMap<String, List<Word>>();
    for (Word word : wordList) {
      if (word.getValue().indexOf(c) != -1) {
        for (String s : word.getCharIndexes()) {
          if (s.substring(0, 1).equals(c)) {
            List<Word> words = wordsGroupByIndexes.get(s);
            if (words==null) {
              words = new ArrayList<Word>();
              wordsGroupByIndexes.put(s, words);
            }
            words.add(word);
          }
        }
      }
      else {
        List<Word> words = wordsGroupByIndexes.get("");
        if (words==null) {
          words = new ArrayList<Word>();
          wordsGroupByIndexes.put("", words);
        }
        words.add(word);
      }
    }
    
    JSONObject json = new JSONObject();
    for (String wordIndex : wordsGroupByIndexes.keySet()) {
      Object entry = buildTree(wordsGroupByIndexes.get(wordIndex), excludeLettersCopy);
      json.put(wordIndex, entry);
    }
    
    return json;
  }
  
  public String getMaxFrequencyLetter(List<Word> wordList, List<String> excludeLetters) {
    int[] letterCount = new int[26];
    Arrays.fill(letterCount, 0);
    
    for (Word word : wordList) {
      for (String charIndex : word.getCharIndexes()) {
        char c = charIndex.charAt(0);
        if (excludeLetters.contains(String.valueOf(c))) {
          continue;
        }
        letterCount[(int)c-97]++;
      }
    }
    
    String maxFreqLetter=null;
    int maxCount = 0;
    for (int i=0; i<letterCount.length; i++) {
      if (letterCount[i] > maxCount) {
        maxCount = letterCount[i];
        char c = (char)(i+97);
        maxFreqLetter = String.valueOf(c);
      }
    }
    return maxFreqLetter;
  }
  
  public Map<Integer, Set<String>> readFromDictionary() {
    Map<Integer, Set<String>> rtnVal = new HashMap<Integer, Set<String>>();
    try {
      FileInputStream fstream = new FileInputStream("words.txt");
      BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
      String word;
      while ((word=br.readLine()) != null) {
        int length = word.length();
        Set<String> wordSet = rtnVal.get(length);
        if (wordSet==null) {
          wordSet = new HashSet<String>();
          rtnVal.put(length, wordSet);
        }
        wordSet.add(word.toLowerCase());
      }
      br.close();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    
    return rtnVal;
  }
  
  public Word buildWord(String str) {
    if (str!=null) {
      Word word = new Word();
      word.setValue(str);
      List<String> charIndexes = new ArrayList<String>();
      outer: for (int i=0; i<str.length(); i++) {
               String c = str.substring(i, i+1);
               for (int j=0; j<charIndexes.size(); j++) {
                 String charIndex = charIndexes.get(j);
                 if (charIndex.substring(0, 1).equals(c)) {
                    charIndexes.set(j, charIndex+","+i);
                   continue outer;
                 }
               }
               charIndexes.add(c+i);
             }
      
      word.setCharIndexes(charIndexes);
      return word;
    }
    
    return null;
  }
  
  public class Word {
    private String value;
    private List<String> charIndexes;
    
    public Word() {
    }
    
    public String getValue() {
      return value;
    }
    public void setValue(String value) {
      this.value = value;
    }

    public List<String> getCharIndexes() {
      return charIndexes;
    }

    public void setCharIndexes(List<String> charIndexes) {
      this.charIndexes = charIndexes;
    }
  }
}
