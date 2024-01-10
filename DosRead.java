/*
 * Nom du programme: DosRead
 * Description: Programme Java pour la transmission audio de données modulées.
 * Auteurs:
 *   - Soltner Audrick
 * Date de création: 05/12/2023
 * Dernière modification: 09/01/2024
 */

import java.io.*;

public class DosRead {
    static final int FP = 1000;
    static final int BAUDS = 100;
    static final int[] START_SEQ = {1,0,1,0,1,0,1,0};
    FileInputStream fileInputStream;
    int sampleRate = 44100;
    int bitsPerSample;
    int dataSize;
    double[] audio;
    int[] outputBits;
    char[] decodedChars;

    /**
     * Constructor that opens the FIlEInputStream
     * and reads sampleRate, bitsPerSample and dataSize
     * from the header of the wav file
     * @param path the path of the wav file to read
     */
    public void readWavHeader(String path){
      byte[] header = new byte[44]; // The header is 44 bytes long
      try {
        fileInputStream= new FileInputStream(path);
        fileInputStream.read(header);
        
        //sampleRate
        byte[] byteSampleRate = {header[24],header[25],header[26],header[27]};
        sampleRate = byteArrayToInt(byteSampleRate, 0, 32);
        //dataSize
        byte[] byteDataSize = {header[40],header[41],header[42],header[43]};
        dataSize = byteArrayToInt(byteDataSize, 0, 32);
        //bitsPerSample
        byte[] byteBitsPerSample = {header[34],header[35]};
        bitsPerSample = byteArrayToInt(byteBitsPerSample, 0, 16);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method to convert a little-endian byte array to an integer
     * @param bytes the byte array to convert
     * @param offset    the offset in the byte array
     * @param fmt   the format of the integer (16 or 32 bits)
     * @return  the integer value
     */
    private static int byteArrayToInt(byte[] bytes, int offset, int fmt) {
        if (fmt == 16)
            return ((bytes[offset + 1] & 0xFF) << 8) | (bytes[offset] & 0xFF);
        else if (fmt == 32)
            return ((bytes[offset + 3] & 0xFF) << 24) |
                    ((bytes[offset + 2] & 0xFF) << 16) |
                    ((bytes[offset + 1] & 0xFF) << 8) |
                    (bytes[offset] & 0xFF);
        else return (bytes[offset] & 0xFF);
    }

    /**
     * Read the audio data from the wav file
     * and convert it to an array of doubles
     * that becomes the audio attribute
     */
 
    public void readAudioDouble(){
      byte[] audioData = new byte[dataSize];
      try {
          fileInputStream.read(audioData);
      } catch (IOException e) {
          e.printStackTrace();
      }

      //Lecture des octets
      int bytesPerSample = (bitsPerSample / 8);
      audio = new double[dataSize / bytesPerSample];

      for (int i = 0; i < audio.length; i += 1) {
          audio[i] = (short) ((audioData[i*2 + 1] << 8) | (audioData[i*2] & 0xFF));
      }
  }


        
    

    /**
     * Reverse the negative values of the audio array
     */
    public void audioRectifier(){
        //valeur absolue
        //Si le nombre est negatif, on l'inverse
      for(int i=0; i<audio.length; i++){
          if(audio[i]<0){
            audio[i] *= -1;
          }
      }
    }

    /**
     * Apply a low pass filter to the audio array
     * Fc = (1/2n)*FECH
     * @param n the number of samples to average
     */
    public void audioLPFilter(int n){
        //Pour chaque valeur on fait la moyenne de n nombre avec n/2 nombre de chaque cote

        for(int i=0; i<audio.length; i++){
          double s = 0;
          int c = 0;
          for(int j = i-n/2; j< i+n/2+1;j++){
              if(j>=0 && j<audio.length){
                c++;
                s+=audio[j];
              }
          }
          if(c==0){
            audio[i] = 0;
          }else{
            audio[i] = s/c;
          }
          
      }
    }

    /**
     * Resample the audio array and apply a threshold
     * @param period the number of audio samples by symbol
     * @param threshold the threshold that separates 0 and 1
     */
    public void audioResampleAndThreshold(int period, int threshold){
        outputBits = new int[audio.length/period];
       
        for(int i=0; i<outputBits.length; i++){
            //Si une valeur est au dessus du threshold, ce sera un 1
            if(audio[i*period+period/2]<threshold){
                outputBits[i] = 0;
            }else{
                outputBits[i] = 1;
            }
        }
    }

    /**
     * Decode the outputBits array to a char array
     * The decoding is done by comparing the START_SEQ with the actual beginning of outputBits.
     * The next first symbol is the first bit of the first char.
     */
    public void decodeBitsToChar(){
        //Detection de la squence d'intialisation
        int start = 0;
        for(int i = 0;i<outputBits.length-START_SEQ.length;i++){
            int j = 0;
            while(j<START_SEQ.length && outputBits[i+j]==START_SEQ[j]){
                j++;
            }
            if(j==START_SEQ.length){
                start=i+START_SEQ.length;
                break;
            }
        }

        //Decodage des caractères
        int n;
        decodedChars = new char[(outputBits.length-start)/8];
        
        for(int i = 0; i < decodedChars.length; i+=1){
            //conversion de l'octet en entier
            n = 0;
            for(int j = 0; j < 8; j++){
                n += outputBits[i*8+j+start]*Math.pow(2, (double) 7-j);
            }
            //conversion de l'entier en caractere
            decodedChars[i] = (char) n;
        }
    }

    /**
     * Print the elements of an array
     * @param data the array to print
     */
    public static void printIntArray(char[] data) {    
      for(int i = 0; i < data.length; i++){//parcours du tableau
            System.out.print(data[i]);
      }
      System.out.print('\n');
    }


    /**
     * Display a signal in a window
     * @param sig  the signal to display
     * @param start the first sample to display
     * @param stop the last sample to display
     * @param mode "line" or "point"
     * @param title the title of the window
     */
    public static void displaySig(double[] sig, int start, int stop, String mode, String title){

        int yMax = 0;
        int yMin = 0;
        int i;
        //Definition du plus petit et du plus grand y
        for(i=start; i<stop;i=i+(stop-start)/10){
            if(sig[i]>yMax){
                yMax = (int) sig[i];
            }
            if(sig[i]<yMin){
                yMin = (int) sig[i];
            }
        }

        //Definition du titre de la fenetre
        StdDraw.setTitle(title);
        //Definition de la taille de la fenetre
        StdDraw.setCanvasSize(1000,300);
        //Definition de l'echelle horizontale
        StdDraw.setXscale(start - (double) (stop-start)/10, stop + (double) (stop-start)/10);
        //dEFINITION DE L'ECHELLE VERTICALE
        StdDraw.setYscale(yMin - (double) (yMax-yMin)/10,yMax + (double) (yMax-yMin)/10);     
        
        //Affichage de l'echelle de valeur verticale
        for(i=yMin; i<yMax;i=i+(yMax-yMin)/10){
            StdDraw.text(100, i, String.valueOf(i));
        }
        
        //Affichage de l'echelle de valeur horizontale
        for(i=start; i<stop;i=i+(stop-start)/10){
            StdDraw.text(i, 0, String.valueOf(i));
        }

        //Verification du mode
        if(mode.equals("line")){
            //affichage des lignes
            for(i=start+1;i<stop; i++){
                StdDraw.line( (double) i-1, sig[i-1], i, sig[i]);
            }
        }else if(mode.equals("point")){
            //affichage des points
            for(i=start;i<stop; i++){
                StdDraw.point(i, sig[i]);
            }
        }

        
    }

    /**
    *  Un exemple de main qui doit pourvoir être exécuté avec les méthodes
    * que vous aurez conçues.
    */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java DosRead <input_wav_file>");
            return;
        }
        String wavFilePath = args[0];

        // Open the WAV file and read its header
        DosRead dosRead = new DosRead();
        dosRead.readWavHeader(wavFilePath);

        // Print the audio data properties
        System.out.println("Fichier audio: " + wavFilePath);
        System.out.println("\tSample Rate: " + dosRead.sampleRate + " Hz");
        System.out.println("\tBits per Sample: " + dosRead.bitsPerSample + " bits");
        System.out.println("\tData Size: " + dosRead.dataSize + " bytes");

        // Read the audio data
        dosRead.readAudioDouble();
        // reverse the negative values
        dosRead.audioRectifier();
        // apply a low pass filter
        dosRead.audioLPFilter(150);

        // Resample audio data and apply a threshold to output only 0 & 1
        dosRead.audioResampleAndThreshold(dosRead.sampleRate/BAUDS, 6650 );

        dosRead.decodeBitsToChar();
        if (dosRead.decodedChars != null){
            System.out.print("Message décodé : ");
            printIntArray(dosRead.decodedChars);
        }

        displaySig(dosRead.audio, 0, dosRead.audio.length-1, "line", "Signal audio");

        // Close the file input stream
        try {
            dosRead.fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
