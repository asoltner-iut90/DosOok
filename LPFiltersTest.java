import java.io.IOException;

public class LPFiltersTest {

    public static double average_time_filter1(double[] signal,int n){
        LPFilter1 filter1 = new LPFilter1();
        double average = 0;
        long start;
        long end;
        long executionTime;
        for(int i = 0; i < n; i++){
            start = System.nanoTime();
            filter1.lpFilter(signal,0,0);
            end = System.nanoTime();
            executionTime = end - start;
            average += executionTime;
        }
        return average/n/Math.pow(10, 6);
    }

    public static double average_time_filter2(double[] signal,int n){
        LPFilter2 filter2 = new LPFilter2();
        double average = 0;
        long start;
        long end;
        long executionTime;
        for(int i = 0; i < n; i++){
            start = System.nanoTime();
            filter2.lpFilter(signal,0,0);
            end = System.nanoTime();
            executionTime = end - start;
            average += executionTime;
        }
        return average/n/Math.pow(10, 9);
    }

    public static void main(String[] args){

        String wavFilePath = "5000.wav";

        DosRead dosRead = new DosRead();
        dosRead.readWavHeader(wavFilePath);
        dosRead.readAudioDouble();
        dosRead.audioRectifier();

        //Test

        long start;
        long end;
        long executionTime;

        
        double time1 =  average_time_filter1(dosRead.audio, 500);
        System.out.println("LPFilter1: " + time1);

        /*
        double time2 =  average_time_filter2(dosRead.audio, 500);
        System.out.println("LPFilter2: " + time2 );
        
        if(time1<time2){
            System.out.println("LPFilter1 est "+time2/time1+" plus rapide.");
        }else{
            System.out.println("LPFilter1 est "+time1/time1+" plus rapide.");
        }*/

        /*
        LPFilter1 filter = new LPFilter1(); 
        DosRead.displaySig(filter.lpFilter(dosRead.audio,0, 0), 0, 3000, "line", "Signal audio");
        */


        //Close File
        try {
            dosRead.fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
