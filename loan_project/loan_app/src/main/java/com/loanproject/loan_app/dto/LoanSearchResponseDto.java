package com.loanproject.loan_app.dto;

import java.util.ArrayList;
import java.util.List;

public class LoanSearchResponseDto {

    private List<LoanSearchResultDto> content = new ArrayList<>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private String authorizedBranch;
    private String searchType;
    private String errorMessage;
    private String infoMessage;

    public LoanSearchResponseDto() {
    }

    public List<LoanSearchResultDto> getContent() {
        return content;
    }

    public void setContent(List<LoanSearchResultDto> content) {
        this.content = content != null ? content : new ArrayList<>();
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public String getAuthorizedBranch() {
        return authorizedBranch;
    }

    public void setAuthorizedBranch(String authorizedBranch) {
        this.authorizedBranch = authorizedBranch;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getInfoMessage() {
        return infoMessage;
    }

    public void setInfoMessage(String infoMessage) {
        this.infoMessage = infoMessage;
    }
}
