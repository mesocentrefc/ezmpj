package mesofc;

import mpi.MPIException;


public interface EZMessageListener {
    
    public void onMessage(final EZStatus ezStatus) throws MPIException;

}
