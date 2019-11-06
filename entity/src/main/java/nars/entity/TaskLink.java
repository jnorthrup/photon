package nars.entity;

/**
 * Created by jim on 1/2/2016.
 */
public interface TaskLink extends Item, TermLink {
    TaskHandle getTargetTask();
}
