package org.mtcg;

import java.util.List;

public class BattleResult {
    private boolean success;
    private String result;
    private List<String> log;

    public BattleResult(boolean success, String result) {
        this.success = success;
        this.result = result;
    }

    public BattleResult(boolean success, String result, List<String> log) {
        this.success = success;
        this.result = result;
        this.log = log;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getResult() {
        return result;
    }

    public List<String> getLog() {
        return log;
    }
}
