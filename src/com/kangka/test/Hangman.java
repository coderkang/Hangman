package com.kangka.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Hangman implements Runnable{

  public static void main(String[] args) {
    // TODO Auto-generated method stub
    Hangman hangman = new Hangman();
    hangman.startGame();
  }
  
  @Override
  public void run() {
    startGame();
  }
  
  public void startGame() {
    final String url = "https://strikingly-hangman.herokuapp.com/game/on";
    final String startParams = "{\"playerId\":\"1010248599@qq.com\", \"action\":\"startGame\"}";
    
    try {
      //start game
      String result = sendRequest(url, startParams);
      JSONParser parser = new JSONParser();
      JSONObject returnData = (JSONObject) parser.parse(result);
      String sessionId = (String) returnData.get("sessionId");
      
      //initialize the search tree
      JSONObject tree = readTreeFile();
      if (tree==null) {
        System.out.println("Fail to initialize tree.");
        return;
      }
      
      String[] letterFreq = readLetterFrequencyFile();
      if (letterFreq==null) {
        System.out.println("Fail to initialize letter frequency table.");
        return;
      }
      
      String nextWordParams = null;
      String returnWord;
      for (int i=1; i<=80; i++) {
        nextWordParams = "{\"sessionId\": \"" + sessionId + "\",\"action\" : \"nextWord\"}";
        result = sendRequest(url, nextWordParams);
        returnData = (JSONObject) parser.parse(result);
        System.out.println("Word NO."+i);
        System.out.println("Initial Data: "+returnData);
        returnWord = (String) ((JSONObject)returnData.get("data")).get("word");
        
        JSONObject subTree = (JSONObject) tree.get(returnWord.length()+"");
        boolean win = guessWord(subTree, letterFreq, returnWord.toLowerCase(), sessionId, url, new HashSet<String>());
        
      }
      
      //check the score
      String resultParams = "{\"sessionId\": \"" + sessionId + "\",\"action\" : \"getResult\"}";
      result = sendRequest(url, resultParams);
      returnData = (JSONObject) parser.parse(result);
      Long score = (Long) ((JSONObject)returnData.get("data")).get("score");
      
      System.out.println("Score is: "+score);
      
      if (score>1300) {
        String submintParams="{\"sessionId\": \"" + sessionId + "\",\"action\" : \"submitResult\" }";
        result=sendRequest(url, submintParams);
        returnData = (JSONObject) parser.parse(result);
        System.out.println("Game Result: "+returnData);
      }
      
    } catch (ParseException e) {
      System.out.println("JSON parse error.");
      e.printStackTrace();
    }
  }
  
  public String sendRequest(String urlParam, String param) {
    OutputStream os = null;
    BufferedReader br = null;
    String result = "";
    try {
        URL url = new URL(urlParam);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("connection", "Keep-Alive");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("user-agent",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_5)");
        
        os = conn.getOutputStream();
        os.write(param.getBytes());
        os.flush();
        
        br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        String line;
        while ((line = br.readLine()) != null) {
            result += line;
        }
    } catch (IOException e) {
      System.out.println("Fail to send out the request" + e);
      e.printStackTrace();
    }
    finally {
        try {
            if (os != null) {
                os.close();
            }
            if (br != null) {
                br.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    return result;
  }
  
  public String[] readLetterFrequencyFile() {
    String[] freqTable = new String[100];
    try {
      FileInputStream fstream = new FileInputStream("letter_frequency.txt");
      BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
      String line;
      int index=0;
      while ((line=br.readLine())!=null) {
        freqTable[++index]=line;
      }
      br.close();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    
    return freqTable;
  }
  
  public JSONObject readTreeFile() {
    try {
      JSONParser parser = new JSONParser();
      return (JSONObject) parser.parse(new FileReader("tree.json"));
    } catch (IOException | ParseException e) {
      e.printStackTrace();
      return null;
    }    
  }
  
  public boolean guessWord(JSONObject tree, String[] freqTable, String word, String sessionId, String url, Set<String> excludeLetters) 
    throws ParseException {
    String c = null;
    Set<String> keys = tree.keySet();
    for (String key : keys) {
      if (key.length()>0) {
        c = key.substring(0, 1);
        break;
      }
    }
    
    //guess letter c
    String guessParams = "{\"sessionId\": \"" + sessionId + "\",\"action\" : \"guessWord\",\"guess\":\"" + c.toUpperCase() + "\"}";
    String result = sendRequest(url, guessParams);
    JSONObject returnData = (JSONObject) new JSONParser().parse(result);

    System.out.println("Guess: "+c+". Return data: "+returnData);

    String returnWord = (String) ((JSONObject)returnData.get("data")).get("word");
    Long wrongCount = (Long) ((JSONObject)returnData.get("data")).get("wrongGuessCountOfCurrentWord");
    
    if (wrongCount>=10) {
      //lose
      System.out.println("Lose.");
      return false;
    }
    
    if (returnWord.contains(c.toUpperCase())) {
      System.out.println("Find: "+c);
      
      if (!returnWord.contains("*")) {
        //Win
        System.out.println("Win.");
        return true;
      }
      
      List<Integer> indexes = getIndexes(returnWord, c.toUpperCase());
      for (int i=0; i<indexes.size(); i++) {
        if (i>0) {
          c+=(","+indexes.get(i));
        } else {
          c+=indexes.get(i);
        }
      }
      
      if (tree.containsKey(c)) {
        System.out.println("Key and Index: "+c);
        if (tree.get(c) instanceof JSONObject) {
          return guessWord((JSONObject)tree.get(c), freqTable, returnWord, sessionId, url, excludeLetters);
        } else {
          //get to the leaf node, no more letters need to guess
          return true;
        }
      } else {
        //c is match, but c's place is not in dictionary
        tryMyLuck(freqTable, returnWord, sessionId, url, excludeLetters);
      }
    } else {
      excludeLetters.add(c);
      if (tree.get("")!=null) {
        return guessWord((JSONObject)tree.get(""), freqTable, returnWord, sessionId, url, excludeLetters);
      } else {
        //not in dictionary
        tryMyLuck(freqTable, returnWord, sessionId, url, excludeLetters);
      }
    }
    
    return false;
  }
  
  public boolean tryMyLuck(String[] freqTable, String word, String sessionId, String url, Set<String> excludeLetters)
    throws ParseException {
    System.out.println("am I lucky? "+ word);
    String guessParams;
    String result;
    JSONObject returnData;
    String[] letterFreq = freqTable[word.length()].split(",");
    for (String s : letterFreq) {
      if (!word.contains(s.toUpperCase()) && !excludeLetters.contains(s)) {
        guessParams = "{\"sessionId\": \"" + sessionId + "\",\"action\" : \"guessWord\",\"guess\":\"" + s.toUpperCase() + "\"}";
        result = sendRequest(url, guessParams);
        returnData = (JSONObject) new JSONParser().parse(result);

        System.out.println("Guess: "+s+". Return data: "+returnData);

        String returnWord = (String) ((JSONObject)returnData.get("data")).get("word");
        Long wrongCount = (Long) ((JSONObject)returnData.get("data")).get("wrongGuessCountOfCurrentWord");
        if (!returnWord.contains("*")) {
          //Win
          System.out.println("Win.");
          enrichDictionary(returnWord, "new_words.txt");
          return true;
        }
        if (wrongCount>=10) {
          //lose
          System.out.println("Lose.");
          enrichDictionary(returnWord, "missing_words.txt");
          return false;
        }
        
        word = returnWord;
      }
    }
    return false;
  }
  
  public List<Integer> getIndexes(String word, String c) {
    List<Integer> indexes = new ArrayList<Integer>();
    int index = word.indexOf(c);
    while (index>=0) {
      indexes.add(index);
      index = word.indexOf(c, index+1);
    }
    
    return indexes;
  }
  
  public static void enrichDictionary(String word, String filename) {
    BufferedWriter out = null;
    try {
        out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, true)));
        out.write(word + "\r\n");
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


}
