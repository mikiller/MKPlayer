package com.smg.mediaplayer.logic;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Mikiller on 2018/4/26.
 */

public class SafeResp implements Serializable {
    private int action;
    private String taskId;
    private List<Label> labels;

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    public static class Label{
        private int label;
        private int level;
        private Detail details;

        public int getLabel() {
            return label;
        }

        public void setLabel(int label) {
            this.label = label;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public Detail getDetails() {
            return details;
        }

        public void setDetails(Detail details) {
            this.details = details;
        }
    }

    public static class Detail{
        List<String> hint;

        public List<String> getHint() {
            return hint;
        }

        public void setHint(List<String> hint) {
            this.hint = hint;
        }
    }
}
