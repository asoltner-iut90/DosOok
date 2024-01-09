/*
 * Nom du programme: DosSend
 * Description: Programme Java pour la transmission audio de données modulées.
 * Auteurs:
 *   - Röthlin Gaël
 *   - Soltner Audrick
 * Date de création: 05/12/2023
 * Dernière modification: 09/01/2024
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DosSend {
    final int FECH = 44100; // fréquence d'échantillonnage
    final int FP = 1000;    // fréquence de la porteuses
    final int BAUDS = 100;  // débit en symboles par seconde
    final int FMT = 16 ;    // format des données
    final int MAX_AMP = (1<<(FMT-1))-1; // amplitude max en entier
    final int CHANNELS = 1; // nombre de voies audio (1 = mono)
    final int[] START_SEQ = {1,0,1,0,1,0,1,0}; // séquence de synchro au début
    final Scanner input = new Scanner(System.in); // pour lire le fichier texte

    long taille;                // nombre d'octets de données à transmettre
    double duree ;              // durée de l'audio
    double[] dataMod;           // données modulées
    char[] dataChar;            // données en char    
    byte[] bits;        
    FileOutputStream outStream; // flux de sortie pour le fichier .wav

    /**
     * Constructor
     * @param path  the path of the wav file to create
     */
    public DosSend(String path){
        File file = new File(path);
        try{
            outStream = new FileOutputStream(file);
        } catch (Exception e) {
            System.out.println("Erreur de création du fichier");
        }
    }

    /**
     * Write a raw 4-byte integer in little endian
     * @param octets    the integer to write
     * @param destStream  the stream to write in
     */

    public void writeLittleEndian(int octets, int taille, FileOutputStream destStream){
        char poidsFaible;
        while(taille > 0){
            poidsFaible = (char) (octets & 0xFF);
            try {
                destStream.write(poidsFaible);
            } catch (Exception e) {
                System.out.println("Erreur d'écriture");
            }
            octets = octets >> 8;
            taille--;
        }
    }

    /**
     * Create and write the header of a wav file
     *
     */
    public void writeWavHeader() {
        taille = (long) (FECH * duree);
        long nbBytes = taille * CHANNELS * FMT / 8;
        try {
            outStream.write(new byte[]{'R', 'I', 'F', 'F'});// entête RIFF
            writeLittleEndian((int) (nbBytes + 36), 4, outStream);// taille totale du fichier (en octets) - 8, car RIFF et taille sont exclus
            outStream.write(new byte[]{'W', 'A', 'V', 'E'});// format WAV
            outStream.write(new byte[]{'f', 'm', 't', ' '});// sous-entête "fmt " pour le format des données
            writeLittleEndian(16, 4, outStream);// taille du sous-entête "fmt " (16 pour PCM)
            writeLittleEndian(1, 2, outStream);// code du format audio (1 pour PCM)
            writeLittleEndian(CHANNELS, 2, outStream);// nombre de canaux
            writeLittleEndian(FECH, 4, outStream);// fréquence d'échantillonnage
            writeLittleEndian(FECH * CHANNELS * FMT / 8, 4, outStream);// débit binaire moyen (en octets par seconde)
            writeLittleEndian(CHANNELS * FMT / 8, 2, outStream);// bloc d'alignement (nombre d'octets pour un échantillon, tous canaux confondus)
            writeLittleEndian(FMT, 2, outStream);// bits par échantillon
            outStream.write(new byte[]{'d', 'a', 't', 'a'});// sous-entête "data" pour les données audio
            writeLittleEndian((int) nbBytes, 4, outStream);// taille des données audio (en octets)
        } catch (IOException e) {
            System.out.println("Erreur d'écriture de l'en-tête : " + e.getMessage());
        }
    }


    /**
     * Write the data in the wav file
     * after normalizing its amplitude to the maximum value of the format (8 bits signed)
     */
    public void writeNormalizeWavData() {
        try {
            long nbBytes = taille * CHANNELS * FMT / 8;// taille des données audio (en octets)
            outStream.write(new byte[]{'d', 'a', 't', 'a'});// entête "data" pour les données audio
            writeLittleEndian((int) nbBytes, 4, outStream);// taille des données audio (en octets)
            double maxAmplitude = getMaxAmplitude(dataMod);// normalisation et écriture des données audio dans le fichier WAV
            int dataModLength = dataMod.length;
            for (int i = 0; i < dataModLength; i++) {
                double sample = dataMod[i];
                short normalizedSample = (short) ((sample / maxAmplitude) * MAX_AMP);
                writeLittleEndian(normalizedSample, FMT / 8, outStream);
            }
            System.out.println("Écriture des données audio normalisées terminée avec succès.");
        } catch (IOException e) {
            System.out.println("Erreur d'écriture des données audio normalisées : " + e.getMessage());
        }
    }
    
    /**
     * Trouve l'amplitude maximale dans les données audio modulées.
     * @param data Les données audio modulées.
     * @return L'amplitude maximale.
     */
    private double getMaxAmplitude(double[] data) {
        double maxAmplitude = 0;
        for(int i = 0; i < data.length; i++){
            if(data[i]>maxAmplitude){
                maxAmplitude = data[i];
            }
        }
        return maxAmplitude;
    }

    /**
     * Read the text data to encode and store them into dataChar
     * @return the number of characters read
     */

     public int readTextData() {
        System.out.print("Entrez le texte à encoder : ");
        String inputText = input.nextLine();
        char[] textChars = inputText.toCharArray();// convertir le texte en tableau de caractères
        dataChar = textChars;// stocker les caractères dans dataChar
        System.out.println("Texte à encoder : " + inputText);// afficher le texte à encoder
        return dataChar.length;
    }
    


    /**
     * convert a char array to a bit array
     * @param chars
     * @return byte array containing only 0 & 1
     */
     public byte[] charToBits(char[] chars) {
        List<Byte> bitsList = new ArrayList<>();
        int charsLength = chars.length;
        for (int j = 0; j < charsLength; j++) {
            char c = chars[j];
            String binaryString = String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0');

            for (int k = 0; k < binaryString.length(); k++) {
                char bitChar = binaryString.charAt(k);
                bitsList.add((byte) (bitChar - '0'));
            }
        }

        byte[] bitsArray = new byte[bitsList.size()];// convertir la liste de bits en un tableau de bytes
        for (int i = 0; i < bitsList.size(); i++) {
            bitsArray[i] = bitsList.get(i);
        }
        return bitsArray;
    }


    /**
     * Modulate the data to send and apply the symbol throughput via BAUDS and FECH.
     * @param bits the data to modulate
     */
     public void modulateData(byte[] bits) {
        double modulationFactor = 1.0;// le facteur de modulation
        double carrierFrequency = FP;// fréquence de la porteuse
        double symbolDuration = 1.0 / BAUDS;// durée d'un symbole en secondes
        int totalSamples = (int) (FECH * duree);// nombre total d'échantillons
        dataMod = new double[totalSamples];// tableau pour stocker le signal modulé
        int currentIndex = 0;// indice pour suivre la position actuelle dans le signal modulé
        // générer le signal modulé



        int startSeqLength = START_SEQ.length;

        for (int j = 0; j < startSeqLength; j++) {
            int bit = START_SEQ[j];
            double amplitude;
        
            if (bit == 1) {
                amplitude = modulationFactor;
            } else {
                amplitude = 0.0;
            }
        
            for (int i = 0; i < FECH * symbolDuration; i++) {
                dataMod[currentIndex++] = amplitude * Math.sin(2 * Math.PI * carrierFrequency * currentIndex / FECH);
            }
        }


        int bitsLength = bits.length;

        for (int j = 0; j < bitsLength; j++) {
            byte bit = bits[j];
            double amplitude;

            if (bit == 1) {
                amplitude = modulationFactor;
            } else {
                amplitude = 0.0;
            }
        
            for (int i = 0; i < FECH * symbolDuration; i++) {
                dataMod[currentIndex++] = amplitude * Math.sin(2 * Math.PI * carrierFrequency * currentIndex / FECH);
            }
        }

    }


    /**
    * Display a signal in a window
    * @param sig   the signal to display
    * @param start the first sample to display
    * @param stop  the last sample to display
    * @param mode  "line" or "point"
    * @param title the title of the window
    */
    public static void displaySig(double[] sig, int start, int stop, String mode, String title) {
        /*StdDraw.setCanvasSize(1000,300);
        StdDraw.setXscale(start, stop);
        StdDraw.setYscale(-5,5);
        StdDraw.setTitle(title);
        int i;
        for(i=start; i<stop;i=i+(stop-start)/10){
            StdDraw.text(i, 0, String.valueOf(i));
        }
        for(i=start+1;i<stop; i++){
            StdDraw.line(i-1, sig[i-1], i, sig[i]);
        }*/
        int yMax = 1;
        int yMin = -1;
        int i;

        for(i=start; i<stop;i=i+(stop-start)/10){
            if(sig[i]>yMax){
                yMax = (int) sig[i];
            }
            if(sig[i]<yMin){
                yMin = (int) sig[i];
            }
        }


        StdDraw.setTitle(title);
        StdDraw.setCanvasSize(1000,300);
        StdDraw.setXscale(start - (double) (stop-start)/10, stop + (double) (stop-start)/10);
        StdDraw.setYscale(yMin - (double) (yMax-yMin)/10,yMax + (double) (yMax-yMin)/10);
        

        for(i=yMin; i<yMax;i=i+(yMax-yMin)/10+1){
            StdDraw.text(100, i, String.valueOf(i));
        }
        for(i=start; i<stop;i=i+(stop-start)/10+1){
            StdDraw.text(i, 0, String.valueOf(i));
        }

        //System.out.println("mode : "+mode);
        
        if(mode.equals("line")){
            
            for(i=start+1;i<stop; i++){
                StdDraw.line(i -1, sig[i-1], i, sig[i]);
            }
        }else if(mode.equals("point")){
            for(i=start;i<stop; i++){
                StdDraw.point(i, sig[i]);
            }
        }
    }

    

    /**
     * Display signals in a window using StdDraw
     * @param listOfSigs a list of the signals to display
     * @param start      the first sample to display
     * @param stop       the last sample to display
     * @param mode       "line" or "point"
     * @param title      the title of the window
     */
    public static void displaySig(List<double[]> listOfSigs, int start, int stop, String mode, String title) {
        StdDraw.clear();
        StdDraw.setCanvasSize(800, 600);
        StdDraw.setXscale(0, listOfSigs.get(0).length);
        StdDraw.setYscale(-1, 1);

        if (title != null && !title.isEmpty()) {
            // Utiliser un texte pour le titre
            StdDraw.text(listOfSigs.get(0).length / 2.0, 1.1, title);
        }

        for (double[] sig : listOfSigs) {
            if (mode.equals("line")) {
                for (int i = start + 1; i <= stop; i++) {
                    StdDraw.line(i - 1, sig[i - 1], i, sig[i]);
                }
            } else if (mode.equals("point")) {
                for (int i = start; i <= stop; i++) {
                    StdDraw.point(i, sig[i]);
                }
            }
        }

        StdDraw.show();
    }
    
    public static void printIntArray(double[] data) {
        System.out.print('[');
        for(int i = 0; i < data.length; i++){
              if(i>0){
                  System.out.print(',');
              } 
              System.out.print(data[i]);
        }
        System.out.println(']');
      }
    
    public static void main(String[] args) {
        DosSend dosSend = new DosSend("DosOok_message.wav");
        dosSend.duree = (double) (dosSend.readTextData() + dosSend.START_SEQ.length / 8) * 8.0 / dosSend.BAUDS;
        dosSend.bits = dosSend.charToBits(dosSend.dataChar);
        dosSend.modulateData(dosSend.bits);
        dosSend.writeWavHeader();
        dosSend.writeNormalizeWavData();

        System.out.println("Message : " + String.valueOf(dosSend.dataChar));
        System.out.println("\tNombre de symboles : " + dosSend.dataChar.length);
        System.out.println("\tNombre d'échantillons : " + dosSend.dataMod.length);
        System.out.println("\tDurée : " + dosSend.duree + " s");
        System.out.println();
        //printIntArray(dosSend.dataMod);

        displaySig(dosSend.dataMod, 0, dosSend.dataMod.length, "line", "Signal modulé");

    }

}
