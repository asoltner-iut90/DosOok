public class LPFilter1 {
    public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq){
        //double[] result = new double[inputSignal.length];
        int n = 22;
        int d_n = 2*n;
        double s = 0;
        for(int i=0; i<n; i++){
            s += inputSignal[i];
        }
        for(int i=0; i<inputSignal.length; i++){
            if(i+n < inputSignal.length){
                s += inputSignal[i+n];
            }
            if(i-n >= 0){
                s -= inputSignal[i-n];
            }
            inputSignal[i] = s/d_n;
        }
        double[] result = inputSignal;
        return result;
    }
}
