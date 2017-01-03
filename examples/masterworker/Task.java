package examples.masterworker;


public interface Task<T> {
 
    T execute();
}