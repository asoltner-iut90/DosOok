import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DosSend{
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
        // Entête RIFF
        outStream.write(new byte[]{'R', 'I', 'F', 'F'});

        // Taille totale du fichier (en octets) - 8, car RIFF et taille sont exclus
        writeLittleEndian((int) (nbBytes + 36), 4, outStream);

        // Format WAV
        outStream.write(new byte[]{'W', 'A', 'V', 'E'});

        // Sous-entête "fmt " pour le format des données
        outStream.write(new byte[]{'f', 'm', 't', ' '});

        // Taille du sous-entête "fmt " (16 pour PCM)
        writeLittleEndian(16, 4, outStream);

        // Code du format audio (1 pour PCM)
        writeLittleEndian(1, 2, outStream);

        // Nombre de canaux
        writeLittleEndian(CHANNELS, 2, outStream);

        // Fréquence d'échantillonnage
        writeLittleEndian(FECH, 4, outStream);

        // Débit binaire moyen (en octets par seconde)
        writeLittleEndian(FECH * CHANNELS * FMT / 8, 4, outStream);

        // Bloc d'alignement (nombre d'octets pour un échantillon, tous canaux confondus)
        writeLittleEndian(CHANNELS * FMT / 8, 2, outStream);

        // Bits par échantillon
        writeLittleEndian(FMT, 2, outStream);

        // Sous-entête "data" pour les données audio
        outStream.write(new byte[]{'d', 'a', 't', 'a'});

        // Taille des données audio (en octets)
        writeLittleEndian((int) nbBytes, 4, outStream);

        // Note : À ce stade, l'en-tête est complet, mais les données audio ne sont pas encore écrites
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
            // Taille des données audio (en octets)
            long nbBytes = taille * CHANNELS * FMT / 8;
    
            // Entête "data" pour les données audio
            outStream.write(new byte[]{'d', 'a', 't', 'a'});
    
            // Taille des données audio (en octets)
            writeLittleEndian((int) nbBytes, 4, outStream);
    
            // Normalisation et écriture des données audio dans le fichier WAV
            double maxAmplitude = getMaxAmplitude(dataMod);
            for (double sample : dataMod) {
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


        // Convertir le texte en tableau de caractères
        char[] textChars = inputText.toCharArray();
    
        // Stocker les caractères dans dataChar
        dataChar = textChars;
    
        // Afficher le texte à encoder
        System.out.println("Texte à encoder : " + inputText);

        // Retourner le nombre de caractères lus
        return dataChar.length;
    }
    


    /**
     * convert a char array to a bit array
     * @param chars
     * @return byte array containing only 0 & 1
     */

    public byte[] charToBits(char[] chars) {
        List<Byte> bitsList = new ArrayList<>();

        for (char c : chars) {
            // Convertir chaque caractère en une représentation binaire sur 8 bits
            String binaryString = String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0');

            // Ajouter chaque bit à la liste
            for (char bitChar : binaryString.toCharArray()) {
                bitsList.add((byte) (bitChar - '0'));
            }
        }

        // Convertir la liste de bits en un tableau de bytes
        byte[] bitsArray = new byte[bitsList.size()];
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
        // Le facteur de modulation (peut être ajusté en fonction de vos besoins)
        double modulationFactor = 1.0;

        // Fréquence de la porteuse
        double carrierFrequency = FP;

        // Durée d'un symbole en secondes
        double symbolDuration = 1.0 / BAUDS;

        // Nombre total d'échantillons
        int totalSamples = (int) (FECH * duree);

        // Tableau pour stocker le signal modulé
        dataMod = new double[totalSamples];

        // Indice pour suivre la position actuelle dans le signal modulé
        int currentIndex = 0;

        // Générer le signal modulé
        for (int bit : START_SEQ) {
            double amplitude = bit == 1 ? modulationFactor : 0.0; // Modulation OOK simple

            for (int i = 0; i < FECH * symbolDuration; i++) {
                dataMod[currentIndex++] = amplitude * Math.sin(2 * Math.PI * carrierFrequency * currentIndex / FECH);
            }
        }
        for (byte bit : bits) {
            double amplitude = bit == 1 ? modulationFactor : 0.0; // Modulation OOK simple

            for (int i = 0; i < FECH * symbolDuration; i++) {
                dataMod[currentIndex++] = amplitude * Math.sin(2 * Math.PI * carrierFrequency * currentIndex / FECH);
            }
        }
    }


    /**
     * Display a signal in a window
     * @param sig  the signal to display
     * @param start the first sample to display
     * @param stop the last sample to display
     * @param mode "line" or "point"
     * @param title the title of the window
     */

     public static void displaySig(double[] sig, int start, int stop, String mode, String title) {
        StdDraw.clear();
        StdDraw.setCanvasSize(800, 600);
        StdDraw.setXscale(0, sig.length);
        StdDraw.setYscale(-1, 1);

        if (title != null && !title.isEmpty()) {
            // Utiliser un texte pour le titre
            StdDraw.text(sig.length / 2.0, 1.1, title);
        }

        if (mode.equals("line")) {
            for (int i = start + 1; i <= stop; i++) {
                StdDraw.line(i - 1, sig[i - 1], i, sig[i]);
            }
        } else if (mode.equals("point")) {
            for (int i = start; i <= stop; i++) {
                StdDraw.point(i, sig[i]);
            }
        }

        StdDraw.show();
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
    
    public static void main(String[] args) {

        // créé un objet DosSend
        DosSend dosSend = new DosSend("DosOok_message.wav");
        // lit le texte à envoyer depuis l'entrée standard
        // et calcule la durée de l'audio correspondant
        dosSend.duree = (double)(dosSend.readTextData()+dosSend.START_SEQ.length/8)*8.0/dosSend.BAUDS;

        // génère le signal modulé après avoir converti les données en bits
        dosSend.modulateData(dosSend.charToBits(dosSend.dataChar));
        // écrit l'entête du fichier wav
        dosSend.writeWavHeader();
        // écrit les données audio dans le fichier wav
        dosSend.writeNormalizeWavData();


        // affiche les caractéristiques du signal dans la console
        System.out.println("Message : "+String.valueOf(dosSend.dataChar));
        System.out.println("\tNombre de symboles : "+dosSend.dataChar.length);
        System.out.println("\tNombre d'échantillons : "+dosSend.dataMod.length);
        System.out.println("\tDurée : "+dosSend.duree+" s");
        System.out.println();
        // exemple d'affichage du signal modulé dans une fenêtre graphique
        displaySig(dosSend.dataMod, 1000, 3000, "line", "Signal modulé");

    }

}
