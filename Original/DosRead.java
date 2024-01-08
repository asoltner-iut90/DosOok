
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
      audio = new double[dataSize / (bitsPerSample / 8)];
      int numSamples = 0;
      int bytesPerSample = (bitsPerSample / 8);
      for (int i = 0; i < audioData.length - bytesPerSample + 1; i += bytesPerSample) {
          short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
          
          //byte[] byteSample = { audioData[i+1], audioData[i]};
          //double sample = byteArrayToInt(byteSample, 0, bitsPerSample);
          audio[numSamples] = sample;
          numSamples++;
      }
  }


        
    

    /**
     * Reverse the negative values of the audio array
     */
    public void audioRectifier(){
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
        for(int i=0; i<audio.length; i++){
          double s = 0;
          int c = 0;
          for(int j = i-n; j< i+n+1;j++){
              if(j>=0 && j<audio.length){
                c++;
                s+=audio[j]*(1.0/(2*n));
              }
          }
          audio[i] = s;
      }
    }

    /**
     * Resample the audio array and apply a threshold
     * @param period the number of audio samples by symbol
     * @param threshold the threshold that separates 0 and 1
     */
    public void audioResampleAndThreshold(int period, int threshold){
        outputBits = new int[audio.length/period];
        System.out.print(outputBits.length);
        for(int i=0; i<outputBits.length; i++){
            if(audio[i*period]<threshold){
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
        for(int i = 0; i < outputBits.length; i+=8){
            for(int j = i; j < i+8; j++){
                System.out.println(outputBits[j]);
            }
        }
    }

    /**
     * Print the elements of an array
     * @param data the array to print
     */
    public static void printIntArray(char[] data) {
      /*
        À compléter
      */
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
        StdDraw.setCanvasSize(1000,300);
        StdDraw.setXscale(start, stop);
        StdDraw.setYscale(-32768,32768);
        float max = 0,min = 0;
        /*
        for(i=start; i<stop;i=i+(stop-start)/10){
            if 
        }*/
        int i;
        for(i=start; i<stop;i=i+(stop-start)/10){
            StdDraw.text(i, 0, String.valueOf(i));
        }
        for(i=start+1;i<stop; i++){
            StdDraw.line(i-1, sig[i-1], i, sig[i]);
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
        dosRead.audioLPFilter(44);

        // Resample audio data and apply a threshold to output only 0 & 1
        dosRead.audioResampleAndThreshold(dosRead.sampleRate/BAUDS, 12000 );

        dosRead.decodeBitsToChar();
        if (dosRead.decodedChars != null){
            System.out.print("Message décodé : ");
            printIntArray(dosRead.decodedChars);
        }

        //displaySig(dosRead.audio, 0, dosRead.audio.length-1, "line", "Signal audio");
        displaySig(dosRead.audio, 0, 3000, "line", "Signal audio");

        // Close the file input stream
        try {
            dosRead.fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
