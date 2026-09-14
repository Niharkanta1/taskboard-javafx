package com.boardly.taskboard.model;

import java.util.ArrayList;
import java.util.List;

public class Checklist {
    private Long id;
    private String title;
    private List<ChecklistItem> items = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ChecklistItem> getItems() {
        return items;
    }

    public void setItems(List<ChecklistItem> items) {
        this.items = items;
    }
}
