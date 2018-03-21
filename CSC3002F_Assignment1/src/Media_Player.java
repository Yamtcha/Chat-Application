/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author user
 */

import java.io.*;
import sun.audio.*;
//import java.util.Scanner;

@SuppressWarnings("restriction")
public class Media_Player implements Serializable
{
    private String filePath;
    //private InputStream get_input_file;
    //private Scanner input_scanner;

   public Media_Player(String filePath) throws IOException
   {
     //get_input_file= new FileInputStream(filePath);
     this.filePath = filePath;
   }

   public void play_audio() throws IOException
   {
     try
     {
       AudioPlayer.player.start(new AudioStream( new FileInputStream(filePath)));

      }catch (IOException e)
      {
          System.out.println(e);
      }
   }

   public static void main(String[] args) {
     try
     {
       Media_Player m = new Media_Player("");
       m.play_audio();
     }catch(IOException e){}

       }
    }
