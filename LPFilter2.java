public class LPFilter2 {
    public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq){
        double[] result = new double[inputSignal.length];
        int n = 44;
        for(int i=0; i<inputSignal.length; i++){
            double s = 0;
        
            int c = 0;
            for(int j = i-n/2; j< i+n/2+1;j++){
                if(j>=0 && j<inputSignal.length){
                  c++;
                  s+=inputSignal[j];
                }
            }
            if(c==0){
              inputSignal[i] = 0;
            }else{
              inputSignal[i] = s/c;
            }
        }
        return inputSignal;
    }
    /*
    public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq){
        double[] result = new double[inputSignal.length];
        int n = 10;
        for(int i = 0; i < inputSignal.length; i++){
            double max = inputSignal[0];
            for(int j = i-n-1; j<=i+n;j++){
                if(j>=0 && j< inputSignal.length){
                    if(inputSignal[j]>max){
                        max = inputSignal[j];
                    }
                }
            }
            result[i] = max;
        }
        return result;
    }*/
}
