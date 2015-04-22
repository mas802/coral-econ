/*
 *   Copyright 2009-2015 Markus Schaffner
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package coral.model;

public class ExpStage {

    private String template;

    private int loopback;

    public int getLoopback() {
        return loopback;
    }

    private int looprepeat;

    public int getLooprepeat() {
        return looprepeat;
    }

    private String[] condition;

    public String[] getCondition() {
        return condition;
    }

    private String[] validate;

    public String[] getValidate() {
        return validate;
    }

    private String waitFor = "";

    public String getWaitFor() {
        return waitFor;
    }

    private String[] simulated;

    public String[] getSimulated() {
        return simulated;
    }

    // private int thisrepeat = -1;

    public ExpStage(String template) {
        this(template, 0, 0);
    }

    public ExpStage(String template, int loopstage, int looprepeat) {
        this(template, null, loopstage, looprepeat);
    }

    public ExpStage(String template, String[] nullValidate) {
        this(template, nullValidate, 0, 0);
    }

    public ExpStage(String template, String[] nullValidate, int loopstage,
            int looprepeat) {
        this(template, loopstage, looprepeat, nullValidate, "");
    }

    public ExpStage(String template, int loopstage, int looprepeat,
            String[] valid, String waitFor) {
        this(template, loopstage, looprepeat, null, valid, waitFor);
    }

    public ExpStage(String template, int loopstage, int looprepeat,
            String[] condition, String[] valid, String waitFor) {
        this(template, loopstage, looprepeat, condition, valid, waitFor, null);
    }

    public ExpStage(String template, int loopstage, int looprepeat,
            String[] condition, String[] valid, String waitFor,
            String[] simulated) {
        this.template = template;
        this.looprepeat = looprepeat;
        this.loopback = loopstage;
        this.condition = condition;
        this.validate = valid;
        this.waitFor = waitFor;
        this.simulated = simulated;
    }

    public String getTemplate() {
        return template;
    }
}
