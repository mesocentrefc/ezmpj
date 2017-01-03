package mesofc;

import java.io.Serializable;

public class EZStatus implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final int sender;
    private final int tag;
    private final Object dataObject;

    private final long nbMessages;

    private int busyWaitSleep = 10;

    public EZStatus(Object dataObject, int sender, int tag, long nbMessages) {
        this.dataObject = dataObject;
        this.sender = sender;
        this.tag = tag;
        this.nbMessages = nbMessages;
    }

    public int getSender() {
        return sender;
    }

    public int getTag() {
        return tag;
    }

    public Object getDataObject() {
        return dataObject;
    }

    public long getNbMessages() {
        return nbMessages;
    }

    public int getBusyWaitSleep() {
        return busyWaitSleep;
    }

    public void setBusyWaitSleep(int busyWaitSleep) {
        this.busyWaitSleep = busyWaitSleep;
    }

}
