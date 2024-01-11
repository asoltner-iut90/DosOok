public class LPFilter2 {
    public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq){
        
        double[] result = new double[inputSignal.length];
        final double alpha = 0.035;
        final double beta = 1-alpha;

        double temp = inputSignal[0];
        double x;
        result[0] = inputSignal[0];
        
        for(int i=1; i<inputSignal.length; i++){
            x = inputSignal[i];
            result[i] = alpha * x + beta * temp;
            temp = x;
        }
        return result;
    }
}
