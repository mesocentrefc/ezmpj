package examples.masterworker;

import mesofc.EZMPJ;
import mesofc.EZMessageListener;
import mesofc.EZStatus;
import mpi.MPIException;

public class Worker implements EZMessageListener {

    public void main(String args[]) {

        // add some work
    }

    @Override
    public void onMessage(EZStatus ezStatus) throws MPIException {

        if (ezStatus.getTag() == Main.COMPUTE_TAG) {
           
            Task<Integer> sleepTask = (Task<Integer>) ezStatus.getDataObject();

            Integer result = executeTask(sleepTask);

            EZMPJ.send(result, Main.MASTER, Main.RESULT_TAG);
        } else {

            EZMPJ.shutdownReceiverThread();
        }

    }

    public <T> T executeTask(Task<T> t) {
        return t.execute();
    }

}
