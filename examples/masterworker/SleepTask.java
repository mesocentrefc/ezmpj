package examples.masterworker;

import java.io.Serializable;

public class SleepTask implements Task<Integer>, Serializable{

    private static final long serialVersionUID = 10L;
    
    private int work;
    
    public SleepTask(int value)
    {
        this.work=value;
    }
    

    @Override
    public Integer execute() {
        
        try {
            Thread.sleep(work);
        } catch (InterruptedException e) {
           
            e.printStackTrace();
        }
        return new Integer(work);
    }

}
