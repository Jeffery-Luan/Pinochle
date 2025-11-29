/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package protocolframework.decision;

/*
* This is an abstract base class used to uniformly represent all types of
* decision results made by AI. It is the return value standard of
*  the entire AI decision-making system.
*
* Provide a unified decision-making result interface
Ensure that all decisions have a basic verification mechanism
Support the recording of decision-making reasoning information
* */

public abstract class Decision {
    protected boolean isValid = true; // Whether the decision is effective

    public Decision() {}

    public Decision(boolean isValid, String reasoning) {
        this.isValid = isValid;
    }
    public boolean isValid() {
        return isValid;
    }
}